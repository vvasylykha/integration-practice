package com.example.exchange.util;

public class CurrencyUtils {

    private static final int CURRENCY_CODE_LENGTH = 3;

    private CurrencyUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String normalizeCurrencyCode(String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }
        
        String normalized = currency.trim().toUpperCase().replaceAll("[^A-Z]", "");
        
        if (normalized.length() != CURRENCY_CODE_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Currency code must be exactly %d characters, got: %s", 
                    CURRENCY_CODE_LENGTH, currency));
        }
        
        return normalized;
    }

    public static boolean isValidCurrencyCode(String currency) {
        if (currency == null || currency.isBlank()) {
            return false;
        }
        
        String normalized = currency.trim().toUpperCase().replaceAll("[^A-Z]", "");
        return normalized.length() == CURRENCY_CODE_LENGTH;
    }
}
