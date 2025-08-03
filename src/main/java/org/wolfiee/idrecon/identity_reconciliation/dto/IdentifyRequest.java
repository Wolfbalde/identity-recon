package org.wolfiee.idrecon.identity_reconciliation.dto;

import lombok.Data;

@Data
public class IdentifyRequest {
    private String email;
    private String phonenum;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenum() {
        return phonenum;
    }

    public void setPhonenum(String phonenum) {
        this.phonenum = phonenum;
    }
}
