package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for the Rate API (GET /api/v1/rates).
 *
 * BP3 (Stable External Boundaries) — the upstream exchange-rate API is intercepted by a local
 * WireMock server on port 8099 (matches exchange.api.url in application-test.properties). The
 * suite never touches the internet, so it is fast, deterministic and CI-safe. WireMock resets
 * its stubs between tests, so each test registers its own.
 *
 * BP1 (Test Isolation — cached-instance leak) — RateService caches rates in Redis. The cache
 * is cleared before every test via CacheManager (NOT @DirtiesContext, which would destroy the
 * Spring context cache and slow the whole suite), so a rate cached by one test never leaks into
 * the next.
 */
@WireMockTest(httpPort = 8099)
class RateControllerIT extends ApiIntegrationTest {

    private static final String USD_RATES = """
            { "base": "USD", "date": "2025-01-01", "rates": { "EUR": 0.90, "UAH": 40.0, "USD": 1.0 } }
            """;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        // BP1: start every test with a cold cache so cached rates don't leak between tests.
        cacheManager.getCacheNames()
                .forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void shouldReturnExchangeRate() {
        stubFor(get(urlEqualTo("/USD")).willReturn(okJson(USD_RATES)));

        given()
                .when().get("/api/v1/rates/USD/EUR")
                .then().statusCode(200)
                .body("baseCurrency", equalTo("USD"))
                .body("targetCurrency", equalTo("EUR"))
                .body("rate", equalTo(0.9f))          // BP3: assert the value WE stubbed, not a live rate
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturnAllRatesForBase() {
        stubFor(get(urlEqualTo("/USD")).willReturn(okJson(USD_RATES)));

        given()
                .when().get("/api/v1/rates/USD")
                .then().statusCode(200)
                .body("EUR", equalTo(0.9f))
                .body("UAH", equalTo(40.0f));
    }

    @Test
    void shouldCacheRateAndCallApiOnlyOnce() {
        stubFor(get(urlEqualTo("/USD")).willReturn(okJson(USD_RATES)));

        given().when().get("/api/v1/rates/USD/EUR").then().statusCode(200);
        given().when().get("/api/v1/rates/USD/EUR").then().statusCode(200);

        // The second request is served from Redis, so the upstream is hit exactly once.
        // This is only reliable because clearCaches() reset the cache before this test (BP1).
        verify(1, getRequestedFor(urlEqualTo("/USD")));
    }

    @Test
    void shouldReturn400OnInvalidBaseCode() {
        // Bean Validation (@ValidCurrencyCode) rejects the 2-letter code before any upstream call.
        given()
                .when().get("/api/v1/rates/XX/EUR")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());

        verify(0, getRequestedFor(urlEqualTo("/XX")));   // validation short-circuits the call
    }

    @Test
    void shouldReturn400OnInvalidTargetCode() {
        given()
                .when().get("/api/v1/rates/USD/XX")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldRetryAndSucceedAfterTransientFailure() {
        // Stateful scenario: fail twice, then succeed. Matches @Retryable (max 3 attempts).
        stubFor(get(urlEqualTo("/USD")).inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-2"));
        stubFor(get(urlEqualTo("/USD")).inScenario("retry")
                .whenScenarioStateIs("attempt-2")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-3"));
        stubFor(get(urlEqualTo("/USD")).inScenario("retry")
                .whenScenarioStateIs("attempt-3")
                .willReturn(okJson(USD_RATES)));

        given()
                .when().get("/api/v1/rates/USD/EUR")
                .then().statusCode(200)
                .body("rate", equalTo(0.9f));

        verify(3, getRequestedFor(urlEqualTo("/USD")));
    }

    @Test
    void shouldReturn503AfterMaxRetryAttemptsExhausted() {
        stubFor(get(urlEqualTo("/USD")).willReturn(aResponse().withStatus(500)));

        given()
                .when().get("/api/v1/rates/USD/EUR")
                .then().statusCode(503);   // @Recover throws ExchangeRateNotFoundException -> 503

        verify(3, getRequestedFor(urlEqualTo("/USD")));
    }
}