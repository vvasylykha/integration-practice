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
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.example.exchange.config.RabbitMQConfig.AUDIT_COMPLETED_ROUTING_KEY;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_EXCHANGE;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_FAILED_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ExchangeMessagingIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @AfterEach
    void cleanUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void shouldCreateAuditLogWhenExchangeCompletedEventReceived() {
        ExchangeCompletedEvent event = completedEvent(1001L, "msg-user-1");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var log = auditLogRepository.findByExchangeIdAndEventType(1001L, "COMPLETED");
                    assertThat(log).isPresent();
                    assertThat(log.get().getUserId()).isEqualTo("msg-user-1");
                    assertThat(log.get().getDetails()).contains("Exchange completed:");
                });
    }

    @Test
    void shouldCreateAuditLogWhenExchangeFailedEventReceived() {
        ExchangeFailedEvent event = failedEvent(1002L, "msg-user-2", "Insufficient funds");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var log = auditLogRepository.findByExchangeIdAndEventType(1002L, "FAILED");
                    assertThat(log).isPresent();
                    assertThat(log.get().getDetails()).contains("Insufficient funds");
                });
    }

    @Test
    void shouldBeIdempotentWhenDuplicateCompletedEventReceived() {
        ExchangeCompletedEvent event = completedEvent(1003L, "msg-user-3");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> auditLogRepository.findByExchangeIdAndEventType(1003L, "COMPLETED").isPresent());

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);

        await()
                .during(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long count = auditLogRepository.findByExchangeId(1003L).size();
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    void shouldBeIdempotentWhenDuplicateFailedEventReceived() {
        ExchangeFailedEvent event = failedEvent(1004L, "msg-user-4", "Timeout");

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> auditLogRepository.findByExchangeIdAndEventType(1004L, "FAILED").isPresent());

        rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);

        await()
                .during(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        assertThat(auditLogRepository.findByExchangeId(1004L)).hasSize(1)
                );
    }

    private ExchangeCompletedEvent completedEvent(Long exchangeId, String userId) {
        return ExchangeCompletedEvent.builder()
                .exchangeId(exchangeId)
                .userId(userId)
                .fromCurrency("USD")
                .toCurrency("EUR")
                .amount(new BigDecimal("100.00"))
                .convertedAmount(new BigDecimal("85.00"))
                .exchangeRate(new BigDecimal("0.850000"))
                .commission(new BigDecimal("0.5000"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ExchangeFailedEvent failedEvent(Long exchangeId, String userId, String errorMessage) {
        return ExchangeFailedEvent.builder()
                .exchangeId(exchangeId)
                .userId(userId)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
