package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.repository.UserBalanceRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@WireMockTest(httpPort = 8099)
class ExchangeControllerIT extends ApiIntegrationTest {

    private static final String TEST_USER = "exchange-test-user";

    private static final double INITIAL_BALANCE = 1000.00;
    private static final double INSUFFICIENT_AMOUNT = 9999.00;
    private static final Long NON_EXISTENT_ID = 999999L;
    private static final String RATE_RESPONSE = """
            {
              "base": "USD",
              "date": "2026-03-01",
              "rates": {
                "EUR": 0.850000,
                "GBP": 0.730000,
                "USD": 1.000000
              }
            }
            """;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

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
