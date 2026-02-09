package com.tradeoption.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class McxApiConfig {

    @Value("${mcx.api.headers.user-agent:Mozilla/5.0}")
    private String userAgent;

    @Value("${mcx.api.headers.accept:application/json}")
    private String accept;

    @Value("${mcx.api.headers.accept-language:en-US}")
    private String acceptLanguage;

    @Value("${mcx.api.headers.origin:https://www.mcxindia.com}")
    private String origin;

    @Value("${mcx.api.headers.referer:https://www.mcxindia.com/}")
    private String referer;

    @Bean
    public RestTemplate mcxRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", userAgent);
            request.getHeaders().add("Accept", accept);
            request.getHeaders().add("Accept-Language", acceptLanguage);
            request.getHeaders().add("Origin", origin);
            request.getHeaders().add("Referer", referer);
            // Add Cookie logic here if needed, for now starting simple
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
