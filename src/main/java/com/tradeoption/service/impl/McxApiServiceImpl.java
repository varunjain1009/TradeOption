package com.tradeoption.service.impl;

import com.tradeoption.service.McxApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class McxApiServiceImpl implements McxApiService {

    private final RestTemplate mcxRestTemplate;

    @Value("${mcx.api.base-url}")
    private String baseUrl;

    public McxApiServiceImpl(RestTemplate mcxRestTemplate) {
        this.mcxRestTemplate = mcxRestTemplate;
    }

    @Override
    public String getOptionChain(String commodity, String expiry) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/market-data/option-chain") // Adjust path as per actual MCX API endpoints
                .queryParam("Commodity", commodity)
                .queryParam("Expiry", expiry)
                .toUriString();
        // Returning raw JSON for now
        return mcxRestTemplate.getForObject(url, String.class);
    }

    @Override
    public String getOptionChainStrikePrice(String commodity, String expiry, String strike) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/market-data/option-chain") // Adjust path/params
                .queryParam("Commodity", commodity)
                .queryParam("Expiry", expiry)
                .queryParam("StrikePrice", strike)
                .toUriString();
        return mcxRestTemplate.getForObject(url, String.class);
    }
}
