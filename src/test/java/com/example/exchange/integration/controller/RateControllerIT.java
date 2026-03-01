package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;

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
class RateControllerIT extends ApiIntegrationTest {

    private static final String USD_RATES_RESPONSE = """
            {
              "base": "USD",
              "date": "2026-03-01",
              "rates": {
                "EUR": 0.850000,
                "GBP": 0.730000
              }
            }
            """;

    private static final String EUR_RATES_RESPONSE = """
            {
              "base": "EUR",
              "date": "2026-03-01",
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
                .statusCode(HttpStatus.OK.value())
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
                .statusCode(HttpStatus.OK.value())
                .body("EUR", comparesEqualTo(0.85f))
                .body("GBP", comparesEqualTo(0.73f));
    }

    @Test
    void shouldCacheRateAndCallApiOnlyOnce() {
        stubFor(get(urlPathEqualTo("/EUR"))
                .willReturn(okJson(EUR_RATES_RESPONSE)));

        given().when().get("/api/v1/rates/{base}/{target}", "EUR", "USD").then().statusCode(HttpStatus.OK.value());
        given().when().get("/api/v1/rates/{base}/{target}", "EUR", "USD").then().statusCode(HttpStatus.OK.value());

        verify(1, getRequestedFor(urlPathEqualTo("/EUR")));
    }

    @Test
    void shouldReturn400OnInvalidBaseCode() {
        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "INVALID", "EUR")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnInvalidTargetCode() {
        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "USD", "XX")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
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
                          "date": "2026-03-01",
                          "rates": { "USD": 1.370000 }
                        }
                        """)));

        given()
        .when()
                .get("/api/v1/rates/{base}/{target}", "GBP", "USD")
        .then()
                .statusCode(HttpStatus.OK.value())
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
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());

        verify(3, getRequestedFor(urlPathEqualTo("/JPY")));
    }
}
