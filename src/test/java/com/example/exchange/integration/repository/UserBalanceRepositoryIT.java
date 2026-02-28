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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class UserBalanceRepositoryIT {

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Test
    void shouldFindBalanceByUserIdAndCurrency() {
        UserBalance saved = userBalanceRepository.save(balance("user-1", "USD", "100.00"));

        Optional<UserBalance> found = userBalanceRepository.findByUserIdAndCurrency("user-1", "USD");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnEmptyWhenBalanceNotFound() {
        Optional<UserBalance> found = userBalanceRepository.findByUserIdAndCurrency("unknown-user", "USD");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllBalancesForUser() {
        userBalanceRepository.save(balance("user-2", "USD", "100.00"));
        userBalanceRepository.save(balance("user-2", "EUR", "200.00"));
        userBalanceRepository.save(balance("user-3", "USD", "50.00"));

        List<UserBalance> balances = userBalanceRepository.findByUserId("user-2");

        assertThat(balances).hasSize(2);
        assertThat(balances).extracting(UserBalance::getCurrency)
                .containsExactlyInAnyOrder("USD", "EUR");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoBalances() {
        List<UserBalance> balances = userBalanceRepository.findByUserId("no-balances-user");

        assertThat(balances).isEmpty();
    }

    @Test
    void shouldAcquirePessimisticLockForUpdate() {
        userBalanceRepository.save(balance("user-4", "GBP", "500.00"));

        Optional<UserBalance> found = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate("user-4", "GBP");

        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldReturnEmptyWhenLockingNonExistentBalance() {
        Optional<UserBalance> found = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate("ghost-user", "USD");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveAndIncrementVersionOnUpdate() {
        UserBalance saved = userBalanceRepository.save(balance("user-5", "EUR", "300.00"));
        assertThat(saved.getVersion()).isZero();

        saved.setBalance(new BigDecimal("350.00"));
        UserBalance updated = userBalanceRepository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldCheckExistenceByUserIdAndCurrency() {
        userBalanceRepository.save(balance("user-6", "JPY", "10000.00"));

        assertThat(userBalanceRepository.existsByUserIdAndCurrency("user-6", "JPY")).isTrue();
        assertThat(userBalanceRepository.existsByUserIdAndCurrency("user-6", "USD")).isFalse();
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
