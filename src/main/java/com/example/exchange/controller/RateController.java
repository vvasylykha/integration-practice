package com.example.exchange.controller;

import com.example.exchange.dto.response.RateResponse;
import com.example.exchange.mapper.RateMapper;
import com.example.exchange.service.RateService;
import com.example.exchange.validation.ValidCurrencyCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rates")
@Validated
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;
    private final RateMapper rateMapper;

    @GetMapping("/{base}/{target}")
    public ResponseEntity<RateResponse> getExchangeRate(
            @ValidCurrencyCode @PathVariable String base,
            @ValidCurrencyCode @PathVariable String target) {
        RateResponse response = rateMapper.toResponse(base, target, rateService.getExchangeRate(base, target), LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{base}")
    public ResponseEntity<Map<String, BigDecimal>> getAllRatesForBase(
            @ValidCurrencyCode @PathVariable String base) {
        Map<String, BigDecimal> rates = rateService.getAllRatesForBase(base);
        return ResponseEntity.ok(rates);
    }
}
