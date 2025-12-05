package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AmountEur value object
 */
class AmountEurTest {

    @Test
    void shouldCreateAmountEurWithValidValue() {
        // Given
        BigDecimal value = new BigDecimal("100.50");
        
        // When
        AmountEur amount = AmountEur.of(value);
        
        // Then
        assertEquals(new BigDecimal("100.50"), amount.value());
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new AmountEur(null);
        });
    }

    @Test
    void shouldThrowExceptionForNegativeValue() {
        // Given
        BigDecimal negativeValue = new BigDecimal("-10.00");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            AmountEur.of(negativeValue);
        });
    }

    @Test
    void shouldAddAmounts() {
        // Given
        AmountEur amount1 = AmountEur.of(new BigDecimal("100.00"));
        AmountEur amount2 = AmountEur.of(new BigDecimal("50.00"));
        
        // When
        AmountEur result = amount1.add(amount2);
        
        // Then
        assertEquals(new BigDecimal("150.00"), result.value());
    }

    @Test
    void shouldCreateZeroAmount() {
        // When
        AmountEur zero = AmountEur.zero();
        
        // Then
        assertTrue(zero.isZero());
        assertEquals(BigDecimal.ZERO.setScale(2), zero.value());
    }
}