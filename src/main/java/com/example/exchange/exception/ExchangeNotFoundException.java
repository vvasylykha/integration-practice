package com.example.exchange.exception;

public class ExchangeNotFoundException extends RuntimeException {
    public ExchangeNotFoundException(Long id) {
        super("Exchange not found with id: " + id);
    }
}
