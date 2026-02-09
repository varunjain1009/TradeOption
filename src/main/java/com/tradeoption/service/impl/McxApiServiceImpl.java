package com.tradeoption.service.impl;

import com.tradeoption.service.McxApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Service
public class McxApiServiceImpl implements McxApiService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(McxApiServiceImpl.class);

    @NonNull
    private final String baseUrl;

    // Stateful Client
    private org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient;
    private org.apache.hc.client5.http.cookie.BasicCookieStore cookieStore;
    private final Object clientLock = new Object();

    // Rate Limiting
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 1000; // 1 second between requests

    public McxApiServiceImpl(RestTemplate mcxRestTemplate, // Kept for constructor signature compat only
            @Value("${mcx.api.base-url:https://www.mcxindia.com}") @NonNull String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private java.util.function.Supplier<org.apache.hc.client5.http.impl.classic.CloseableHttpClient> clientFactory;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Default factory creates the real client
        if (this.clientFactory == null) {
            this.clientFactory = () -> {
                org.apache.hc.client5.http.config.RequestConfig requestConfig = org.apache.hc.client5.http.config.RequestConfig
                        .custom()
                        .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(5))
                        .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(10))
                        .setConnectionRequestTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(5))
                        .build();

                return org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                        .setDefaultCookieStore(cookieStore)
                        .setDefaultRequestConfig(requestConfig)
                        .setUserAgent(
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build();
            };
        }
        resetSession();
    }

    // For testing purposes
    public void setClientFactory(
            java.util.function.Supplier<org.apache.hc.client5.http.impl.classic.CloseableHttpClient> clientFactory) {
        this.clientFactory = clientFactory;
    }

    private void resetSession() {
        synchronized (clientLock) {
            log.info("Resetting HTTP Client and Cookie Store...");
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e) {
                log.warn("Error closing old client: {}", e.getMessage());
            }

            this.cookieStore = new org.apache.hc.client5.http.cookie.BasicCookieStore();
            this.cookieStore = new org.apache.hc.client5.http.cookie.BasicCookieStore();
            // Use the factory to create the client.
            // Ensure default factory is set if not present (though init should handle it)
            if (this.clientFactory == null) {
                this.clientFactory = () -> org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                        .setDefaultCookieStore(cookieStore)
                        .setUserAgent(
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build();
            }
            this.httpClient = clientFactory.get();
        }
    }

    @Override
    public String getOptionChain(String commodity, String expiry) {
        return executeWithRetry(() -> fetchOptionChainInternal(commodity, expiry));
    }

    @Override
    public String getOptionChainStrikePrice(String commodity, String expiry, String strike) {
        return getOptionChain(commodity, expiry);
    }

    // --- Core Logic ---

    private String executeWithRetry(java.util.function.Supplier<String> action) {
        // Attempt 1
        String response = action.get();
        if (!isBlocked(response)) {
            return response;
        }

        log.warn("MCX API Blocked (403/HTML). Initiating Session Reset & Warmup...");

        // Reset & Warmup
        resetSession();
        warmUpSession();

        // Attempt 2 (Retry)
        response = action.get();
        if (!isBlocked(response)) {
            log.info("Retry Successful!");
            return response;
        }

        log.error("MCX API Blocked after retry. Returning empty response.");
        return "{}"; // Fail gracefully to trigger fallback in Service
    }

    private String fetchOptionChainInternal(String commodity, String expiry) {
        // Rate Limiting
        synchronized (clientLock) {
            long now = System.currentTimeMillis();
            long timeSinceLastRequest = now - lastRequestTime;
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                long sleepTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                log.debug("Rate limiting: sleeping for {}ms", sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestTime = System.currentTimeMillis();
        }

        String url = baseUrl + "/backpage.aspx/GetOptionChain";
        String jsonBody = String.format("{\"Commodity\":\"%s\",\"Expiry\":\"%s\"}", commodity, expiry);

        try {
            org.apache.hc.client5.http.classic.methods.HttpPost request = new org.apache.hc.client5.http.classic.methods.HttpPost(
                    url);
            request.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(jsonBody,
                    org.apache.hc.core5.http.ContentType.APPLICATION_JSON));

            // Simulation Headers
            request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            request.setHeader("Accept-Language", "en-GB,en;q=0.9");
            request.setHeader("Connection", "keep-alive");
            request.setHeader("X-Requested-With", "XMLHttpRequest");
            request.setHeader("Origin", "https://www.mcxindia.com");
            request.setHeader("Referer", "https://www.mcxindia.com/market-data/option-chain");
            request.setHeader("Sec-Fetch-Dest", "empty");
            request.setHeader("Sec-Fetch-Mode", "cors");
            request.setHeader("Sec-Fetch-Site", "same-origin");

            return httpClient.execute(request, response -> {
                int status = response.getCode();
                String body = org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity());
                if (status == 403) {
                    return "BLOCKED: 403";
                }
                return body;
            });

        } catch (Exception e) {
            log.error("Execution Request Failed: {}", e.getMessage());
            return "{}";
        }
    }

    private void warmUpSession() {
        log.info("Warming up MCX Session...");
        try {
            // 1. Home Page
            executeGet(baseUrl + "/market-data");
            Thread.sleep(100 + (long) (Math.random() * 200));

            // 2. Option Chain Page (Crucial for ASP.NET_SessionId)
            executeGet(baseUrl + "/market-data/option-chain");

            log.info("Warmup Complete. Cookies: {}", cookieStore.getCookies());
        } catch (Exception e) {
            log.warn("Warmup incomplete: {}", e.getMessage());
        }
    }

    private void executeGet(String url) {
        try {
            org.apache.hc.client5.http.classic.methods.HttpGet request = new org.apache.hc.client5.http.classic.methods.HttpGet(
                    url);
            request.setHeader("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            httpClient.execute(request, response -> {
                org.apache.hc.core5.http.io.entity.EntityUtils.consume(response.getEntity()); // Ensure stream
                                                                                              // closed/cookies
                                                                                              // processed
                return null;
            });
        } catch (Exception e) {
            log.debug("GET {} failed: {}", url, e.getMessage());
        }
    }

    private boolean isBlocked(String response) {
        if (response == null || response.trim().isEmpty())
            return true;
        if (response.startsWith("BLOCKED"))
            return true;

        // Soft Block Detection (HTML content instead of JSON)
        String lower = response.toLowerCase();
        return lower.contains("<html") || lower.contains("access denied") || lower.contains("technical difficulty");
    }
}
