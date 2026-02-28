package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.BaseIntegrationTest;
import com.example.exchange.repository.UserBalanceRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class BalanceControllerIT extends BaseIntegrationTest {

    private static final String TEST_USER = "balance-test-user";

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @AfterEach
    void cleanUp() {
        userBalanceRepository.findByUserId(TEST_USER)
                .forEach(b -> userBalanceRepository.deleteById(b.getId()));
    }

    @Test
    void shouldDepositAndReturn201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "currency": "USD",
                          "amount": 500.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/balances/deposit")
        .then()
                .statusCode(201)
                .body("userId", equalTo(TEST_USER))
                .body("currency", equalTo("USD"))
                .body("balance", comparesEqualTo(500.00f));
    }

    @Test
    void shouldGetAllBalancesForUser() {
        depositBalance(TEST_USER, "USD", 100.00);
        depositBalance(TEST_USER, "EUR", 200.00);

        given()
        .when()
                .get("/api/v1/balances/{userId}", TEST_USER)
        .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("currency", containsInAnyOrder("USD", "EUR"));
    }

    @Test
    void shouldGetSpecificBalance() {
        depositBalance(TEST_USER, "GBP", 350.00);

        given()
        .when()
                .get("/api/v1/balances/{userId}/{currency}", TEST_USER, "GBP")
        .then()
                .statusCode(200)
                .body("userId", equalTo(TEST_USER))
                .body("currency", equalTo("GBP"))
                .body("balance", comparesEqualTo(350.00f));
    }

    @Test
    void shouldReturn404WhenBalanceNotFound() {
        given()
        .when()
                .get("/api/v1/balances/{userId}/{currency}", "non-existent-user", "USD")
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        given()
        .when()
                .get("/api/v1/balances/{userId}", "user-with-no-balances")
        .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void shouldReturn400OnInvalidCurrencyCode() {
        given()
        .when()
                .get("/api/v1/balances/{userId}/{currency}", TEST_USER, "INVALID")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnNegativeDepositAmount() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "%s",
                          "currency": "USD",
                          "amount": -10.00
                        }
                        """.formatted(TEST_USER))
        .when()
                .post("/api/v1/balances/deposit")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400WhenUserIdIsBlank() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "userId": "",
                          "currency": "USD",
                          "amount": 100.00
                        }
                        """)
        .when()
                .post("/api/v1/balances/deposit")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue());
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
