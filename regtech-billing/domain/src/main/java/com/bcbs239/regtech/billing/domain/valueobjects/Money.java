package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money value object with currency support and arithmetic operations.
 * Immutable value object that represents monetary amounts with proper currency handling.
 */
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        // Ensure consistent scale for monetary calculations
        amount = amount.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Factory method to create Money with proper scaling
     */
    public static Money of(BigDecimal amount, Currency currency) {
        if (amount == null) {
            return new Money(BigDecimal.ZERO, currency);
        }
        return new Money(amount, currency);
    }
    
    /**
     * Factory method to create Money from string amount
     */
    public static Money of(String amount, Currency currency) {
        return of(new BigDecimal(amount), currency);
    }
    
    /**
     * Factory method to create zero amount in specified currency
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    /**
     * Add two Money amounts with currency validation
     */
    public Result<Money> add(Money other) {
        Result<Void> validation = validateCurrency(other);
        if (validation.isFailure()) {
            return Result.failure(validation.getError().get());
        }
        return Result.success(new Money(this.amount.add(other.amount), this.currency));
    }
    
    /**
     * Subtract Money amount with currency validation
     */
    public Result<Money> subtract(Money other) {
        Result<Void> validation = validateCurrency(other);
        if (validation.isFailure()) {
            return Result.failure(validation.getError().get());
        }
        return Result.success(new Money(this.amount.subtract(other.amount), this.currency));
    }
    
    /**
     * Multiply Money by integer multiplier
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    /**
     * Multiply Money by BigDecimal multiplier
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    /**
     * Divide Money by integer divisor
     */
    public Money divide(int divisor) {
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP), this.currency);
    }
    
    /**
     * Check if amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if amount is negative
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Check if amount is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Compare with another Money amount
     */
    public Result<Integer> compareTo(Money other) {
        Result<Void> validation = validateCurrency(other);
        if (validation.isFailure()) {
            return Result.failure(validation.getError().get());
        }
        return Result.success(this.amount.compareTo(other.amount));
    }
    
    /**
     * Get absolute value
     */
    public Money abs() {
        return new Money(amount.abs(), currency);
    }
    
    /**
     * Negate the amount
     */
    public Money negate() {
        return new Money(amount.negate(), currency);
    }
    
    /**
     * Validate that currencies match for operations
     */
    private Result<Void> validateCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            return Result.failure(new ErrorDetail("CURRENCY_MISMATCH", 
                String.format("Cannot perform operation on different currencies: %s and %s", 
                    this.currency.getCurrencyCode(), other.currency.getCurrencyCode())));
        }
        return Result.success(null);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", amount.setScale(2, RoundingMode.HALF_UP), currency.getCurrencyCode());
    }
}
