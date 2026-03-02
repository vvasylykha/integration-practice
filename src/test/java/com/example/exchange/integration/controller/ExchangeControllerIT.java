package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Exchange REST API ({@code /api/v1/exchanges}).
 *
 * <p><b>Task:</b> add the class-level annotation that starts a WireMock HTTP server
 * on port {@code 8099} before each test. This port matches {@code rate-api.base-url}
 * in {@code application-test.properties} and intercepts outbound calls to the
 * external exchange-rate provider.
 */
class ExchangeControllerIT extends ApiIntegrationTest {

    /**
     * Before each test: stub the external rate API to return known exchange rates for USD,
     * and deposit an initial USD balance for the test user.
     */
    @BeforeEach
    void setUpWireMockAndBalance() {
        // TODO: implement
    }

    /**
     * After each test: delete all exchange and balance records for the test user.
     * Delete exchanges before balances to respect foreign-key constraints.
     */
    @AfterEach
    void cleanUp() {
        // TODO: implement
    }

    /**
     * A valid USD→EUR exchange request should return HTTP 201 Created with
     * {@code id}, {@code userId}, {@code fromCurrency}, {@code toCurrency},
     * {@code amount}, {@code exchangeRate}, {@code commission}, and
     * {@code status} ({@code "COMPLETED"}) in the response body.
     *
     * <p>Endpoint: {@code POST /api/v1/exchanges}
     */
    @Test
    void shouldPerformExchangeSuccessfullyAndReturn201() {
        // TODO: implement
    }

    /**
     * After a successful USD→EUR exchange the source USD balance should be lower
     * than the initially deposited amount.
     *
     * <p>Endpoints: {@code POST /api/v1/exchanges}, then
     * {@code GET /api/v1/balances/{userId}/{currency}}
     */
    @Test
    void shouldDeductBalanceAfterExchange() {
        // TODO: implement
    }

    /**
     * An exchange with an amount exceeding the available balance should return
     * HTTP 400 Bad Request with {@code status}, {@code message}, and {@code timestamp}
     * in the error body.
     *
     * <p>Endpoint: {@code POST /api/v1/exchanges}
     */
    @Test
    void shouldReturn400WhenInsufficientBalance() {
        // TODO: implement
    }

    /**
     * An exchange where {@code fromCurrency} and {@code toCurrency} are identical
     * should return HTTP 400 Bad Request with {@code status}, {@code message},
     * and {@code timestamp} in the error body.
     *
     * <p>Endpoint: {@code POST /api/v1/exchanges}
     */
    @Test
    void shouldReturn400WhenSameCurrency() {
        // TODO: implement
    }

    /**
     * A request with a blank {@code userId} should fail Bean Validation and return
     * HTTP 400 Bad Request with {@code status}, {@code errors}, and {@code timestamp}
     * in the error body.
     *
     * <p>Endpoint: {@code POST /api/v1/exchanges}
     */
    @Test
    void shouldReturn400OnValidationError() {
        // TODO: implement
    }

    /**
     * An exchange that was just created should be retrievable by its id with HTTP 200 OK
     * and matching {@code id} and {@code userId} in the response body.
     *
     * <p>Endpoint: {@code GET /api/v1/exchanges/{id}}
     */
    @Test
    void shouldGetExchangeById() {
        // TODO: implement
    }

    /**
     * Requesting an exchange with a non-existent id should return HTTP 404 Not Found
     * with {@code status}, {@code message}, and {@code timestamp} in the error body.
     *
     * <p>Endpoint: {@code GET /api/v1/exchanges/{id}}
     */
    @Test
    void shouldReturn404WhenExchangeNotFound() {
        // TODO: implement
    }

    /**
     * When two exchanges exist and page size is 1, the paginated endpoint should
     * return HTTP 200 OK with exactly 1 exchange per page.
     *
     * <p>Endpoint: {@code GET /api/v1/exchanges/user/{userId}?page=0&size=1}
     */
    @Test
    void shouldGetUserExchangesPaginated() {
        // TODO: implement
    }

    /**
     * When the external rate API returns a server error, the exchange endpoint should
     * return HTTP 503 Service Unavailable after exhausting all retry attempts.
     * Override the WireMock stub to return a server error before sending the request.
     *
     * <p>Endpoint: {@code POST /api/v1/exchanges}
     */
    @Test
    void shouldReturn503WhenExternalApiIsDown() {
        // TODO: implement
    }
}
