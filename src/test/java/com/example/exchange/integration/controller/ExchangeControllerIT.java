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
 * Component diagram: diagrams/png/ExchangeController Component Architecture.png
 *   (ExchangeController -> services -> ExchangeRepository, plus the published audit event)
 *
 * This class violates two best practices ON PURPOSE.
 *
 * TODO (REQUIRED):
 *   BP3 — Stable External Boundaries
 *   performExchange() fetches the rate from the REAL exchange-rate API over the internet, so the
 *   happy-path test is slow, non-deterministic and fails offline / in CI. Add the annotation that
 *   starts a local WireMock server on port 8099, point exchange.api.url at it, and stub GET /USD.
 *
 * TODO (REQUIRED):
 *   BP1 — Test Isolation & Independence
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

    // TODO (REQUIRED):
    //  BP1 — Test Isolation & Independence
    //  Add @AfterEach that deletes exchanges, then balances (FK order).

    @Test
    void shouldPerformExchangeAndReturn201() {
        // WHAT IT TESTS: happy path POST /api/v1/exchanges → 201 and an ExchangeResponse, using a
        //   rate from the WireMock stub (BP3). Each test owns its data and id (no shared state, BP1).
        //
        // ❌ BP3: this POST triggers a live call to the real rate API (slow / online-only).
        int id = given().contentType(JSON).body(exchangeBody("user123", "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(201)
                .extract().path("id");
        lastExchangeId = (long) id;  // ❌ BP1: leaks to the next test
    }

    @Test
    void shouldGetExchangeById() {
        // WHAT IT TESTS: GET /api/v1/exchanges/{id} → 200 for an exchange THIS test created;
        //   capture the id locally and assert on the returned fields (don't assert a fixed
        //   BIGSERIAL id — it keeps growing, BP1).
        //
        // ❌ BP1: depends on shouldPerformExchangeAndReturn201 having run first
        given().when().get("/api/v1/exchanges/" + lastExchangeId)
                .then().statusCode(200)
                .body("id", equalTo(1));  // TODO (BP1): BIGSERIAL keeps growing — don't assert a fixed id
    }

    // TODO (REQUIRED):
    //   Test that after a successful exchange the source balance is reduced by amount + commission
    //   and the target balance grows by the converted amount (verify via balance endpoints / repo).

    // TODO (REQUIRED):
    //   Test that exchanging more than the user holds → 400 (InsufficientBalanceException) and no
    //   balances are mutated.

    // TODO (OPTIONAL):
    //   Test that fromCurrency == toCurrency → 400.

    // TODO (OPTIONAL):
    //   Test a malformed request body (blank userId, bad currency code, non-positive amount)
    //   → 400 with an error body.

    // TODO (OPTIONAL):
    //   Test GET /api/v1/exchanges/{id} for a non-existent id → 404.

    // TODO (OPTIONAL):
    //   Test GET /api/v1/exchanges/user/{userId}?page&size returns the user's exchanges honouring
    //   the pagination params.

    // TODO (OPTIONAL):
    //   Test that when the rate API is unavailable (WireMock stub returns 500 on every call, BP3),
    //   POST /api/v1/exchanges surfaces 503 after retries are exhausted.

    private Map<String, Object> depositBody(String userId, String currency, String amount) {
        return Map.of("userId", userId, "currency", currency, "amount", amount);
    }

    private Map<String, Object> exchangeBody(String userId, String from, String to, String amount) {
        return Map.of("userId", userId, "fromCurrency", from, "toCurrency", to, "amount", amount);
    }
}