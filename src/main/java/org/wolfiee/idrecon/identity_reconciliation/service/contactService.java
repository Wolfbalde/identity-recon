package org.wolfiee.idrecon.identity_reconciliation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wolfiee.idrecon.identity_reconciliation.dao.contactrepo;
import org.wolfiee.idrecon.identity_reconciliation.dto.ContactGroup;
import org.wolfiee.idrecon.identity_reconciliation.dto.IdentifyResponse;
import org.wolfiee.idrecon.identity_reconciliation.model.Contact;
import org.wolfiee.idrecon.identity_reconciliation.model.LinkPrecedence;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class contactService {

    @Autowired
    private contactrepo repo;
    /**
     * Main method to identify and reconcile contacts given email and/or phone number.
     * This method:
     *  - Finds all contacts matching either email or phone number.
     *  - Collects the full related group via primary/secondary links.
     *  - Merges multiple primaries if needed.
     *  - Creates new contact entries if no match or new info.
     *  - Returns a consolidated IdentifyResponse with grouped contact info.
     *
     * @param *email* Email provided in request (nullable)
     * @param *phonenum* Phone number provided in request (nullable)
     * @return IdentifyResponse containing consolidated contact group information
     */
    @Transactional
    public IdentifyResponse identifyContact(String email, String phonenum) {
        System.out.println("Identify called with email=" + email + ", phonenum=" + phonenum);

        // Find initial contacts that match either email OR phone number
        Set<Contact> initialContacts = new HashSet<>(repo.findByEmailOrPhonenum(email, phonenum));

        System.out.println("[identifyContact] Initial matches count: " + initialContacts.size());
        for (Contact c : initialContacts) {
            System.out.printf("Contact id=%d, email=%s, phonenum=%s, linkPrecedence=%s, linkedid=%s%n",
                    c.getId(), c.getEmail(), c.getPhonenum(), c.getLinkPrecedence(), c.getLinkedid());
        }

        // Build full group of contacts linked via linkedid and linkPrecedence
        Set<Contact> fullGroup = new HashSet<>(initialContacts);
        Queue<Contact> queue = new LinkedList<>(initialContacts);

        // collect all linked contacts: from secondaries to primaries and vice versa
        while (!queue.isEmpty()) {
            Contact current = queue.poll();

            // If current is secondary, add its primary contact to group and queue
            if (current.getLinkPrecedence() == LinkPrecedence.SECONDARY && current.getLinkedid() != null) {
                Contact primary = repo.findById(current.getLinkedid()).orElse(null);
                if (primary != null && fullGroup.add(primary)) {
                    queue.add(primary);
                }
            }

            // If current is primary, add all its secondary contacts to group and queue
            if (current.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
                List<Contact> secondaries = repo.findByLinkedid(current.getId());
                for (Contact sec : secondaries) {
                    if (fullGroup.add(sec)) {
                        queue.add(sec);
                    }
                }
            }
        }

        System.out.println("[identifyContact] Full group size: " + fullGroup.size());
        for (Contact c : fullGroup) {
            System.out.printf("Full group contact id=%d, email=%s, phonenum=%s, linkPrecedence=%s, linkedid=%s%n",
                    c.getId(), c.getEmail(), c.getPhonenum(), c.getLinkPrecedence(), c.getLinkedid());
        }


        // Find all primary contacts in the full group, sorted oldest first
        List<Contact> primaries = fullGroup.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .sorted(Comparator.comparing(Contact::getCreatedAt))
                .collect(Collectors.toList());

        Contact primaryContact;

        // If no contacts found at all, create a new primary contact
        if (fullGroup.isEmpty()) {
            primaryContact = createNewPrimary(email, phonenum);
            return buildResponse(primaryContact, Collections.emptySet());
        }


        // If contacts exist but no primary (all are secondaries), promote oldest to primary
        if (primaries.isEmpty()) {
            Contact oldest = fullGroup.stream()
                    .sorted(Comparator.comparing(Contact::getCreatedAt))
                    .findFirst().orElse(null);

            if (oldest != null) {
                oldest.setLinkPrecedence(LinkPrecedence.PRIMARY);
                oldest.setLinkedid(null);
                repo.save(oldest);
                primaryContact = oldest;

                for (Contact c : fullGroup) {
                    if (!c.getId().equals(primaryContact.getId())
                            && c.getLinkedid() != null && !c.getLinkedid().equals(primaryContact.getId())) {
                        c.setLinkedid(primaryContact.getId());
                        repo.save(c);
                    }
                }
            } else {
                primaryContact = createNewPrimary(email, phonenum);
            }
        } else {
            primaryContact = primaries.get(0);

            for (int i = 1; i < primaries.size(); i++) {
                Contact toDemote = primaries.get(i);

                boolean needsUpdate = toDemote.getLinkPrecedence() != LinkPrecedence.SECONDARY
                        || !Objects.equals(toDemote.getLinkedid(), primaryContact.getId());

                if (needsUpdate) {
                    toDemote.setLinkPrecedence(LinkPrecedence.SECONDARY);
                    toDemote.setLinkedid(primaryContact.getId());
                    repo.save(toDemote);

                    List<Contact> theirSecondaries = repo.findByLinkedid(toDemote.getId());
                    for (Contact sec : theirSecondaries) {
                        sec.setLinkedid(primaryContact.getId());
                        repo.save(sec);
                    }
                    System.out.printf("[identifyContact] Demoted primary %d to secondary linked to %d%n",
                            toDemote.getId(), primaryContact.getId());
                }
            }
        }

        // Check if incoming email or phone number are new in this group
        // If new, create new secondary contact linked to primary
        boolean emailExists = fullGroup.stream().anyMatch(c -> email != null && email.equals(c.getEmail()));
        boolean phoneExists = fullGroup.stream().anyMatch(c -> phonenum != null && phonenum.equals(c.getPhonenum()));
        boolean exactMatch = fullGroup.stream().anyMatch(c ->
                Objects.equals(email, c.getEmail()) && Objects.equals(phonenum, c.getPhonenum()));

        if (((email != null && !emailExists) || (phonenum != null && !phoneExists)) && !exactMatch) {
            Contact secondary = new Contact();
            secondary.setEmail(email);
            secondary.setPhonenum(phonenum);
            secondary.setLinkPrecedence(LinkPrecedence.SECONDARY);
            secondary.setLinkedid(primaryContact.getId());
            LocalDateTime now = LocalDateTime.now();
            secondary.setCreatedAt(now);
            secondary.setUpdatedAt(now);
            repo.save(secondary);
            fullGroup.add(secondary);

            System.out.printf("[identifyContact] Created new secondary contact id=%d linked to primary id=%d%n",
                    secondary.getId(), primaryContact.getId());
        }

        // Build and return the consolidated response payload
        return buildResponse(primaryContact, fullGroup);
    }

    /**
     * Build the API response DTO with primary contact info,
     * lists of unique emails, phone numbers (primary first),
     * and IDs of secondary contacts.
     */
    private IdentifyResponse buildResponse(Contact primary, Set<Contact> fullGroup) {
        List<Contact> contacts = new ArrayList<>(fullGroup);

        // Gather unique emails, place primary email first
        List<String> emails = contacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        emails.remove(primary.getEmail());
        emails.add(0, primary.getEmail());

        // Gather unique phone numbers, place primary phone first
        List<String> phones = contacts.stream()
                .map(Contact::getPhonenum)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        phones.remove(primary.getPhonenum());
        phones.add(0, primary.getPhonenum());

        // Collect IDs of secondary contacts
        List<Long> secondaryIds = contacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .collect(Collectors.toList());

        // Populate ContactGroup DTO
        ContactGroup group = new ContactGroup();
        group.setPrimaryContactId(primary.getId());
        group.setEmails(emails);
        group.setPhoneNumbers(phones);
        group.setSecondaryContactIds(secondaryIds);

        // Wrap in IdentifyResponse DTO
        IdentifyResponse response = new IdentifyResponse();
        response.setContacts(group);
        return response;
    }

    /**
     * Helper method to create and save a new primary contact
     */
    private Contact createNewPrimary(String email, String phonenum) {
        Contact primary = new Contact();
        primary.setEmail(email);
        primary.setPhonenum(phonenum);
        primary.setLinkPrecedence(LinkPrecedence.PRIMARY);
        LocalDateTime now = LocalDateTime.now();
        primary.setCreatedAt(now);
        primary.setUpdatedAt(now);
        repo.save(primary);
        System.out.printf("[identifyContact] Created new primary contact id=%d%n", primary.getId());
        return primary;
    }
}
