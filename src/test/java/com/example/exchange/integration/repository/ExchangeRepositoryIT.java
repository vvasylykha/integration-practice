package com.example.exchange.integration.repository;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.model.Exchange;
import com.example.exchange.model.Exchange.ExchangeStatus;
import com.example.exchange.repository.ExchangeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for ExchangeRepository — PRACTICE: BP4 (Execution Speed & Context Optimization).
 *
 * Component diagram (Data Access slice — ExchangeRepository -> exchanges):
 *   diagrams/png/ExchangeController Component Architecture.png
 *
 * UNLIKE the BP2 class, this one is GREEN at the start. The symptom is not a failing assertion —
 * it is SPEED. Watch the run: @SpringBootTest starts the FULL application context (web, AMQP,
 * Redis) just to run a few DB queries, and @DirtiesContext throws that context away after EVERY
 * method, so it is rebuilt from scratch for each test. With these tests the log fills with repeated
 * "Started ... in N seconds" and the suite is painfully slow.
 *
 * TODO (REQUIRED):
 *   BP4 — Execution Speed & Context Optimization
 *   Remove @DirtiesContext. There is nothing "dirty" to clean: each test already uses its OWN
 *   unique userId, and @DirtiesContext does NOT reset the database anyway (the Testcontainers
 *   PostgreSQL is a static, shared container) — all it does is destroy the Spring context cache.
 *   Drop it and the single context is built once and reused across every test → the suite runs in
 *   a fraction of the time.
 *
 */
@SpringBootTest                                                               // ❌ BP4: loads web/AMQP/Redis just for DB queries
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)  // ❌ BP4: destroys the context cache → rebuilt every test
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ExchangeRepositoryIT {

    @Autowired
    ExchangeRepository repository;

    @Test
    void shouldSaveAndFindById() {
        Exchange saved = repository.saveAndFlush(exchange("u-save", ExchangeStatus.COMPLETED));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldSetTimestampsOnPersist() {
        // @PrePersist populates created_at / updated_at.
        Exchange saved = repository.saveAndFlush(exchange("u-time", ExchangeStatus.COMPLETED));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindAllExchangesForUser() {
        repository.saveAndFlush(exchange("u-list", ExchangeStatus.COMPLETED));
        repository.saveAndFlush(exchange("u-list", ExchangeStatus.COMPLETED));
        repository.saveAndFlush(exchange("u-list-other", ExchangeStatus.COMPLETED));

        List<Exchange> found = repository.findByUserId("u-list");

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Exchange::getUserId).containsOnly("u-list");
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoExchanges() {
        assertThat(repository.findByUserId("u-none")).isEmpty();
    }

    @Test
    void shouldPageExchangesForUser() {
        for (int i = 0; i < 3; i++) {
            repository.saveAndFlush(exchange("u-page", ExchangeStatus.COMPLETED));
        }

        Page<Exchange> firstPage = repository.findByUserId("u-page", PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    void shouldFindByStatus() {
        Exchange saved = repository.saveAndFlush(exchange("u-status", ExchangeStatus.FAILED));

        List<Exchange> failed = repository.findByStatus(ExchangeStatus.FAILED);

        assertThat(failed).extracting(Exchange::getId).contains(saved.getId());
        assertThat(failed).allMatch(e -> e.getStatus() == ExchangeStatus.FAILED);
    }

    private Exchange exchange(String userId, ExchangeStatus status) {
        return Exchange.builder()
                .userId(userId)
                .fromCurrency("USD")
                .toCurrency("EUR")
                .amount(new BigDecimal("100.0000"))
                .convertedAmount(new BigDecimal("92.0000"))
                .exchangeRate(new BigDecimal("0.920000"))
                .commission(new BigDecimal("0.5000"))
                .status(status)
                .build();
    }
}
