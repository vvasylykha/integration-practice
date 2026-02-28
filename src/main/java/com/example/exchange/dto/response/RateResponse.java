package com.example.exchange.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateResponse {

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private LocalDateTime timestamp;
}
