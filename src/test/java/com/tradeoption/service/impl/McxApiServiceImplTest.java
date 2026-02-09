package com.tradeoption.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class McxApiServiceImplTest {

        @MockBean
        private org.springframework.web.client.RestTemplate restTemplate; // Keep purely to satisfy context if needed,
                                                                          // though likely unused now

        @MockBean
        private com.tradeoption.repository.RocksDBRepository rocksDBRepository;

        @Autowired
        private McxApiServiceImpl mcxApiService;

        @Value("${mcx.api.base-url:https://www.mcxindia.com}")
        private String baseUrl;

        @org.mockito.Mock
        private org.apache.hc.client5.http.impl.classic.CloseableHttpClient mockHttpClient;

        @org.mockito.Mock
        private org.apache.hc.client5.http.impl.classic.CloseableHttpResponse mockResponse;

        @org.mockito.Mock
        private org.apache.hc.core5.http.HttpEntity mockEntity;

        @BeforeEach
        public void init() throws Exception {
                org.mockito.MockitoAnnotations.openMocks(this);

                // Inject mock factory
                mcxApiService.setClientFactory(() -> mockHttpClient);

                // Re-initialize to pick up the mock client
                mcxApiService.init();
        }

        @Test
        public void testGetOptionChain() throws Exception {
                String commodity = "GOLD";
                String expiry = "2023-12-05";
                String expectedResponse = "{\"data\": \"mock data\"}";

                // Mock Entity
                // Mock Entity: Need stream for each call (403 body, then 200 body)
                org.mockito.Mockito.when(mockEntity.getContent())
                                .thenReturn(new java.io.ByteArrayInputStream("Access Denied".getBytes())) // 1st call
                                                                                                          // (403)
                                .thenReturn(new java.io.ByteArrayInputStream(expectedResponse.getBytes())); // 2nd call
                                                                                                            // (200)

                org.mockito.Mockito.when(mockResponse.getEntity()).thenReturn(mockEntity);

                // Mock Status: 403 then 200
                org.mockito.Mockito.when(mockResponse.getCode())
                                .thenReturn(403)
                                .thenReturn(200);

                // Mock Execute for Warmup (GET) and Data (POST)
                // Since we are using a callback, we need to invoke it.

                // Match GET requests (Warmup)
                // Use raw class matching to avoid ambiguity with overloaded methods
                org.mockito.Mockito.when(mockHttpClient.execute(
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.client5.http.classic.methods.HttpGet.class),
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.core5.http.io.HttpClientResponseHandler.class)))
                                .thenReturn(null); // Warmup discards response

                // Match POST requests (Data)
                org.mockito.Mockito.when(mockHttpClient.execute(
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.client5.http.classic.methods.HttpPost.class),
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.core5.http.io.HttpClientResponseHandler.class)))
                                .thenAnswer(invocation -> {
                                        org.apache.hc.core5.http.io.HttpClientResponseHandler<String> handler = invocation
                                                        .getArgument(1);
                                        return handler.handleResponse(mockResponse);
                                });

                String result = mcxApiService.getOptionChain(commodity, expiry);
                assertEquals(expectedResponse, result);

                // Verify flow
                // Should have called GET twice (warmup) and POST twice (1 fail, 1 success)
                org.mockito.Mockito.verify(mockHttpClient, org.mockito.Mockito.atLeast(2)).execute(
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.client5.http.classic.methods.HttpGet.class),
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.core5.http.io.HttpClientResponseHandler.class));
                org.mockito.Mockito.verify(mockHttpClient, org.mockito.Mockito.times(2)).execute(
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.client5.http.classic.methods.HttpPost.class),
                                org.mockito.ArgumentMatchers
                                                .any(org.apache.hc.core5.http.io.HttpClientResponseHandler.class));
        }
}
