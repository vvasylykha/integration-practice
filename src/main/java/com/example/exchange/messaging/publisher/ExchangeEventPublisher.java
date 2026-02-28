package com.example.exchange.messaging.publisher;

import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.event.ExchangeFailedEvent;

public interface ExchangeEventPublisher {

    void publishCompletedEvent(ExchangeCompletedEvent event);

    void publishFailedEvent(ExchangeFailedEvent event);
}
