package com.eqh.application.feignClient;

import com.eqh.application.dto.Address;
import com.eqh.application.dto.Party;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "partyClient", url = "http://localhost:8083/api/party")
public interface PartyClient {

    @GetMapping("/{partyNumber}/addresses")
    List<Address> getAddresses(@RequestParam("partyNumber") String partyNumber);

    @GetMapping("/{partyNumber}")
    public Party getPartyDetails(@PathVariable("partyNumber") String partyNumber);
}



