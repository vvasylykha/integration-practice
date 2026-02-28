package com.example.exchange.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrencyCode {
    
    String message() default "Currency code must be a valid 3-letter ISO code";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
