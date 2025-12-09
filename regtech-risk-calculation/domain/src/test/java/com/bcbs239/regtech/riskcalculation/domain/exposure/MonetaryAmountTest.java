package com.bcbs239.regtech.riskcalculation.domain.exposure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonetaryAmount Value Object Tests")
class MonetaryAmountTest {

    @Test
    @DisplayName("Should create valid monetary amount")
    void shouldCreateValidMonetaryAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1000.50");
        String currency = "USD";
        
        // When
        MonetaryAmount monetaryAmount = new MonetaryAmount(amount, currency);
        
        // Then
        assertEquals(amount, monetaryAmount.amount());
        assertEquals(currency, monetaryAmount.currencyCode());
    }

    @Test
    @DisplayName("Should create monetary amount using factory method")
    void shouldCreateMonetaryAmountUsingFactory() {
        // Given
        BigDecimal amount = new BigDecimal("500.25");
        String currency = "eur";
        
        // When
        MonetaryAmount monetaryAmount = MonetaryAmount.of(amount, currency);
        
        // Then
        assertEquals(amount, monetaryAmount.amount());
        assertEquals("EUR", monetaryAmount.currencyCode()); // Should be uppercase
    }

    @Test
    @DisplayName("Should create zero amount")
    void shouldCreateZeroAmount() {
        // Given
        String currency = "GBP";
        
        // When
        MonetaryAmount monetaryAmount = MonetaryAmount.zero(currency);
        
        // Then
        assertEquals(BigDecimal.ZERO, monetaryAmount.amount());
        assertEquals("GBP", monetaryAmount.currencyCode());
    }

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        // Given
        BigDecimal amount = null;
        String currency = "USD";
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new MonetaryAmount(amount, currency));
    }

    @Test
    @DisplayName("Should reject null currency code")
    void shouldRejectNullCurrencyCode() {
        // Given
        BigDecimal amount = new BigDecimal("100");
        String currency = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new MonetaryAmount(amount, currency));
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        // Given
        BigDecimal amount = new BigDecimal("-100");
        String currency = "USD";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new MonetaryAmount(amount, currency));
        assertEquals("Amount cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject empty currency code")
    void shouldRejectEmptyCurrencyCode() {
        // Given
        BigDecimal amount = new BigDecimal("100");
        String currency = "";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new MonetaryAmount(amount, currency));
        assertEquals("Currency code cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject whitespace-only currency code")
    void shouldRejectWhitespaceOnlyCurrencyCode() {
        // Given
        BigDecimal amount = new BigDecimal("100");
        String currency = "   ";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new MonetaryAmount(amount, currency));
        assertEquals("Currency code cannot be empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "USDD", "U", ""})
    @DisplayName("Should reject invalid currency code length")
    void shouldRejectInvalidCurrencyCodeLength(String currency) {
        // Given
        BigDecimal amount = new BigDecimal("100");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new MonetaryAmount(amount, currency));
        assertEquals("Currency code must be 3 characters (ISO 4217)", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept zero amount")
    void shouldAcceptZeroAmount() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;
        String currency = "USD";
        
        // When
        MonetaryAmount monetaryAmount = new MonetaryAmount(amount, currency);
        
        // Then
        assertEquals(BigDecimal.ZERO, monetaryAmount.amount());
        assertEquals("USD", monetaryAmount.currencyCode());
    }

    @Test
    @DisplayName("Should be equal when same amount and currency")
    void shouldBeEqualWhenSameAmountAndCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("1000.50");
        String currency = "USD";
        MonetaryAmount amount1 = new MonetaryAmount(amount, currency);
        MonetaryAmount amount2 = new MonetaryAmount(amount, currency);
        
        // When & Then
        assertEquals(amount1, amount2);
        assertEquals(amount1.hashCode(), amount2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different amount")
    void shouldNotBeEqualWhenDifferentAmount() {
        // Given
        MonetaryAmount amount1 = new MonetaryAmount(new BigDecimal("1000.50"), "USD");
        MonetaryAmount amount2 = new MonetaryAmount(new BigDecimal("1000.51"), "USD");
        
        // When & Then
        assertNotEquals(amount1, amount2);
    }

    @Test
    @DisplayName("Should not be equal when different currency")
    void shouldNotBeEqualWhenDifferentCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("1000.50");
        MonetaryAmount amount1 = new MonetaryAmount(amount, "USD");
        MonetaryAmount amount2 = new MonetaryAmount(amount, "EUR");
        
        // When & Then
        assertNotEquals(amount1, amount2);
    }
}