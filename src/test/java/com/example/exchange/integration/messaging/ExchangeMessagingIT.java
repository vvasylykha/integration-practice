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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ExchangeMessagingIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * After each test: delete all audit log records to prevent data leaking
     * into subsequent tests.
     */
    @AfterEach
    void cleanUp() {
        // TODO: implement
    }

    /**
     * Publishing an {@link ExchangeCompletedEvent} to the audit exchange should cause
     * the message listener to persist an audit log entry with event type {@code "COMPLETED"},
     * the correct {@code userId}, and a non-empty {@code details} field.
     */
    @Test
    void shouldCreateAuditLogWhenExchangeCompletedEventReceived() {
        // TODO: implement
    }

    /**
     * Publishing an {@link ExchangeFailedEvent} to the audit exchange should cause
     * the message listener to persist an audit log entry with event type {@code "FAILED"}
     * and the failure reason in the {@code details} field.
     */
    @Test
    void shouldCreateAuditLogWhenExchangeFailedEventReceived() {
        // TODO: implement
    }

    /**
     * Publishing the same {@link ExchangeCompletedEvent} twice should result in exactly
     * one audit log record — the consumer must be idempotent (at-least-once delivery).
     * Use Awaitility to verify the count stays at 1 throughout an observation window.
     */
    @Test
    void shouldBeIdempotentWhenDuplicateCompletedEventReceived() {
        // TODO: implement
    }

    /**
     * Publishing the same {@link ExchangeFailedEvent} twice should result in exactly
     * one audit log record — the consumer must be idempotent.
     * Use Awaitility to verify the count stays at 1 throughout an observation window.
     */
    @Test
    void shouldBeIdempotentWhenDuplicateFailedEventReceived() {
        // TODO: implement
    }
}
