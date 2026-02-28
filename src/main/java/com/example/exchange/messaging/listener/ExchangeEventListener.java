package com.example.exchange.messaging.listener;

import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.event.ExchangeFailedEvent;
import com.example.exchange.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_COMPLETED_QUEUE;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_DLQ;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_FAILED_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeEventListener {

    private final AuditService auditService;

    @RabbitListener(queues = AUDIT_COMPLETED_QUEUE)
    public void handleExchangeCompletedEvent(ExchangeCompletedEvent event) {
        log.info("Received exchange completed event for exchange ID: {}", event.getExchangeId());
        String details = String.format("Exchange completed: from=%s, to=%s, amount=%s, converted=%s, rate=%s, commission=%s",
                event.getFromCurrency(), event.getToCurrency(), event.getAmount(),
                event.getConvertedAmount(), event.getExchangeRate(), event.getCommission());
        processAuditEvent(event.getExchangeId(), "COMPLETED", event.getUserId(),
                details, event.getIpAddress(), event.getUserAgent());
    }

    @RabbitListener(queues = AUDIT_FAILED_QUEUE)
    public void handleExchangeFailedEvent(ExchangeFailedEvent event) {
        log.info("Received exchange failed event for exchange ID: {}", event.getExchangeId());
        String details = String.format("Exchange failed: reason=%s",
                event.getErrorMessage() != null ? event.getErrorMessage() : "Unknown error");
        processAuditEvent(event.getExchangeId(), "FAILED", event.getUserId(),
                details, event.getIpAddress(), event.getUserAgent());
    }

    @RabbitListener(queues = AUDIT_DLQ)
    public void handleDeadLetterEvent(Object message) {
        log.error("Dead-lettered audit message received, manual intervention required: {}", message);
    }

    private void processAuditEvent(Long exchangeId, String eventType, String userId,
                                    String details, String ipAddress, String userAgent) {
        if (auditService.isEventAlreadyProcessed(exchangeId, eventType)) {
            log.warn("Event already processed for exchange {}, skipping", exchangeId);
            return;
        }
        auditService.logExchangeEvent(exchangeId, eventType, userId, details, ipAddress, userAgent);
        log.info("Successfully processed {} audit event for exchange {}", eventType, exchangeId);
    }
}
