package org.wolfiee.idrecon.identity_reconciliation.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wolfiee.idrecon.identity_reconciliation.model.Contact;

import java.util.List;

@Repository
public interface contactrepo extends JpaRepository<Contact,Long> {
    List<Contact> findByEmail(String email);
    List<Contact> findByPhonenum(String phonenum);
    List<Contact> findByEmailOrPhonenum(String email, String phonenum);
    List<Contact> findByLinkedid(Long linkedid);
}
