package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.repository.UserBalanceRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for the Exchange API (/api/v1/exchanges).
 *
 * Component diagram: diagrams/png/ExchangeController Component Architecture.png
 *   (ExchangeController -> services -> ExchangeRepository, plus the published audit event)
 *
 * BP3 — the upstream rate API is intercepted by a local WireMock server on port 8099 (matches
 * exchange.api.url in application-test.properties). The suite runs offline and deterministically.
 * Each test gets a known rate via the stub registered in @BeforeEach; tests that need a failure
 * override that stub locally.
 *
 * BP1 — @BeforeEach gives the user a fresh balance and @AfterEach removes all data (exchanges
 * before balances, FK order). No shared static state; created ids are captured locally and
 * asserted with notNullValue() rather than fixed numbers. @Transactional is intentionally NOT
 * used: these tests cross a real HTTP boundary, so the server commits in its own transaction.
 */
@WireMockTest(httpPort = 8099)
class ExchangeControllerIT extends ApiIntegrationTest {

    private static final String USER = "exchange-user";

    @Autowired
    ExchangeRepository exchangeRepository;
    @Autowired
    UserBalanceRepository userBalanceRepository;

    @BeforeEach
    void setUp() {
        stubFor(get(urlEqualTo("/USD")).willReturn(okJson("""
                { "base": "USD", "date": "2025-01-01", "rates": { "EUR": 0.90, "USD": 1.0 } }
                """)));
        given().contentType(JSON).body(depositBody(USER, "USD", "1000.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201);
    }

    @AfterEach
    void cleanUp() {
        exchangeRepository.deleteAll();
        userBalanceRepository.deleteAll();
    }

    @Test
    void shouldPerformExchangeAndReturn201() {
        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("userId", equalTo(USER))
                .body("fromCurrency", equalTo("USD"))
                .body("toCurrency", equalTo("EUR"))
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void shouldDeductBalanceAfterExchange() {
        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(201);

        // 1000 - (100 + 0.5% commission) = 899.50, i.e. strictly less than the deposited 1000.
        given().when().get("/api/v1/balances/" + USER + "/USD")
                .then().statusCode(200)
                .body("balance", lessThan(1000.0f));
    }

    @Test
    void shouldReturn400WhenInsufficientBalance() {
        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100000.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400WhenSameCurrency() {
        // Source == target is rejected by the service before any rate lookup.
        given().contentType(JSON).body(exchangeBody(USER, "USD", "USD", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnValidationError() {
        // Blank userId fails Bean Validation -> MethodArgumentNotValidException -> 400 with errors map.
        given().contentType(JSON).body(exchangeBody("", "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldGetExchangeById() {
        int id = given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges").then().statusCode(201)
                .extract().path("id");

        given().when().get("/api/v1/exchanges/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("userId", equalTo(USER));
    }

    @Test
    void shouldReturn404WhenExchangeNotFound() {
        given().when().get("/api/v1/exchanges/999999")
                .then().statusCode(404)
                .body("status", equalTo(404))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldGetUserExchangesPaginated() {
        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges").then().statusCode(201);
        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges").then().statusCode(201);

        // Two exchanges exist; page size 1 returns exactly one element.
        given().when().get("/api/v1/exchanges/user/" + USER + "?page=0&size=1")
                .then().statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    void shouldReturn503WhenExternalApiIsDown() {
        // Override the @BeforeEach stub: the rate API always fails, retries are exhausted -> 503.
        stubFor(get(urlEqualTo("/USD")).willReturn(aResponse().withStatus(500)));

        given().contentType(JSON).body(exchangeBody(USER, "USD", "EUR", "100.00"))
                .when().post("/api/v1/exchanges")
                .then().statusCode(503);
    }

    private Map<String, Object> depositBody(String userId, String currency, String amount) {
        return Map.of("userId", userId, "currency", currency, "amount", amount);
    }

    private Map<String, Object> exchangeBody(String userId, String from, String to, String amount) {
        return Map.of("userId", userId, "fromCurrency", from, "toCurrency", to, "amount", amount);
    }
}