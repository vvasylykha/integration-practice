package com.example.exchange.integration.controller;

import com.example.exchange.integration.base.ApiIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Balance REST API ({@code /api/v1/balances}).
 *
 * <p><b>Task:</b> declare the required fields, implement the {@code @AfterEach} cleanup,
 * and implement each test method using the RestAssured Given-When-Then DSL.
 */
class BalanceControllerIT extends ApiIntegrationTest {


    /**
     * Delete all balance records created during the test to prevent data leaking
     * into subsequent tests.
     */
    @AfterEach
    void cleanUp() {
        // TODO: implement
    }

    /**
     * A valid deposit request should return HTTP 201 Created with
     * {@code userId}, {@code currency}, and {@code balance} in the response body.
     *
     * <p>Endpoint: {@code POST /api/v1/balances/deposit}
     */
    @Test
    void shouldDepositAndReturn201() {
        // TODO: implement
    }

    /**
     * A user with two deposits in different currencies should get HTTP 200 OK
     * with a list containing exactly those two balance entries.
     *
     * <p>Endpoint: {@code GET /api/v1/balances/{userId}}
     */
    @Test
    void shouldGetAllBalancesForUser() {
        // TODO: implement
    }

    /**
     * Requesting the balance for a specific user and currency that was deposited
     * should return HTTP 200 OK with matching {@code userId}, {@code currency},
     * and {@code balance} in the response body.
     *
     * <p>Endpoint: {@code GET /api/v1/balances/{userId}/{currency}}
     */
    @Test
    void shouldGetSpecificBalance() {
        // TODO: implement
    }

    /**
     * Requesting the balance for a user who has no records should return
     * HTTP 404 Not Found with {@code status}, {@code message}, and {@code timestamp}
     * in the error body.
     *
     * <p>Endpoint: {@code GET /api/v1/balances/{userId}/{currency}}
     */
    @Test
    void shouldReturn404WhenBalanceNotFound() {
        // TODO: implement
    }

    /**
     * Requesting all balances for a user who has never deposited should return
     * HTTP 200 OK with an empty JSON array.
     *
     * <p>Endpoint: {@code GET /api/v1/balances/{userId}}
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        // TODO: implement
    }

    /**
     * A request with an invalid (non 3-letter) currency code should return
     * HTTP 400 Bad Request with {@code status}, {@code errors}, and {@code timestamp}
     * in the error body.
     *
     * <p>Endpoint: {@code GET /api/v1/balances/{userId}/{currency}}
     */
    @Test
    void shouldReturn400OnInvalidCurrencyCode() {
        // TODO: implement
    }

    /**
     * A deposit request with a negative amount should be rejected with
     * HTTP 400 Bad Request and a validation error body containing
     * {@code status}, {@code errors}, and {@code timestamp}.
     *
     * <p>Endpoint: {@code POST /api/v1/balances/deposit}
     */
    @Test
    void shouldReturn400OnNegativeDepositAmount() {
        // TODO: implement
    }

    /**
     * A deposit request with a blank {@code userId} should be rejected with
     * HTTP 400 Bad Request and a validation error body containing
     * {@code status} and {@code errors}.
     *
     * <p>Endpoint: {@code POST /api/v1/balances/deposit}
     */
    @Test
    void shouldReturn400WhenUserIdIsBlank() {
        // TODO: implement
    }
}
