package com.example.exchange.integration.repository;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.model.UserBalance;
import com.example.exchange.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserBalanceRepository.
 *
 * BP4 — @DataJpaTest loads ONLY the JPA layer (no web/AMQP/Redis), so it is fast and its context
 * is shared/cached across repository test classes. No @DirtiesContext: the slice wraps each test
 * in a transaction and rolls it back, so tests stay isolated with zero manual cleanup.
 *
 * BP2 — @AutoConfigureTestDatabase(replace = NONE) keeps the real Testcontainers PostgreSQL
 * instead of falling back to in-memory H2. The real Flyway migrations, BIGSERIAL/DECIMAL types,
 * SELECT ... FOR UPDATE and @Version are all exercised against the database we actually ship.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

    private UserBalance balance(String userId, String currency, String amount) {
        return UserBalance.builder()
                .userId(userId)
                .currency(currency)
                .balance(new BigDecimal(amount))
                .lockedBalance(BigDecimal.ZERO)
                .build();
    }
}