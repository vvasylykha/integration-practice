package com.example.exchange.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeCompletedEvent {

    private Long exchangeId;
    private String userId;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private BigDecimal commission;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
}
