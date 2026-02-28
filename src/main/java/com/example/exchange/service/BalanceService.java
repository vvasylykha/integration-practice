package com.example.exchange.service;

import com.example.exchange.exception.BalanceNotFoundException;
import com.example.exchange.exception.InsufficientBalanceException;
import com.example.exchange.model.UserBalance;
import com.example.exchange.repository.UserBalanceRepository;
import com.example.exchange.util.CurrencyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserBalanceRepository userBalanceRepository;

    public UserBalance getBalance(String userId, String currency) {
        final String normalizedCurrency = CurrencyUtils.normalizeCurrencyCode(currency);
        
        log.debug("Getting balance for user {} in currency {}", userId, normalizedCurrency);
        
        return userBalanceRepository.findByUserIdAndCurrency(userId, normalizedCurrency)
                .orElseThrow(() -> new BalanceNotFoundException("No balance found for user " + userId + " in currency " + normalizedCurrency));
    }

    public List<UserBalance> getUserBalances(String userId) {
        return userBalanceRepository.findByUserId(userId);
    }

    @Transactional
    public UserBalance deposit(String userId, String currency, BigDecimal amount) {
        final String normalizedCurrency = CurrencyUtils.normalizeCurrencyCode(currency);
        
        log.info("Depositing {} {} for user {}", amount, normalizedCurrency, userId);
        
        UserBalance balance = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate(userId, normalizedCurrency)
                .orElse(UserBalance.builder()
                        .userId(userId)
                        .currency(normalizedCurrency)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build());

        balance.setBalance(balance.getBalance().add(amount));

        return userBalanceRepository.save(balance);
    }

    @Transactional
    public void validateSufficientBalance(String userId, String currency, BigDecimal requiredAmount) {
        final String normalizedCurrency = CurrencyUtils.normalizeCurrencyCode(currency);

        UserBalance balance = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate(userId, normalizedCurrency)
                .orElseThrow(() -> new InsufficientBalanceException("No balance found for user " + userId + " in currency " + normalizedCurrency));

        BigDecimal availableBalance = balance.getBalance().subtract(balance.getLockedBalance());

        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }

    @Transactional
    public void deductBalance(String userId, String currency, BigDecimal amount) {
        final String normalizedCurrency = CurrencyUtils.normalizeCurrencyCode(currency);
        
        log.debug("Deducting {} {} from user {}", amount, normalizedCurrency, userId);
        
        UserBalance balance = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate(userId, normalizedCurrency)
                .orElseThrow(() -> new InsufficientBalanceException("No balance found for user " + userId + " in currency " + normalizedCurrency));
        
        BigDecimal newBalance = balance.getBalance().subtract(amount);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for user " + userId + " in currency " + normalizedCurrency);
        }
        
        balance.setBalance(newBalance);
        userBalanceRepository.save(balance);
    }

    @Transactional
    public void addBalance(String userId, String currency, BigDecimal amount) {
        final String normalizedCurrency = CurrencyUtils.normalizeCurrencyCode(currency);
        
        log.debug("Adding {} {} to user {}", amount, normalizedCurrency, userId);
        
        UserBalance balance = userBalanceRepository
                .findByUserIdAndCurrencyForUpdate(userId, normalizedCurrency)
                .orElse(UserBalance.builder()
                        .userId(userId)
                        .currency(normalizedCurrency)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build());
        
        balance.setBalance(balance.getBalance().add(amount));
        userBalanceRepository.save(balance);
    }
}
