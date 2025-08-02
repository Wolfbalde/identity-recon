package org.wolfiee.idrecon.identity_reconciliation.dto;


import lombok.Data;

import java.util.List;

@Data
public class ContactGroup {

    private long primaryContactId;
    private List<String> emails;
    private List<String> phoneNumbers;
    private List<Long> secondaryContactIds;
}
