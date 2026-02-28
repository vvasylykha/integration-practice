package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.BaseIntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@WireMockTest(httpPort = 8099)
class RateControllerIT extends BaseIntegrationTest {

    private static final String USD_RATES_RESPONSE = """
            {
              "base": "USD",
              "date": "2024-01-01",
              "rates": {
                "EUR": 0.850000,
                "GBP": 0.730000
              }
            }
            """;

    private static final String EUR_RATES_RESPONSE = """
            {
              "base": "EUR",
              "date": "2024-01-01",
              "rates": {
                "USD": 1.180000,
                "GBP": 0.860000
              }
            }
            """;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void shouldReturnExchangeRate() {
        stubFor(get(urlPathEqualTo("/USD"))
                .willReturn(okJson(USD_RATES_RESPONSE)));

        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "USD", "EUR")
        .then()
                .statusCode(200)
                .body("baseCurrency", equalTo("USD"))
                .body("targetCurrency", equalTo("EUR"))
                .body("rate", comparesEqualTo(0.85f))
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturnAllRatesForBase() {
        stubFor(get(urlPathEqualTo("/USD"))
                .willReturn(okJson(USD_RATES_RESPONSE)));

        given()
        .when()
                .get("/api/v1/rates/{base}", "USD")
        .then()
                .statusCode(200)
                .body("EUR", comparesEqualTo(0.85f))
                .body("GBP", comparesEqualTo(0.73f));
    }

    @Test
    void shouldCacheRateAndCallApiOnlyOnce() {
        stubFor(get(urlPathEqualTo("/EUR"))
                .willReturn(okJson(EUR_RATES_RESPONSE)));

        given().when().get("/api/v1/rates/{base}/{target}", "EUR", "USD").then().statusCode(200);
        given().when().get("/api/v1/rates/{base}/{target}", "EUR", "USD").then().statusCode(200);

        verify(1, getRequestedFor(urlPathEqualTo("/EUR")));
    }

    @Test
    void shouldReturn400OnInvalidBaseCode() {
        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "INVALID", "EUR")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnInvalidTargetCode() {
        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "USD", "XX")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldRetryAndSucceedAfterTransientFailure() {
        stubFor(get(urlPathEqualTo("/GBP"))
                .inScenario("retry-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serverError())
                .willSetStateTo("second-attempt"));

        stubFor(get(urlPathEqualTo("/GBP"))
                .inScenario("retry-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(serverError())
                .willSetStateTo("third-attempt"));

        stubFor(get(urlPathEqualTo("/GBP"))
                .inScenario("retry-success")
                .whenScenarioStateIs("third-attempt")
                .willReturn(okJson("""
                        {
                          "base": "GBP",
                          "date": "2024-01-01",
                          "rates": { "USD": 1.370000 }
                        }
                        """)));

        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "GBP", "USD")
        .then()
                .statusCode(200)
                .body("rate", notNullValue());

        verify(3, getRequestedFor(urlPathEqualTo("/GBP")));
    }

    @Test
    void shouldReturn503AfterMaxRetryAttemptsExhausted() {
        stubFor(get(urlPathEqualTo("/JPY"))
                .willReturn(serverError()));

        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "JPY", "USD")
        .then()
                .statusCode(503)
                .body("status", equalTo(503))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());

        verify(3, getRequestedFor(urlPathEqualTo("/JPY")));
    }
}
