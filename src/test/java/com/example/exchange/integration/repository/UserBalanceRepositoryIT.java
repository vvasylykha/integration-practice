package com.example.exchange.integration.repository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Integration tests for {@link com.example.exchange.repository.UserBalanceRepository},
 * covering query methods, pessimistic/optimistic locking, and existence checks
 * against a real PostgreSQL database.
 *
 * <p><b>Task:</b> annotate this class to configure a JPA test slice:
 * <ul>
 *   <li>Use the Spring Boot slice annotation that loads only JPA components
 *       (no full application context).</li>
 *   <li>Disable auto-replacement of the datasource with an in-memory database
 *       so tests run against real PostgreSQL (required for pessimistic locking).</li>
 *   <li>Activate the {@code "test"} profile.</li>
 *   <li>Import {@code TestContainersConfig}.</li>
 * </ul>
 *
 * <p>Also declare and inject the repository field. Each test runs in a transaction
 * that is rolled back automatically — no manual cleanup needed.
 */
class UserBalanceRepositoryIT {

    /**
     * Finding a balance by userId and currency should return a present {@link Optional}
     * with the matching {@code id} and {@code balance} amount.
     */
    @Test
    void shouldFindBalanceByUserIdAndCurrency() {
        // TODO: implement
    }

    /**
     * Finding a balance for a userId and currency that was never saved
     * should return an empty {@link Optional} without throwing an exception.
     */
    @Test
    void shouldReturnEmptyWhenBalanceNotFound() {
        // TODO: implement
    }

    /**
     * Finding all balances for a user who has two currencies should return exactly
     * those two entries and must not include records belonging to other users.
     */
    @Test
    void shouldFindAllBalancesForUser() {
        // TODO: implement
    }

    /**
     * Finding all balances for a user who has no records should return an empty list.
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        // TODO: implement
    }

    /**
     * Querying with a pessimistic write lock ({@code SELECT ... FOR UPDATE})
     * should return the existing balance entity with the correct amount.
     */
    @Test
    void shouldAcquirePessimisticLockForUpdate() {
        // TODO: implement
    }

    /**
     * Querying with a pessimistic write lock for a record that does not exist
     * should return an empty {@link Optional} without throwing an exception.
     */
    @Test
    void shouldReturnEmptyWhenLockingNonExistentBalance() {
        // TODO: implement
    }

    /**
     * After saving a new entity its {@code version} should be {@code 0};
     * after modifying and flushing it the {@code version} should be incremented to {@code 1}.
     * Verifies that JPA optimistic locking ({@code @Version}) works correctly.
     */
    @Test
    void shouldSaveAndIncrementVersionOnUpdate() {
        // TODO: implement
    }

    /**
     * Existence check for a currency that was saved should return {@code true};
     * existence check for a currency that was never saved for that user should return {@code false}.
     */
    @Test
    void shouldCheckExistenceByUserIdAndCurrency() {
        // TODO: implement
    }
}
