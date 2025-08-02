package org.wolfiee.idrecon.identity_reconciliation.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long linkedid;

    @Enumerated(EnumType.STRING)
    private LinkPrecedence linkPrecedence;
    private String phonenum;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLinkedid() {
        return linkedid;
    }

    public void setLinkedid(long linkedid) {
        this.linkedid = linkedid;
    }

    public LinkPrecedence getLinkPrecedence() {
        return linkPrecedence;
    }

    public void setLinkPrecedence(LinkPrecedence linkPrecedence) {
        this.linkPrecedence = linkPrecedence;
    }

    public String getPhonenum() {
        return phonenum;
    }

    public void setPhonenum(String phonenum) {
        this.phonenum = phonenum;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

