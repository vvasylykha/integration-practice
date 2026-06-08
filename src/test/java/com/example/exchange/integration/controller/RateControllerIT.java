package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Rate API (GET /api/v1/rates).
 *
 * Component diagram: diagrams/png/RateController Integration Scope.png
 *   (RateController -> RateService -> ExchangeRateApiClient -> external API, with the Redis cache)
 *
 * This class violates two best practices ON PURPOSE. Your job is to fix them.
 *
 * TODO (REQUIRED):
 *   BP3 — Stable External Boundaries
 *   These tests call the REAL exchange-rate API over the internet, so they are slow,
 *   non-deterministic (rates change every day) and fail offline / in CI.
 *     1. Add the annotation that starts a local WireMock server on port 8099.
 *     2. Point exchange.api.url at it (see application-test.properties).
 *     3. Stub the upstream call — the client requests GET /{base}, e.g. GET /USD.
 *
 * TODO (REQUIRED):
 *   BP1 — Test Isolation & Independence
 *   RateService caches rates in Redis (@Cacheable on "rates" / "rates-all"). The cache
 *   survives BETWEEN tests, so a value cached by one test is served to the next and a test
 *   that expects a fresh upstream call (or a failure) gets a stale cache hit instead.
 *     -> Inject CacheManager and clear every cache in a @BeforeEach so each test starts cold.
 *     -> Do NOT use @DirtiesContext for this — it destroys the Spring context cache (slow, see BP4).
 */
class RateControllerIT extends ApiIntegrationTest {

    @Test
    void shouldReturnExchangeRate() {
        // WHAT IT TESTS: happy path GET /api/v1/rates/{base}/{target} → 200 with a correct
        //   RateResponse (baseCurrency, targetCurrency, rate) equal to the value YOU stubbed
        //   in WireMock (BP3).
        //
        // ❌ BP3: hits the live API. The rate drifts daily, so this assertion is non-deterministic,
        //         and the whole test fails when there is no internet.
        given()
                .when().get("/api/v1/rates/USD/EUR")
                .then().statusCode(200)
                .body("baseCurrency", equalTo("USD"))
                .body("targetCurrency", equalTo("EUR"))
                .body("rate", equalTo(0.92f));  // TODO (BP3): assert a value YOU stubbed, not a live rate
    }

    // TODO (REQUIRED):
    //   Test the happy path GET /api/v1/rates/{base} → 200 with the map of rates for the base
    //   currency; values come from the WireMock stub of GET /USD (BP3).

    // TODO (REQUIRED):
    //   Test path validation: GET /api/v1/rates/XX/EUR (invalid base) → 400 with an error body
    //   (status/errors/timestamp). No WireMock needed — validation runs BEFORE any upstream call.

    // TODO (OPTIONAL):
    //   Test path validation: GET /api/v1/rates/USD/XX (invalid target) → 400.

    // TODO (OPTIONAL):
    //   Test Redis caching: two consecutive GET /api/v1/rates/USD/EUR hit the upstream EXACTLY
    //   once (the 2nd response is served from cache); verify the call count via WireMock.
    //   Reliable only when the cache is cleared in @BeforeEach (BP1).

    // TODO (OPTIONAL):
    //   Test retry on transient failure: a stateful WireMock scenario returns 500 on the first
    //   two calls and a valid body on the third. Expect 200 and EXACTLY 3 upstream calls
    //   (matches exchange.api.retry.max-attempts).

    // TODO (OPTIONAL):
    //   Test exhausted retries: GET /USD always returns 500; after all attempts the endpoint
    //   returns 503 and makes EXACTLY 3 upstream calls.
}