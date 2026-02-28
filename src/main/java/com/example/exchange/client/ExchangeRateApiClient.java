package com.example.exchange.client;

import com.example.exchange.client.dto.ExchangeRateApiResponse;
import com.example.exchange.exception.ExchangeRateNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

    private final RestClient restClient;

    @Retryable(
            retryFor = {RestClientException.class},
            maxAttemptsExpression = "${exchange.api.retry.max-attempts:3}",
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExchangeRateApiResponse fetchExchangeRates(String baseCurrency) {
        log.info("Fetching exchange rates for base currency: {}", baseCurrency);

        ExchangeRateApiResponse response = restClient.get()
                .uri("/{base}", baseCurrency)
                .retrieve()
                .body(ExchangeRateApiResponse.class);

        if (response == null || response.getRates() == null) {
            throw new ExchangeRateNotFoundException("Failed to fetch exchange rates for " + baseCurrency);
        }

        log.info("Successfully fetched {} exchange rates for {}",
                response.getRates().size(), baseCurrency);
        return response;
    }

    @Recover
    public ExchangeRateApiResponse recover(RestClientException e, String baseCurrency) {
        log.error("All retries exhausted for {}: {}", baseCurrency, e.getMessage());
        throw new ExchangeRateNotFoundException(
                "Failed to fetch exchange rates for " + baseCurrency + ": " + e.getMessage());
    }
}
