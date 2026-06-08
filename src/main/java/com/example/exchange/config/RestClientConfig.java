package com.example.exchange.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@Configuration
@EnableRetry
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${exchange.api.url}")
    private String apiUrl;

    @Value("${exchange.api.timeout:5000}")
    private int timeout;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .requestInterceptor((request, body, execution) -> {
                    String host = request.getURI().getHost();
                    boolean local = "localhost".equals(host) || "127.0.0.1".equals(host);
                    log.info("Exchange-rate API call -> {} {}  [{}]", request.getMethod(), request.getURI(), local ? "LOCAL STUB (e.g. WireMock)" : "LIVE EXTERNAL API");
                    return execution.execute(request, body);
                })
                .build();
    }
}
