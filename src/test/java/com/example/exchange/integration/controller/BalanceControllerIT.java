package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for the Balance API (/api/v1/balances).
 *
 * BP1 (Test Isolation) — every test creates its own user with a unique id, so counts and amounts
 * are deterministic regardless of execution order, and @AfterEach removes all balances so nothing
 * leaks between tests or across re-runs. @Transactional is intentionally NOT used: these tests
 * cross a real HTTP boundary, so the server commits in its own transaction and explicit cleanup
 * is required.
 */
class BalanceControllerIT extends ApiIntegrationTest {

    @Autowired
    UserBalanceRepository userBalanceRepository;

    @AfterEach
    void cleanUp() {
        userBalanceRepository.deleteAll();
    }

    @Test
    void shouldDepositAndReturn201() {
        String userId = uniqueUser();

        given().contentType(JSON).body(depositBody(userId, "USD", "1000.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(201)
                .body("userId", equalTo(userId))
                .body("currency", equalTo("USD"))
                .body("balance", comparesEqualTo(1000.00f));
    }

    @Test
    void shouldGetAllBalancesForUser() {
        String userId = uniqueUser();
        deposit(userId, "USD", "100.00");
        deposit(userId, "EUR", "200.00");

        // Unique user => exactly the two balances this test created, never another test's data.
        given().when().get("/api/v1/balances/" + userId)
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("currency", hasItems("USD", "EUR"));
    }

    @Test
    void shouldGetSpecificBalance() {
        String userId = uniqueUser();
        deposit(userId, "USD", "100.00");

        given().when().get("/api/v1/balances/" + userId + "/USD")
                .then().statusCode(200)
                .body("userId", equalTo(userId))
                .body("currency", equalTo("USD"))
                .body("balance", comparesEqualTo(100.00f));
    }

    @Test
    void shouldReturn404WhenBalanceNotFound() {
        // Fresh unique user with no deposits -> the specific-balance lookup is 404.
        given().when().get("/api/v1/balances/" + uniqueUser() + "/USD")
                .then().statusCode(404)
                .body("status", equalTo(404))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        // Listing balances for a user with no records is 200 with an empty array (not 404).
        given().when().get("/api/v1/balances/" + uniqueUser())
                .then().statusCode(200)
                .body("$", empty());
    }

    @Test
    void shouldReturn400OnInvalidCurrencyCode() {
        // 2-letter code fails @ValidCurrencyCode on the path variable -> ConstraintViolation -> 400.
        given().when().get("/api/v1/balances/" + uniqueUser() + "/XX")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400OnNegativeDepositAmount() {
        // @DecimalMin on the request body rejects a non-positive amount.
        given().contentType(JSON).body(depositBody(uniqueUser(), "USD", "-50.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void shouldReturn400WhenUserIdIsBlank() {
        // @NotBlank on userId -> MethodArgumentNotValidException -> 400 with errors map.
        given().contentType(JSON).body(depositBody("", "USD", "100.00"))
                .when().post("/api/v1/balances/deposit")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("errors", notNullValue());
    }

    private String uniqueUser() {
        return "user-" + UUID.randomUUID();
    }

    // performs the deposit (setup action)
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