package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class BalanceControllerIT extends ApiIntegrationTest {

    @Autowired
    UserBalanceRepository userBalanceRepository;

    // TODO (BP1): there is no cleanup. Records from one test leak into the next,
    //             so the suite passes once and fails on re-run / random order.
    //             Add an @AfterEach that removes the data this test created.

    @Test
    void shouldDepositAndReturn201() {
        // ❌ BP1: hard-coded shared user. Re-running accumulates balance and breaks the assertion.
        given().contentType(JSON).body(depositBody("user123", "USD", "1000.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201)
                .body("balance", equalTo(1000.00f)); // TODO (BP1): assert on data this test created, not an absolute value
    }

    @Test
    void shouldGetAllBalancesForUser() {
        deposit("user123", "USD", "100.00");
        deposit("user123", "EUR", "200.00");
        // ❌ BP1: same shared "user123" as other tests → size depends on execution order
        given().when().get("/api/v1/balances/user123")
                .then().statusCode(200)
                .body("$", hasSize(2));  // TODO (BP1): use a unique userId per test so the count is deterministic
    }

    // TODO (BP1): implement remaining methods from the README list, each with its OWN unique data:
    //   shouldGetSpecificBalance, shouldReturn404WhenBalanceNotFound,
    //   shouldReturnEmptyListWhenUserHasNoBalances, shouldReturn400OnInvalidCurrencyCode,
    //   shouldReturn400OnNegativeDepositAmount, shouldReturn400WhenUserIdIsBlank

    private void deposit(String userId, String currency, String amount) {
        given().contentType(JSON).body(depositBody(userId, currency, amount))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201);
    }

    // builds the request body (for the explicit deposit test)
    private Map<String, Object> depositBody(String userId, String currency, String amount) {
        return Map.of("userId", userId, "currency", currency, "amount", amount);
    }

}
