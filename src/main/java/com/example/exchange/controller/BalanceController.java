package com.example.exchange.controller;

import com.example.exchange.dto.request.DepositRequest;
import com.example.exchange.dto.response.BalanceResponse;
import com.example.exchange.mapper.BalanceMapper;
import com.example.exchange.model.UserBalance;
import com.example.exchange.service.BalanceService;
import com.example.exchange.validation.ValidCurrencyCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balances")
@Validated
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final BalanceMapper balanceMapper;

    @GetMapping("/{userId}")
    public ResponseEntity<List<BalanceResponse>> getUserBalances(@PathVariable String userId) {
        List<UserBalance> balances = balanceService.getUserBalances(userId);
        List<BalanceResponse> responses = balanceMapper.toResponseList(balances);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{userId}/{currency}")
    public ResponseEntity<BalanceResponse> getUserBalance(
            @PathVariable String userId,
            @ValidCurrencyCode @PathVariable String currency) {

        UserBalance balance = balanceService.getBalance(userId, currency);
        BalanceResponse response = balanceMapper.toResponse(balance);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<BalanceResponse> deposit(@Valid @RequestBody DepositRequest request) {
        UserBalance balance = balanceService.deposit(
                request.getUserId(),
                request.getCurrency(),
                request.getAmount());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(balanceMapper.toResponse(balance));
    }
}
