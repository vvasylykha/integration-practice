package com.example.exchange.integration.messaging;

import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.event.ExchangeFailedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for RabbitMQ message consumers that persist audit log records.
 *
 * <p><b>Task:</b> annotate this class to configure a Spring Boot integration test
 * for messaging:
 * <ul>
 *   <li>Start the Spring context without a web server (no HTTP endpoints needed).</li>
 *   <li>Activate the {@code "test"} profile.</li>
 *   <li>Import {@code TestContainersConfig} — this class does not extend
 *       {@code ApiIntegrationTest}, so the import must be declared explicitly.</li>
 * </ul>
 *
 * <p>Also inject {@code RabbitTemplate} and {@code AuditLogRepository} fields.
 */
class ExchangeMessagingIT {

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
