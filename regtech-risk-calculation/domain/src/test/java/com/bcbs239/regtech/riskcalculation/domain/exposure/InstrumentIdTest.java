package com.bcbs239.regtech.riskcalculation.domain.exposure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InstrumentId Value Object Tests")
class InstrumentIdTest {

    @Test
    @DisplayName("Should create valid instrument ID")
    void shouldCreateValidInstrumentId() {
        // Given
        String value = "LOAN-12345";
        
        // When
        InstrumentId instrumentId = new InstrumentId(value);
        
        // Then
        assertEquals(value, instrumentId.value());
    }

    @Test
    @DisplayName("Should create instrument ID using factory method")
    void shouldCreateInstrumentIdUsingFactory() {
        // Given
        String value = "BOND-ISIN-US1234567890";
        
        // When
        InstrumentId instrumentId = InstrumentId.of(value);
        
        // Then
        assertEquals(value, instrumentId.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        // Given
        String value = null;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new InstrumentId(value));
        assertEquals("InstrumentId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject empty value")
    void shouldRejectEmptyValue() {
        // Given
        String value = "";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new InstrumentId(value));
        assertEquals("InstrumentId cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", " \t \n "})
    @DisplayName("Should reject whitespace-only values")
    void shouldRejectWhitespaceOnlyValues(String value) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new InstrumentId(value));
        assertEquals("InstrumentId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should be equal when same value")
    void shouldBeEqualWhenSameValue() {
        // Given
        String value = "LOAN-12345";
        InstrumentId instrumentId1 = new InstrumentId(value);
        InstrumentId instrumentId2 = new InstrumentId(value);
        
        // When & Then
        assertEquals(instrumentId1, instrumentId2);
        assertEquals(instrumentId1.hashCode(), instrumentId2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different value")
    void shouldNotBeEqualWhenDifferentValue() {
        // Given
        InstrumentId instrumentId1 = new InstrumentId("LOAN-12345");
        InstrumentId instrumentId2 = new InstrumentId("BOND-67890");
        
        // When & Then
        assertNotEquals(instrumentId1, instrumentId2);
    }

    @Test
    @DisplayName("Should handle ISIN format")
    void shouldHandleIsinFormat() {
        // Given
        String isin = "US0378331005"; // Apple Inc. ISIN
        
        // When
        InstrumentId instrumentId = InstrumentId.of(isin);
        
        // Then
        assertEquals(isin, instrumentId.value());
    }

    @Test
    @DisplayName("Should handle loan ID format")
    void shouldHandleLoanIdFormat() {
        // Given
        String loanId = "LOAN-2024-001234";
        
        // When
        InstrumentId instrumentId = InstrumentId.of(loanId);
        
        // Then
        assertEquals(loanId, instrumentId.value());
    }

    @Test
    @DisplayName("Should handle derivative contract ID format")
    void shouldHandleDerivativeContractIdFormat() {
        // Given
        String contractId = "DERIV-SWAP-EUR-USD-20241209";
        
        // When
        InstrumentId instrumentId = InstrumentId.of(contractId);
        
        // Then
        assertEquals(contractId, instrumentId.value());
    }

    @Test
    @DisplayName("Should handle special characters")
    void shouldHandleSpecialCharacters() {
        // Given
        String value = "INST-2024/12/09-ABC_123.XYZ";
        
        // When
        InstrumentId instrumentId = InstrumentId.of(value);
        
        // Then
        assertEquals(value, instrumentId.value());
    }

    @Test
    @DisplayName("Should preserve case sensitivity")
    void shouldPreserveCaseSensitivity() {
        // Given
        InstrumentId instrumentId1 = InstrumentId.of("loan-123");
        InstrumentId instrumentId2 = InstrumentId.of("LOAN-123");
        
        // When & Then
        assertNotEquals(instrumentId1, instrumentId2);
        assertEquals("loan-123", instrumentId1.value());
        assertEquals("LOAN-123", instrumentId2.value());
    }
}