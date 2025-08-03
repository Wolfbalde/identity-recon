package org.wolfiee.idrecon.identity_reconciliation.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.wolfiee.idrecon.identity_reconciliation.dto.IdentifyRequest;
import org.wolfiee.idrecon.identity_reconciliation.dto.IdentifyResponse;
import org.wolfiee.idrecon.identity_reconciliation.service.contactService;

@RestController
public class contactcontroller {


    @Autowired
    private contactService contact;


    @PostMapping("/identify")
    public ResponseEntity<IdentifyResponse> identify(@RequestBody IdentifyRequest request) {
        return ResponseEntity.ok(contact.identifyContact(request.getEmail(), request.getPhonenum()));
    }

}
