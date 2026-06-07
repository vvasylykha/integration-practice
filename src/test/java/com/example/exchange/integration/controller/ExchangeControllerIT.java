package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Exchange API (/api/v1/exchanges).
 *
 * This class violates two best practices ON PURPOSE.
 *
 * TODO (BP3 — Stable External Boundaries):
 *   performExchange() fetches the rate from the REAL exchange-rate API over the internet, so the
 *   happy-path test is slow, non-deterministic and fails offline / in CI. Add the annotation that
 *   starts a local WireMock server on port 8099, point exchange.api.url at it, and stub GET /USD.
 *
 * TODO (BP1 — Test Isolation):
 *   The shared static field, the hard-coded "user123", and the missing @AfterEach all leak state
 *   between tests. Give each test its own data and clean up afterwards. NOTE: @Transactional
 *   rollback does NOT work here — RestAssured hits a real HTTP port and the server runs its own
 *   transaction, so cleanup must be an explicit deleteAll() (exchanges BEFORE balances).
 */
class ExchangeControllerIT extends ApiIntegrationTest {

    @Autowired
    ExchangeRepository exchangeRepository;
    @Autowired
    UserBalanceRepository userBalanceRepository;

    private static Long lastExchangeId;  // ❌ BP1: shared static state between tests

    @BeforeEach
    void setUp() {
        // Gives the happy path a balance to spend, so it can reach 201.
        // ❌ BP1: always the same "user123", and this deposit is never cleaned up between tests.
        given().contentType(JSON).body(depositBody("user123", "USD", "1000.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201);
    }

    // TODO (BP1): add @AfterEach that deletes exchanges, then balances (FK order).

    @Test
    void shouldPerformExchangeAndReturn201() {
        // ❌ BP3: this POST triggers a live call to the real rate API (slow / online-only).
        int id = given().contentType(JSON).body(exchangeBody("user123", "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(201)
                .extract().path("id");
        lastExchangeId = (long) id;  // ❌ BP1: leaks to the next test
    }

    @Test
    void shouldGetExchangeById() {
        // ❌ BP1: depends on shouldPerformExchangeAndReturn201 having run first
        given().when().get("/api/v1/exchanges/" + lastExchangeId)
                .then().statusCode(200)
                .body("id", equalTo(1));  // TODO (BP1): BIGSERIAL keeps growing — don't assert a fixed id
    }

    // TODO (BP1/BP3): implement the rest, each self-contained and using the WireMock stub:
    //   shouldDeductBalanceAfterExchange, shouldReturn400WhenInsufficientBalance,
    //   shouldReturn400WhenSameCurrency, shouldReturn400OnValidationError,
    //   shouldReturn404WhenExchangeNotFound, shouldGetUserExchangesPaginated,
    //   shouldReturn503WhenExternalApiIsDown  (can only be shown once WireMock can force a 500)

    private Map<String, Object> depositBody(String userId, String currency, String amount) {
        return Map.of("userId", userId, "currency", currency, "amount", amount);
    }

    private Map<String, Object> exchangeBody(String userId, String from, String to, String amount) {
        return Map.of("userId", userId, "fromCurrency", from, "toCurrency", to, "amount", amount);
    }
}