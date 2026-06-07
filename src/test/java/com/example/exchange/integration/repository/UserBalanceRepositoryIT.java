package com.example.exchange.integration.repository;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.model.UserBalance;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserBalanceRepository.
 *
 * UNLIKE the other practice classes, this one is GREEN at the start. The symptom is not a
 * failing assertion — it is SPEED. Watch the run: the full application context (web, AMQP,
 * Redis) is started, and @DirtiesContext throws it away after every method, so the context is
 * rebuilt from scratch for each test. With 8 tests that is painfully slow and the log is full
 * of repeated "Started ... in N seconds".
 *
 * TODO (BP4 — Execution Speed & Context Optimization):
 *   A repository test does not need the whole application. Replace @SpringBootTest with the LEAN
 *   slice that loads ONLY JPA beans, and remove @DirtiesContext (the slice rolls back per test
 *   automatically, so there is nothing dirty to clean up). The shared context will then be
 *   cached and reused instead of rebuilt.
 *
 * TODO (BP2 — Infrastructure Realism):
 *   The moment you switch to the JPA slice it will auto-replace the DataSource with the in-memory
 *   H2 that is on the test classpath. Your real Flyway migrations then run against H2 — and the
 *   very first one (BIGSERIAL) is PostgreSQL-only, so the context fails to start. That is the
 *   point: H2 is NOT the database you ship. Disable the auto-replacement so the slice keeps using
 *   the real Testcontainers PostgreSQL, and you are back to testing your actual schema, types,
 *   SELECT ... FOR UPDATE and @Version semantics.
 */
@SpringBootTest                                                   // ❌ BP4: loads web/AMQP/Redis just for DB queries
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // ❌ BP4: destroys the context cache → rebuilt every test
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
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
        repository.saveAndFlush(balance("u-other", "USD", "999.00"));   // must NOT be returned

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

        // On real PostgreSQL this issues SELECT ... FOR NO KEY UPDATE (a row-level write lock).
        Optional<UserBalance> locked = repository.findByUserIdAndCurrencyForUpdate("u-lock", "USD");

        assertThat(locked).isPresent();
        assertThat(locked.get().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnEmptyWhenLockingNonExistentBalance() {
        // Locking a row that does not exist returns an empty Optional, not an exception.
        assertThat(repository.findByUserIdAndCurrencyForUpdate("u-nolock", "USD")).isEmpty();
    }

    @Test
    void shouldSaveAndIncrementVersionOnUpdate() {
        UserBalance saved = repository.saveAndFlush(balance("u-version", "USD", "10.00"));
        assertThat(saved.getVersion()).isZero();   // @Version starts at 0

        saved.setBalance(new BigDecimal("20.00"));
        UserBalance updated = repository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);   // optimistic-lock version bumped on update
    }

    @Test
    void shouldCheckExistenceByUserIdAndCurrency() {
        repository.saveAndFlush(balance("u-exists", "USD", "100.00"));

        assertThat(repository.existsByUserIdAndCurrency("u-exists", "USD")).isTrue();
        assertThat(repository.existsByUserIdAndCurrency("u-exists", "EUR")).isFalse();
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