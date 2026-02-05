package com.tradeoption.controller;

import com.tradeoption.service.McxApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcx")
public class McxProxyController {

    private final McxApiService mcxApiService;

    public McxProxyController(McxApiService mcxApiService) {
        this.mcxApiService = mcxApiService;
    }

    @GetMapping("/option-chain")
    public String getOptionChain(@RequestParam String commodity, @RequestParam String expiry) {
        return mcxApiService.getOptionChain(commodity, expiry);
    }

    @GetMapping("/option-chain-strike")
    public String getOptionChainStrike(@RequestParam String commodity, @RequestParam String expiry,
            @RequestParam String strike) {
        return mcxApiService.getOptionChainStrikePrice(commodity, expiry, strike);
    }
}
