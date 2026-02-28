package com.example.exchange.dto.request;

import com.example.exchange.validation.ValidCurrencyCode;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRequest {

    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;

    @NotBlank(message = "From currency is required")
    @ValidCurrencyCode
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    @ValidCurrencyCode
    private String toCurrency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 digits and 4 decimal places")
    private BigDecimal amount;
}
