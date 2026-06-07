package com.example.exchange.integration.messaging;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.example.exchange.config.RabbitMQConfig.AUDIT_COMPLETED_ROUTING_KEY;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_EXCHANGE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging integration tests: a published exchange event must be consumed and persisted to
 * audit_log, and the consumer must be idempotent under at-least-once delivery.
 *
 * This class violates BP1 (Test Isolation) ON PURPOSE, in two event-driven-specific ways:
 *
 * TODO (BP1 — Zero Side Effects / self-contained data):
 *   There is no cleanup. audit_log has a UNIQUE constraint on (exchange_id, event_type), and the
 *   consumer skips events it has already processed. So a row left by one run makes the next run
 *   see a record that "already exists" — the idempotency assertions then pass or fail depending
 *   on history, not on this test. Add an @AfterEach that deletes all audit_log rows.
 *
 * TODO (BP1 — Predictable Results / async timing):
 *   Messaging is asynchronous: convertAndSend() returns immediately, the listener persists the
 *   audit row a moment later on another thread. Asserting right after sending reads the DB BEFORE
 *   the consumer has run, so the test is flaky. Use Awaitility to wait for the expected state
 *   instead of asserting synchronously.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ExchangeMessagingIT {

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    AuditLogRepository auditLogRepository;

    // TODO (BP1): add @AfterEach that deletes all audit_log rows so a fixed exchangeId can be
    //             reused across runs without colliding on the UNIQUE (exchange_id, event_type).

    @Test
    void shouldCreateAuditLogWhenExchangeCompletedEventReceived() {
        long exchangeId = 5001L;
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY,
                completedEvent(exchangeId, "user123"));

        // ❌ BP1: reads the DB immediately, before the listener thread has persisted anything → flaky
        var log = auditLogRepository.findByExchangeIdAndEventType(exchangeId, "COMPLETED");
        assertThat(log).isPresent();
        assertThat(log.get().getUserId()).isEqualTo("user123");
        assertThat(log.get().getDetails()).isNotBlank();
    }

    @Test
    void shouldBeIdempotentWhenDuplicateCompletedEventReceived() {
        long exchangeId = 5002L;
        var event = completedEvent(exchangeId, "user123");

        // ❌ BP1: same fixed exchangeId on every run; with no cleanup the 2nd run starts dirty
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);

        // ❌ BP1: no async wait — may count 0 (too early) or 1; not a real idempotency check
        assertThat(auditLogRepository.findByExchangeId(exchangeId)).hasSize(1);
    }

    // TODO (BP1): implement the remaining methods, each self-contained and using Awaitility:
    //   shouldCreateAuditLogWhenExchangeFailedEventReceived   (routing key audit.failed, type "FAILED")
    //   shouldBeIdempotentWhenDuplicateFailedEventReceived

    private ExchangeCompletedEvent completedEvent(long exchangeId, String userId) {
        return ExchangeCompletedEvent.builder()
                .exchangeId(exchangeId)
                .userId(userId)
                .fromCurrency("USD")
                .toCurrency("EUR")
                .amount(new BigDecimal("100.00"))
                .convertedAmount(new BigDecimal("90.00"))
                .exchangeRate(new BigDecimal("0.90"))
                .commission(new BigDecimal("0.50"))
                .timestamp(LocalDateTime.now())
                .build();
    }
}