package com.example.exchange.integration.messaging;

import com.example.exchange.integration.config.TestContainersConfig;
import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.event.ExchangeFailedEvent;
import com.example.exchange.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static com.example.exchange.config.RabbitMQConfig.AUDIT_COMPLETED_ROUTING_KEY;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_EXCHANGE;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_FAILED_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Messaging integration tests: a published exchange event is consumed and persisted to audit_log,
 * and the consumer is idempotent under at-least-once delivery.
 *
 * Component diagram (Messaging slice — RabbitMQ -> ExchangeEventListener -> AuditService ->
 *   AuditLogRepository -> audit_log): diagrams/png/ExchangeController Component Architecture.png
 *
 * BP1 — each test cleans audit_log in @AfterEach, so a fixed exchangeId can be reused safely and
 * the UNIQUE (exchange_id, event_type) constraint never collides across runs. Because consumption
 * is asynchronous, every assertion goes through Awaitility: we wait for the expected state rather
 * than reading the DB before the listener thread has run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ExchangeMessagingIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HOLD = Duration.ofSeconds(2);

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    AuditLogRepository auditLogRepository;

    @AfterEach
    void cleanUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void shouldCreateAuditLogWhenExchangeCompletedEventReceived() {
        long exchangeId = 5001L;
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY,
                completedEvent(exchangeId, "user123"));

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var log = auditLogRepository.findByExchangeIdAndEventType(exchangeId, "COMPLETED");
            assertThat(log).isPresent();
            assertThat(log.get().getUserId()).isEqualTo("user123");
            assertThat(log.get().getDetails()).isNotBlank();
        });
    }

    @Test
    void shouldCreateAuditLogWhenExchangeFailedEventReceived() {
        long exchangeId = 5101L;
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY,
                failedEvent(exchangeId, "user123", "Rate provider unavailable"));

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var log = auditLogRepository.findByExchangeIdAndEventType(exchangeId, "FAILED");
            assertThat(log).isPresent();
            assertThat(log.get().getDetails()).contains("Rate provider unavailable");
        });
    }

    @Test
    void shouldBeIdempotentWhenDuplicateCompletedEventReceived() {
        long exchangeId = 5002L;
        var event = completedEvent(exchangeId, "user123");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);

        // First wait until at least one row is persisted, then assert the count STAYS at 1
        // throughout the hold window — proving the duplicate did not create a second row.
        await().atMost(TIMEOUT)
                .until(() -> auditLogRepository.findByExchangeId(exchangeId).size() == 1);
        await().during(HOLD).atMost(TIMEOUT).untilAsserted(() ->
                assertThat(auditLogRepository.findByExchangeId(exchangeId)).hasSize(1));
    }

    @Test
    void shouldBeIdempotentWhenDuplicateFailedEventReceived() {
        long exchangeId = 5102L;
        var event = failedEvent(exchangeId, "user123", "Timeout");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);

        await().atMost(TIMEOUT)
                .until(() -> auditLogRepository.findByExchangeId(exchangeId).size() == 1);
        await().during(HOLD).atMost(TIMEOUT).untilAsserted(() ->
                assertThat(auditLogRepository.findByExchangeId(exchangeId)).hasSize(1));
    }

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

    private ExchangeFailedEvent failedEvent(long exchangeId, String userId, String reason) {
        return ExchangeFailedEvent.builder()
                .exchangeId(exchangeId)
                .userId(userId)
                .fromCurrency("USD")
                .toCurrency("EUR")
                .errorMessage(reason)
                .timestamp(LocalDateTime.now())
                .build();
    }
}