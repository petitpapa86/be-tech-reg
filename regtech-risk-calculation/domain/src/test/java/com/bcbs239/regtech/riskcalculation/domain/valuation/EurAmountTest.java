package com.bcbs239.regtech.riskcalculation.domain.valuation;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EurAmount Value Object Tests")
class EurAmountTest {

    @Test
    @DisplayName("Should create valid EUR amount")
    void shouldCreateValidEurAmount() {
        // Given
        BigDecimal value = new BigDecimal("1000.50");
        
        // When
        EurAmount eurAmount = new EurAmount(value);
        
        // Then
        assertEquals(value, eurAmount.value());
    }

    @Test
    @DisplayName("Should create EUR amount using BigDecimal factory")
    void shouldCreateEurAmountUsingBigDecimalFactory() {
        // Given
        BigDecimal value = new BigDecimal("500.25");
        
        // When
        EurAmount eurAmount = EurAmount.of(value);
        
        // Then
        assertEquals(value, eurAmount.value());
    }

    @Test
    @DisplayName("Should create EUR amount using double factory")
    void shouldCreateEurAmountUsingDoubleFactory() {
        // Given
        double value = 750.75;
        
        // When
        EurAmount eurAmount = EurAmount.of(value);
        
        // Then
        assertEquals(BigDecimal.valueOf(value), eurAmount.value());
    }

    @Test
    @DisplayName("Should create zero EUR amount")
    void shouldCreateZeroEurAmount() {
        // When
        EurAmount eurAmount = EurAmount.zero();
        
        // Then
        assertEquals(BigDecimal.ZERO, eurAmount.value());
    }

    @Test
    @DisplayName("Should create from AmountEur")
    void shouldCreateFromAmountEur() {
        // Given
        AmountEur amountEur = AmountEur.of(new BigDecimal("1234.56"));
        
        // When
        EurAmount eurAmount = EurAmount.fromAmountEur(amountEur);
        
        // Then
        assertEquals(amountEur.value(), eurAmount.value());
    }

    @Test
    @DisplayName("Should convert to AmountEur")
    void shouldConvertToAmountEur() {
        // Given
        BigDecimal value = new BigDecimal("987.65");
        EurAmount eurAmount = EurAmount.of(value);
        
        // When
        AmountEur amountEur = eurAmount.toAmountEur();
        
        // Then
        assertEquals(value, amountEur.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        // Given
        BigDecimal value = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new EurAmount(value));
    }

    @Test
    @DisplayName("Should reject negative value")
    void shouldRejectNegativeValue() {
        // Given
        BigDecimal value = new BigDecimal("-100");
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new EurAmount(value));
        assertEquals("EUR amount cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept zero value")
    void shouldAcceptZeroValue() {
        // Given
        BigDecimal value = BigDecimal.ZERO;
        
        // When
        EurAmount eurAmount = new EurAmount(value);
        
        // Then
        assertEquals(BigDecimal.ZERO, eurAmount.value());
    }

    @Test
    @DisplayName("Should be equal when same value")
    void shouldBeEqualWhenSameValue() {
        // Given
        BigDecimal value = new BigDecimal("1000.50");
        EurAmount eurAmount1 = new EurAmount(value);
        EurAmount eurAmount2 = new EurAmount(value);
        
        // When & Then
        assertEquals(eurAmount1, eurAmount2);
        assertEquals(eurAmount1.hashCode(), eurAmount2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different value")
    void shouldNotBeEqualWhenDifferentValue() {
        // Given
        EurAmount eurAmount1 = new EurAmount(new BigDecimal("1000.50"));
        EurAmount eurAmount2 = new EurAmount(new BigDecimal("1000.51"));
        
        // When & Then
        assertNotEquals(eurAmount1, eurAmount2);
    }

    @Test
    @DisplayName("Should handle precision correctly")
    void shouldHandlePrecisionCorrectly() {
        // Given
        BigDecimal value1 = new BigDecimal("1000.50");
        BigDecimal value2 = new BigDecimal("1000.500"); // Same value, different scale
        
        // When
        EurAmount eurAmount1 = EurAmount.of(value1);
        EurAmount eurAmount2 = EurAmount.of(value2);
        
        // Then
        // BigDecimal.equals() considers scale, so these should be different
        assertNotEquals(eurAmount1, eurAmount2);
        // But compareTo should be 0
        assertEquals(0, eurAmount1.value().compareTo(eurAmount2.value()));
    }

    @Test
    @DisplayName("Should round-trip conversion with AmountEur")
    void shouldRoundTripConversionWithAmountEur() {
        // Given
        BigDecimal originalValue = new BigDecimal("1234.5678");
        EurAmount originalEurAmount = EurAmount.of(originalValue);
        
        // When
        AmountEur amountEur = originalEurAmount.toAmountEur();
        EurAmount convertedEurAmount = EurAmount.fromAmountEur(amountEur);
        
        // Then
        assertEquals(originalEurAmount, convertedEurAmount);
        assertEquals(originalValue, convertedEurAmount.value());
    }

    @Test
    @DisplayName("Should handle large values")
    void shouldHandleLargeValues() {
        // Given
        BigDecimal largeValue = new BigDecimal("999999999999.99");
        
        // When
        EurAmount eurAmount = EurAmount.of(largeValue);
        
        // Then
        assertEquals(largeValue, eurAmount.value());
    }

    @Test
    @DisplayName("Should handle small decimal values")
    void shouldHandleSmallDecimalValues() {
        // Given
        BigDecimal smallValue = new BigDecimal("0.01");
        
        // When
        EurAmount eurAmount = EurAmount.of(smallValue);
        
        // Then
        assertEquals(smallValue, eurAmount.value());
    }
}