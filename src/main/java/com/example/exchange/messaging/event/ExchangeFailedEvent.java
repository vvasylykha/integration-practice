package com.example.exchange.messaging.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeFailedEvent {

    private Long exchangeId;
    private String userId;
    private String fromCurrency;
    private String toCurrency;
    private String errorMessage;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
}
