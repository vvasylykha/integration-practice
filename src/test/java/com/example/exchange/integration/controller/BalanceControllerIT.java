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

/**
 * Integration tests for the Balance API (/api/v1/balances).
 *
 * Component diagram: diagrams/png/BalanceController Component Architecture.png
 *   (BalanceController -> BalanceService -> UserBalanceRepository -> user_balances)
 */
class BalanceControllerIT extends ApiIntegrationTest {

    @Autowired
    UserBalanceRepository userBalanceRepository;

    // TODO (REQUIRED):
    //  BP1 — Test Isolation & Independence
    //  There is no cleanup. Records from one test leak into the next,
    //  so the suite passes once and fails on re-run / random order.
    //  Add an @AfterEach that removes the data this test created.

    @Test
    void shouldDepositAndReturn201() {
        // WHAT IT TESTS: happy path POST /api/v1/balances/deposit → 201 and a BalanceResponse
        //   whose balance equals the amount THIS test deposited (use a unique userId per test).
        //
        // ❌ BP1: hard-coded shared user. Re-running accumulates balance and breaks the assertion.
        given().contentType(JSON).body(depositBody("user123", "USD", "1000.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201)
                .body("balance", equalTo(1000.00f)); // assert on data this test created, not an absolute value
    }

    @Test
    void shouldGetAllBalancesForUser() {
        // WHAT IT TESTS: GET /api/v1/balances/{userId} → 200 with all balances of that user.
        //   Deposit two currencies for a UNIQUE userId, expect exactly 2 entries (count must be
        //   deterministic regardless of execution order — BP1).
        deposit("user123", "USD", "100.00");
        deposit("user123", "EUR", "200.00");
        // ❌ BP1: same shared "user123" as other tests → size depends on execution order
        given().when().get("/api/v1/balances/user123")
                .then().statusCode(200)
                .body("$", hasSize(2));  // use a unique userId per test so the count is deterministic
    }

    // TODO (REQUIRED):
    //   Test GET /api/v1/balances/{userId}/{currency} → 200 with the BalanceResponse for that
    //   single currency, matching the amount this test deposited (unique userId, BP1).

    // TODO (REQUIRED):
    //   Test GET /api/v1/balances/{userId}/{currency} for a user/currency that was never created
    //   → 404. Use a unique, guaranteed-absent userId (BP1).

    // TODO (OPTIONAL):
    //   Test GET /api/v1/balances/{userId} for a user with no balances → 200 and an empty array
    //   (not 404).

    // TODO (OPTIONAL):
    //   Test POST deposit with a non 3-letter currency code → 400 with an error body.

    // TODO (OPTIONAL):
    //   Test POST deposit with a negative (or zero) amount → 400.

    // TODO (OPTIONAL):
    //   Test POST deposit with a blank userId → 400.

    private void deposit(String userId, String currency, String amount) {
        given().contentType(JSON).body(depositBody(userId, currency, amount))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201);
    }

    private Map<String, Object> depositBody(String userId, String currency, String amount) {
        return Map.of("userId", userId, "currency", currency, "amount", amount);
    }

}
