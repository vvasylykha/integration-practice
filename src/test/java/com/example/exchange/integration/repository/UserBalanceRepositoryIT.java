package com.example.exchange.integration.repository;

import com.example.exchange.model.UserBalance;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserBalanceRepository — PRACTICE: BP2 (Infrastructure Realism).
 *
 * Component diagram (persistence slice — UserBalanceRepository -> user_balances):
 *   diagrams/png/BalanceController Component Architecture.png
 *
 * This is a lean @DataJpaTest slice that runs against an in-memory H2 (configured below). It looks
 * convincing — 8 of the 9 tests are GREEN. That is exactly the danger: H2 is NOT the database you
 * ship, so a green H2 run gives FALSE CONFIDENCE.
 *
 * The 9th test, shouldUpsertBalanceUsingPostgresOnConflict, FAILS here: upsertBalance() uses
 * PostgreSQL's INSERT ... ON CONFLICT ... DO UPDATE, which simply does not exist on H2. A core
 * query that works in production is untestable on H2 — and even the 8 "green" tests never exercise
 * the real PostgreSQL FOR (NO KEY) UPDATE lock, BIGSERIAL or @Version behaviour.
 *
 * (The H2 URL sets DATABASE_TO_LOWER/CASE_INSENSITIVE_IDENTIFIERS only so the real Flyway schema
 *  even loads — by default H2's identifier casing leaves Hibernate unable to find the tables. Those
 *  flags still do NOT add ON CONFLICT, which is the whole point.)
 *
 *  TODO (REQUIRED):
 *   BP2 — Infrastructure Realism
 *   Stop testing against H2. Use the real database you ship by removing the H2 @TestPropertySource
 *   and pointing the slice at Testcontainers PostgreSQL:
 *     @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
 *     @Import(TestContainersConfig.class)
 *   Then ON CONFLICT works, all 9 tests pass, and you are genuinely testing your production schema,
 *   types, locks and optimistic-locking semantics.
 *
 */

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        // ❌ BP2: an in-memory H2 standing in for production PostgreSQL
        "spring.datasource.url=jdbc:h2:mem:bp2;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class UserBalanceRepositoryIT {

    @Autowired
    UserBalanceRepository repository;

    @Test
    void shouldFindBalanceByUserIdAndCurrency() {
        UserBalance saved = repository.saveAndFlush(balance("u-find", "USD", "100.00"));

        Optional<UserBalance> found = repository.findByUserIdAndCurrency("u-find", "USD");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnEmptyWhenBalanceNotFound() {
        assertThat(repository.findByUserIdAndCurrency("u-missing", "USD")).isEmpty();
    }

    @Test
    void shouldFindAllBalancesForUser() {
        repository.saveAndFlush(balance("u-all", "USD", "100.00"));
        repository.saveAndFlush(balance("u-all", "EUR", "200.00"));
        repository.saveAndFlush(balance("u-other", "USD", "999.00"));

        List<UserBalance> balances = repository.findByUserId("u-all");

        assertThat(balances).hasSize(2);
        assertThat(balances).extracting(UserBalance::getCurrency)
                .containsExactlyInAnyOrder("USD", "EUR");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        assertThat(repository.findByUserId("u-empty")).isEmpty();
    }

    @Test
    void shouldAcquirePessimisticLockForUpdate() {
        repository.saveAndFlush(balance("u-lock", "USD", "100.00"));
        Optional<UserBalance> locked = repository.findByUserIdAndCurrencyForUpdate("u-lock", "USD");

        assertThat(locked).isPresent();
        assertThat(locked.get().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnEmptyWhenLockingNonExistentBalance() {
        assertThat(repository.findByUserIdAndCurrencyForUpdate("u-nolock", "USD")).isEmpty();
    }

    @Test
    void shouldSaveAndIncrementVersionOnUpdate() {
        UserBalance saved = repository.saveAndFlush(balance("u-version", "USD", "10.00"));
        assertThat(saved.getVersion()).isZero();

        saved.setBalance(new BigDecimal("20.00"));
        UserBalance updated = repository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldCheckExistenceByUserIdAndCurrency() {
        repository.saveAndFlush(balance("u-exists", "USD", "100.00"));

        assertThat(repository.existsByUserIdAndCurrency("u-exists", "USD")).isTrue();
        assertThat(repository.existsByUserIdAndCurrency("u-exists", "EUR")).isFalse();
    }

    @Test
    void shouldUpsertBalanceUsingPostgresOnConflict() {
        // ❌ BP2: upsertBalance() is PostgreSQL INSERT ... ON CONFLICT ... DO UPDATE. On H2 this
        //         throws a syntax error (no such clause), so the test fails — even though the
        //         feature works perfectly on the production database. Run on Testcontainers
        //         PostgreSQL (the BP2 fix) and it goes green: first call inserts, second hits the
        //         conflict and adds to the existing balance.
        repository.upsertBalance("u-upsert", "USD", new BigDecimal("100.00"));
        repository.upsertBalance("u-upsert", "USD", new BigDecimal("25.00"));

        Optional<UserBalance> found = repository.findByUserIdAndCurrency("u-upsert", "USD");
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("125.00");
    }

    private UserBalance balance(String userId, String currency, String amount) {
        return UserBalance.builder()
                .userId(userId)
                .currency(currency)
                .balance(new BigDecimal(amount))
                .lockedBalance(BigDecimal.ZERO)
                .build();
    }
}