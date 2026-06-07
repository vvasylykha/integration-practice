package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Rate API (GET /api/v1/rates).
 *
 * This class violates two best practices ON PURPOSE. Your job is to fix them.
 *
 * TODO (BP3 — Stable External Boundaries):
 *   These tests call the REAL exchange-rate API over the internet, so they are slow,
 *   non-deterministic (rates change every day) and fail offline / in CI.
 *     1. Add the annotation that starts a local WireMock server on port 8099.
 *     2. Point exchange.api.url at it (see application-test.properties).
 *     3. Stub the upstream call — the client requests GET /{base}, e.g. GET /USD.
 *
 * TODO (BP1 — Test Isolation, cached-instance leak):
 *   RateService caches rates in Redis (@Cacheable on "rates" / "rates-all"). The cache
 *   survives BETWEEN tests, so a value cached by one test is served to the next and a test
 *   that expects a fresh upstream call (or a failure) gets a stale cache hit instead.
 *     -> Inject CacheManager and clear every cache in a @BeforeEach so each test starts cold.
 *     -> Do NOT use @DirtiesContext for this — it destroys the Spring context cache (slow, see BP4).
 */
class RateControllerIT extends ApiIntegrationTest {

    @Test
    void shouldReturnExchangeRate() {
        // ❌ BP3: hits the live API. The rate drifts daily, so this assertion is non-deterministic,
        //         and the whole test fails when there is no internet.
        given()
                .when().get("/api/v1/rates/USD/EUR")
                .then().statusCode(200)
                .body("baseCurrency", equalTo("USD"))
                .body("targetCurrency", equalTo("EUR"))
                .body("rate", equalTo(0.92f));  // TODO (BP3): assert a value YOU stubbed, not a live rate
    }

    // NOTE: the methods below are placeholders. Most of them only become meaningful once WireMock
    //       is wired (BP3) and the cache is cleared per test (BP1) — you cannot force the live API
    //       to fail or to return a fixed value, so implement them after fixing BP3/BP1.

    @Test
    void shouldReturnAllRatesForBase() {
        // TODO (BP3): stub GET /USD, call GET /api/v1/rates/USD, assert the returned map of rates.
    }

    @Test
    void shouldCacheRateAndCallApiOnlyOnce() {
        // TODO (BP1 + BP3): stub GET /USD, call GET /api/v1/rates/USD/EUR twice, then verify with
        //                   WireMock that the upstream was hit EXACTLY ONCE (2nd response from Redis).
        //                   This only passes reliably if the cache is cleared in @BeforeEach (BP1).
    }

    @Test
    void shouldReturn400OnInvalidBaseCode() {
        // TODO: call GET /api/v1/rates/XX/EUR (invalid base), assert HTTP 400 with status/errors/timestamp.
        //       No WireMock needed here — validation runs before any upstream call.
    }

    @Test
    void shouldReturn400OnInvalidTargetCode() {
        // TODO: call GET /api/v1/rates/USD/XX (invalid target), assert HTTP 400.
    }

    @Test
    void shouldRetryAndSucceedAfterTransientFailure() {
        // TODO (BP3): WireMock stateful scenario — return 500 on the first two calls, then a valid
        //             body on the third. Assert HTTP 200 and exactly 3 upstream calls
        //             (matches exchange.api.retry.max-attempts).
    }

    @Test
    void shouldReturn503AfterMaxRetryAttemptsExhausted() {
        // TODO (BP3): stub GET /USD to always return 500. Assert the endpoint returns HTTP 503
        //             after retries are exhausted, and verify exactly 3 upstream calls.
    }
}