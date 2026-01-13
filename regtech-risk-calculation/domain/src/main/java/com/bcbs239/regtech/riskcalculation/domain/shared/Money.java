package com.bcbs239.regtech.riskcalculation.domain.shared;

import org.jspecify.annotations.NonNull;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: Money
 * Represent an amount and its currency.
 */
public record Money(
    @NonNull BigDecimal amount,
    @NonNull String currency
) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
    }

    @NonNull
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    @NonNull
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(double factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match: " + this.currency + " vs " + other.currency);
        }
    }
}
