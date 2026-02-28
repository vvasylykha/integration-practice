package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.BaseIntegrationTest;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.repository.UserBalanceRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@WireMockTest(httpPort = 8099)
class ExchangeControllerIT extends BaseIntegrationTest {

    private static final String TEST_USER = "exchange-test-user";
    private static final String RATE_RESPONSE = """
            {
              "base": "USD",
              "date": "2024-01-01",
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

    @BeforeEach
    void setUpWireMockAndBalance() {
        stubFor(get(urlPathEqualTo("/USD"))
                .willReturn(okJson(RATE_RESPONSE)));

        depositBalance(TEST_USER, "USD", 1000.00);
    }

    @AfterEach
    void cleanUp() {
        exchangeRepository.findByUserId(TEST_USER)
                .forEach(e -> exchangeRepository.deleteById(e.getId()));
        userBalanceRepository.findByUserId(TEST_USER)
                .forEach(b -> userBalanceRepository.deleteById(b.getId()));
    }

    @Test
    void shouldPerformExchangeSuccessfullyAndReturn201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 100.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", equalTo(TEST_USER))
                .body("fromCurrency", equalTo("USD"))
                .body("toCurrency", equalTo("EUR"))
                .body("amount", comparesEqualTo(100.00f))
                .body("exchangeRate", comparesEqualTo(0.85f))
                .body("commission", notNullValue())
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void shouldDeductBalanceAfterExchange() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 100.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(201);

        given()
        .when()
                .get("/api/v1/balances/{userId}/{currency}", TEST_USER, "USD")
        .then()
                .statusCode(200)
                .body("balance", lessThan(1000.00f));
    }

    @Test
    void shouldReturn400WhenInsufficientBalance() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 9999.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400WhenSameCurrency() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "USD",
                          "amount": 100.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnValidationError() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 100.00
                        }
                        """)
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldGetExchangeById() {
        Integer exchangeId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 50.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
        .when()
                .get("/api/v1/exchanges/{id}", exchangeId)
        .then()
                .statusCode(200)
                .body("id", equalTo(exchangeId))
                .body("userId", equalTo(TEST_USER));
    }

    @Test
    void shouldReturn404WhenExchangeNotFound() {
        given()
        .when()
                .get("/api/v1/exchanges/{id}", 999999L)
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldGetUserExchangesPaginated() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"userId":"%s","fromCurrency":"USD","toCurrency":"EUR","amount":10.00}
                        """.formatted(TEST_USER))
        .when().post("/api/v1/exchanges").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"userId":"%s","fromCurrency":"USD","toCurrency":"GBP","amount":10.00}
                        """.formatted(TEST_USER))
        .when().post("/api/v1/exchanges").then().statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 1)
        .when()
                .get("/api/v1/exchanges/user/{userId}", TEST_USER)
        .then()
                .statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    void shouldReturn503WhenExternalApiIsDown() {
        stubFor(get(urlPathEqualTo("/USD"))
                .willReturn(serverError()));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "fromCurrency": "USD",
                          "toCurrency": "EUR",
                          "amount": 100.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/exchanges")
        .then()
                .statusCode(503)
                .body("status", equalTo(503))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    private void depositBalance(String userId, String currency, double amount) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "currency": "%s",
                          "amount": %s
                        }
                        """.formatted(userId, currency, amount))
        .when()
                .post("/api/v1/balances/deposit")
        .then()
                .statusCode(201);
    }
}
