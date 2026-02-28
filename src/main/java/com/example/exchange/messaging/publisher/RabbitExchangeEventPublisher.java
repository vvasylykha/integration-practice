package com.example.exchange.messaging.publisher;

import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.event.ExchangeFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.exchange.config.RabbitMQConfig.AUDIT_COMPLETED_ROUTING_KEY;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_EXCHANGE;
import static com.example.exchange.config.RabbitMQConfig.AUDIT_FAILED_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitExchangeEventPublisher implements ExchangeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishCompletedEvent(ExchangeCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_COMPLETED_ROUTING_KEY, event);
            log.debug("Published exchange completed event for exchange ID {}", event.getExchangeId());
        } catch (AmqpException e) {
            log.error("Failed to publish completed event for exchange ID {}: {}", event.getExchangeId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void publishFailedEvent(ExchangeFailedEvent event) {
        try {
            rabbitTemplate.convertAndSend(AUDIT_EXCHANGE, AUDIT_FAILED_ROUTING_KEY, event);
            log.debug("Published exchange failed event for exchange ID {}", event.getExchangeId());
        } catch (AmqpException e) {
            log.error("Failed to publish failed event for exchange ID {}: {}", event.getExchangeId(), e.getMessage(), e);
            throw e;
        }
    }
}
