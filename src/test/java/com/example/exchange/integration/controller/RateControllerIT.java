package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Rate REST API ({@code /api/v1/rates}),
 *
 * <p><b>Task:</b> add the class-level annotation that starts a WireMock HTTP server
 * on port {@code 8099} before each test. The port matches {@code rate-api.base-url}
 * in {@code application-test.properties}. Stubs are reset between tests, so each
 * test must register its own.
 */
class RateControllerIT extends ApiIntegrationTest {

    /**
     * Before each test: clear all Spring Cache entries to prevent cached rates
     * from a previous test being served instead of the registered WireMock response.
     */
    @BeforeEach
    void clearCache() {
        // TODO: implement
    }

    /**
     * Requesting the rate for a valid base and target currency should return HTTP 200 OK
     * with {@code baseCurrency}, {@code targetCurrency}, {@code rate}, and {@code timestamp}
     * in the response body.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldReturnExchangeRate() {
        // TODO: implement
    }

    /**
     * Requesting all rates for a base currency should return HTTP 200 OK with a JSON map
     * of target currency codes to their rate values.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}}
     */
    @Test
    void shouldReturnAllRatesForBase() {
        // TODO: implement
    }

    /**
     * Requesting the same rate twice should result in only one call to the external API;
     * the second request must be served from cache.
     * Verify with WireMock that the external endpoint was called exactly once.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldCacheRateAndCallApiOnlyOnce() {
        // TODO: implement
    }

    /**
     * A request with an invalid (non 3-letter) base currency code should return
     * HTTP 400 Bad Request with {@code status}, {@code errors}, and {@code timestamp}.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldReturn400OnInvalidBaseCode() {
        // TODO: implement
    }

    /**
     * A request with an invalid (non 3-letter) target currency code should return
     * HTTP 400 Bad Request with {@code status}, {@code errors}, and {@code timestamp}.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldReturn400OnInvalidTargetCode() {
        // TODO: implement
    }

    /**
     * When the external API fails on the first two attempts and succeeds on the third,
     * the endpoint should return HTTP 200 OK and the external API should have been
     * called exactly 3 times.
     * Use WireMock stateful scenarios to simulate the sequence of failures and success.
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldRetryAndSucceedAfterTransientFailure() {
        // TODO: implement
    }

    /**
     * When all retry attempts are exhausted (external API always returns 500), the endpoint
     * should return HTTP 503 Service Unavailable. Verify that the external API was called
     * exactly 3 times (max retry attempts).
     *
     * <p>Endpoint: {@code GET /api/v1/rates/{base}/{target}}
     */
    @Test
    void shouldReturn503AfterMaxRetryAttemptsExhausted() {
        // TODO: implement
    }
}
