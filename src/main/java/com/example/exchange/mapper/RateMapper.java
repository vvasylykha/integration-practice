package com.example.exchange.mapper;

import com.example.exchange.dto.response.RateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RateMapper {


    default RateResponse toResponse(String baseCurrency, String targetCurrency,
                                    BigDecimal rate,
                                    LocalDateTime timestamp) {
        return RateResponse.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(rate)
                .timestamp(timestamp)
                .build();
    }
}
