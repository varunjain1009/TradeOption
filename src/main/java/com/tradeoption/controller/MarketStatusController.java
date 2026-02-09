package com.tradeoption.controller;

import com.tradeoption.service.MarketStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MarketStatusController {

    private final MarketStatusService marketStatusService;

    public MarketStatusController(MarketStatusService marketStatusService) {
        this.marketStatusService = marketStatusService;
    }

    @GetMapping("/api/market-status")
    public Map<String, Object> getMarketStatus() {
        boolean isOpen = marketStatusService.isMarketOpen();
        String reason = marketStatusService.getMarketStatusReason();
        return Map.of("isOpen", isOpen, "reason", reason);
    }
}
