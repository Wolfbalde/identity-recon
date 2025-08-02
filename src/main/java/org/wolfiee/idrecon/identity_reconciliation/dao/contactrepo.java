package org.wolfiee.idrecon.identity_reconciliation.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wolfiee.idrecon.identity_reconciliation.model.Contact;

@Repository
public interface contactrepo extends JpaRepository<Contact,Long> {
}
