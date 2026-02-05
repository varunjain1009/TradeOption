package com.tradeoption.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
public class McxApiServiceImplTest {

    @Autowired
    private RestTemplate mcxRestTemplate;

    @Autowired
    private McxApiServiceImpl mcxApiService;

    @Value("${mcx.api.base-url}")
    private String baseUrl;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(mcxRestTemplate);
    }

    @Test
    public void testGetOptionChain() {
        String commodity = "GOLD";
        String expiry = "2023-12-05";
        String expectedResponse = "{\"data\": \"mock data\"}";

        mockServer.expect(requestTo(containsString("/market-data/option-chain")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        String result = mcxApiService.getOptionChain(commodity, expiry);
        assertEquals(expectedResponse, result);
        mockServer.verify();
    }
}
