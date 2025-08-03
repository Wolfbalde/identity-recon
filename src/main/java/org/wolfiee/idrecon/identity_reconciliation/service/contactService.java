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


    @Transactional
    public IdentifyResponse identifyContact(String email, String phonenum) {
        Set<Contact> contacts = new HashSet<>(repo.findByEmailOrPhoneNumber(email, phonenum));

        Set<Contact> fullGroup = new HashSet<>(contacts);
        Queue<Contact> queue = new LinkedList<>(contacts);
        while (!queue.isEmpty()) {
            Contact c = queue.poll();
            if (c.getLinkPrecedence() == LinkPrecedence.SECONDARY && c.getLinkedid() != null) {
                Contact primary = repo.findById(c.getLinkedid()).orElse(null);
                if (primary != null && fullGroup.add(primary)) queue.add(primary);
            }
            if (c.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
                List<Contact> secondaries = repo.findByLinkedId(c.getId());
                for (Contact sec : secondaries) {
                    if (fullGroup.add(sec)) queue.add(sec);
                }
            }
        }

        Contact primary;
        List<Contact> primaries = fullGroup.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .sorted(Comparator.comparing(Contact::getCreatedAt))
                .collect(Collectors.toList());

        if (fullGroup.isEmpty()) {
            primary = new Contact();
            primary.setEmail(email);
            primary.setPhonenum(phonenum);
            primary.setLinkPrecedence(LinkPrecedence.PRIMARY);
            primary.setCreatedAt(LocalDateTime.now());
            primary.setUpdatedAt(LocalDateTime.now());
            repo.save(primary);

            return buildResponse(primary, Collections.emptySet());
        } else {
            primary = primaries.get(0);
            // Merge logic for other primaries
            for (int i = 1; i < primaries.size(); i++) {
                Contact merged = primaries.get(i);
                merged.setLinkPrecedence(LinkPrecedence.SECONDARY);
                merged.setLinkedid(primary.getId());
                repo.save(merged);
                for (Contact sec : repo.findByLinkedId(merged.getId())) {
                    sec.setLinkedid(primary.getId());
                    repo.save(sec);
                }
            }
        }

        // Check if a new secondary should be created
        boolean emailExists = fullGroup.stream().anyMatch(c -> email != null && email.equals(c.getEmail()));
        boolean phoneExists = fullGroup.stream().anyMatch(c -> phonenum != null && phonenum.equals(c.getPhonenum()));
        boolean exactMatch = fullGroup.stream().anyMatch(c ->
                Objects.equals(email, c.getEmail()) && Objects.equals(phonenum, c.getPhonenum()));

        if ((!emailExists && email != null) || (!phoneExists && phonenum != null)) {
            if (!exactMatch) {
                Contact secondary = new Contact();
                secondary.setEmail(email);
                secondary.setPhonenum(phonenum);
                secondary.setLinkPrecedence(LinkPrecedence.SECONDARY);
                secondary.setLinkedid(primary.getId());
                secondary.setCreatedAt(LocalDateTime.now());
                secondary.setUpdatedAt(LocalDateTime.now());
                repo.save(secondary);
                fullGroup.add(secondary);
            }
        }

        return buildResponse(primary, fullGroup);
    }

    private IdentifyResponse buildResponse(Contact primary, Set<Contact> fullGroup) {
        List<Contact> contacts = new ArrayList<>(fullGroup);

        List<String> emails = contacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        emails.remove(primary.getEmail());
        emails.add(0, primary.getEmail());

        List<String> phones = contacts.stream()
                .map(Contact::getPhonenum)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        phones.remove(primary.getPhonenum());
        phones.add(0, primary.getPhonenum());

        List<Long> secondaryIds = contacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .collect(Collectors.toList());

        ContactGroup group = new ContactGroup();
        group.setPrimaryContactId(primary.getId());
        group.setEmails(emails);
        group.setPhoneNumbers(phones);
        group.setSecondaryContactIds(secondaryIds);

        IdentifyResponse response = new IdentifyResponse();
        response.setContacts(group);
        return response;


    }
}
