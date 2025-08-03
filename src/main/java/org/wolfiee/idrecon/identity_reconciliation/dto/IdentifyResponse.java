package org.wolfiee.idrecon.identity_reconciliation.dto;


import lombok.Data;

@Data
public class IdentifyResponse {

    private ContactGroup contacts;

    public ContactGroup getContacts() {
        return contacts;
    }

    public void setContacts(ContactGroup contacts) {
        this.contacts = contacts;
    }
}
