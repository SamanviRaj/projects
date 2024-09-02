package com.eqh.application.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "party", url = "http://party-number-service") // Replace with actual service name and URL
public interface PartyClient {

    @GetMapping("/api/party/number")
    String getPartyNumber(@RequestParam("id") String id);
}


