package com.eqh.party.controller;

import com.eqh.party.entity.Address;
import com.eqh.party.entity.EmailAddress;
import com.eqh.party.entity.Party;
import com.eqh.party.entity.Person;
import com.eqh.party.service.PartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/party")
public class partyController {

    private final PartyService partyService;

    @Autowired
    public partyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping("/{partyNumber}")
    public Party getPartyDetails(@PathVariable("partyNumber") String partyNumber) {
        return partyService.getPartyDetailsByPartyNumber(partyNumber);
    }

    @GetMapping("/{partyNumber}/emails")
    public List<EmailAddress> getEmailAddresses(@PathVariable("partyNumber") String partyNumber) {
        return partyService.getEmailAddressesByPartyNumber(partyNumber);
    }

    @GetMapping("/{partyNumber}/addresses")
    public List<Address> getAddresses(@PathVariable("partyNumber") String partyNumber) {
        return partyService.getAddressesByPartyNumber(partyNumber);
    }

    @GetMapping("/{partyNumber}/person")
    public Person getPersonDetails(@PathVariable("partyNumber") String partyNumber) {
        return partyService.getPersonDetailsByPartyNumber(partyNumber);
    }



}



