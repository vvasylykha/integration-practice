package com.example.exchange.service;

import com.example.exchange.dto.request.ExchangeRequest;
import com.example.exchange.messaging.event.ExchangeCompletedEvent;
import com.example.exchange.messaging.publisher.ExchangeEventPublisher;
import com.example.exchange.exception.ExchangeNotFoundException;
import com.example.exchange.exception.InvalidCurrencyException;
import com.example.exchange.model.Exchange;
import com.example.exchange.repository.ExchangeRepository;
import com.example.exchange.util.CurrencyUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final RateService rateService;
    private final BalanceService balanceService;
    private final ExchangeEventPublisher eventPublisher;

    private final BigDecimal commissionRate;

    public ExchangeService(ExchangeRepository exchangeRepository,
                           RateService rateService,
                           BalanceService balanceService,
                           ExchangeEventPublisher eventPublisher,
                           @Value("${exchange.commission.rate}") BigDecimal commissionRate) {
        this.exchangeRepository = exchangeRepository;
        this.rateService = rateService;
        this.balanceService = balanceService;
        this.eventPublisher = eventPublisher;
        this.commissionRate = commissionRate;
    }

    @Transactional
    public Exchange performExchange(ExchangeRequest request) {
        String fromCurrency = CurrencyUtils.normalizeCurrencyCode(request.getFromCurrency());
        String toCurrency = CurrencyUtils.normalizeCurrencyCode(request.getToCurrency());
        
        log.info("Processing exchange request: {} from {} to {} for user {}",
                request.getAmount(), fromCurrency, toCurrency, request.getUserId());

        validateRequest(fromCurrency, toCurrency);

        BigDecimal exchangeRate = rateService.getExchangeRate(fromCurrency, toCurrency);
        BigDecimal commission = calculateCommission(request.getAmount());
        BigDecimal convertedAmount = calculateConvertedAmount(request.getAmount(), exchangeRate);

        processBalanceTransfer(request.getUserId(), fromCurrency, toCurrency, 
                request.getAmount(), commission, convertedAmount);

        Exchange exchange = buildExchange(request, fromCurrency, toCurrency, 
                exchangeRate, commission, convertedAmount);
        exchange = exchangeRepository.save(exchange);
        
        log.info("Exchange completed successfully: ID {}", exchange.getId());
        publishExchangeCompletedEvent(exchange);

        return exchange;
    }

    public Exchange getExchangeById(Long id) {
        return exchangeRepository.findById(id)
                .orElseThrow(() -> new ExchangeNotFoundException(id));
    }

    public List<Exchange> getUserExchanges(String userId) {
        return exchangeRepository.findByUserId(userId);
    }

    public Page<Exchange> getUserExchangesPaginated(String userId, Pageable pageable) {
        return exchangeRepository.findByUserId(userId, pageable);
    }

    public List<Exchange> getUserExchanges(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return getUserExchangesPaginated(userId, pageable).getContent();
    }

    private void validateRequest(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            throw new InvalidCurrencyException("Source and target currencies cannot be the same");
        }
    }

    private BigDecimal calculateCommission(BigDecimal amount) {
        return amount.multiply(commissionRate).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConvertedAmount(BigDecimal amount, BigDecimal exchangeRate) {
        return amount.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);
    }

    private void processBalanceTransfer(String userId, String fromCurrency, String toCurrency,
                                        BigDecimal amount, BigDecimal commission, BigDecimal convertedAmount) {
        BigDecimal totalDeduction = amount.add(commission);
        balanceService.validateSufficientBalance(userId, fromCurrency, totalDeduction);
        balanceService.deductBalance(userId, fromCurrency, totalDeduction);
        balanceService.addBalance(userId, toCurrency, convertedAmount);
    }

    private Exchange buildExchange(ExchangeRequest request, String fromCurrency, String toCurrency,
                                   BigDecimal exchangeRate, BigDecimal commission, BigDecimal convertedAmount) {
        return Exchange.builder()
                .userId(request.getUserId())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .commission(commission)
                .status(Exchange.ExchangeStatus.COMPLETED)
                .build();
    }

    private void publishExchangeCompletedEvent(Exchange exchange) {
        ExchangeCompletedEvent event = ExchangeCompletedEvent.builder()
                .exchangeId(exchange.getId())
                .userId(exchange.getUserId())
                .fromCurrency(exchange.getFromCurrency())
                .toCurrency(exchange.getToCurrency())
                .amount(exchange.getAmount())
                .convertedAmount(exchange.getConvertedAmount())
                .exchangeRate(exchange.getExchangeRate())
                .commission(exchange.getCommission())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishCompletedEvent(event);
    }
}
