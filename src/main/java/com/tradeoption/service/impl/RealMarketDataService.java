package com.tradeoption.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeoption.domain.OptionLeg;
import com.tradeoption.service.MarketDataService;
import com.tradeoption.service.McxApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("!test")
public class RealMarketDataService implements MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(RealMarketDataService.class);
    private final McxApiService mcxApiService;
    private final ObjectMapper objectMapper;
    private final com.tradeoption.service.SystemConfigService systemConfigService;

    // Cache for Stale Data: Key -> Price
    private final java.util.Map<String, Double> priceCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Request Deduplication Cache: Key -> (Price, Timestamp)
    private final java.util.Map<String, CachedRequest> requestCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long REQUEST_CACHE_TTL_MS = 2000; // 2 seconds

    private static class CachedRequest {
        final Double price;
        final long timestamp;

        CachedRequest(Double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    // Option Chain Response Cache: Key (Symbol:Expiry) -> (JsonResponse, Timestamp)
    private final java.util.Map<String, CachedOptionChain> optionChainCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long OPTION_CHAIN_CACHE_TTL_MS = 5000; // 5 seconds

    private static class CachedOptionChain {
        final String jsonResponse;
        final long timestamp;

        CachedOptionChain(String jsonResponse, long timestamp) {
            this.jsonResponse = jsonResponse;
            this.timestamp = timestamp;
        }
    }

    public RealMarketDataService(McxApiService mcxApiService, ObjectMapper objectMapper,
            com.tradeoption.service.SystemConfigService systemConfigService) {
        this.mcxApiService = mcxApiService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    private final MockMarketDataService fallbackService = new MockMarketDataService();

    @Override
    public java.util.Optional<Double> getLtp(String symbol) {
        // MCX Integration Removed

        // Fallback to Mock
        if (systemConfigService.getConfig().isEnableMockFallback()) {
            return fallbackService.getLtp(symbol);
        }
        return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Double> getLtp(OptionLeg leg) {
        return getPrice(leg, "LTP");
    }

    @Override
    public java.util.Optional<Double> getBid(OptionLeg leg) {
        return getPrice(leg, "BID");
    }

    @Override
    public java.util.Optional<Double> getAsk(OptionLeg leg) {
        return getPrice(leg, "ASK");
    }

    private java.util.Optional<Double> getPrice(OptionLeg leg, String fieldType) {
        // MCX Integration Removed

        // Fallback to Mock
        if (systemConfigService.getConfig().isEnableMockFallback()) {
            if ("BID".equals(fieldType))
                return fallbackService.getBid(leg);
            if ("ASK".equals(fieldType))
                return fallbackService.getAsk(leg);
            return fallbackService.getLtp(leg);
        }
        return java.util.Optional.empty();
    }

    private String resolveField(String legType, String fieldType) {
        boolean isCall = legType.equalsIgnoreCase("CE") || legType.equalsIgnoreCase("CALL");

        switch (fieldType) {
            case "BID":
                return isCall ? "CallBidPrice" : "PutBidPrice"; // MCX typically uses these or similar
            case "ASK":
                return isCall ? "CallAskPrice" : "PutAskPrice";
            case "LTP":
            default:
                return isCall ? "CallLTP" : "PutLTP";
        }
    }
}
