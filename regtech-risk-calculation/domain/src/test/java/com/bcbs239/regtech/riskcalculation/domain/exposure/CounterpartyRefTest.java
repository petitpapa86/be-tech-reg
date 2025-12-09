package com.bcbs239.regtech.riskcalculation.domain.exposure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CounterpartyRef Value Object Tests")
class CounterpartyRefTest {

    @Test
    @DisplayName("Should create valid counterparty reference with LEI")
    void shouldCreateValidCounterpartyRefWithLei() {
        // Given
        String counterpartyId = "CP-12345";
        String name = "ABC Corporation";
        String leiCode = "213800ABCDEFGHIJKL12";
        
        // When
        CounterpartyRef counterpartyRef = new CounterpartyRef(counterpartyId, name, Optional.of(leiCode));
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isPresent());
        assertEquals(leiCode, counterpartyRef.leiCode().get());
    }

    @Test
    @DisplayName("Should create valid counterparty reference without LEI")
    void shouldCreateValidCounterpartyRefWithoutLei() {
        // Given
        String counterpartyId = "CP-67890";
        String name = "XYZ Limited";
        
        // When
        CounterpartyRef counterpartyRef = new CounterpartyRef(counterpartyId, name, Optional.empty());
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should create counterparty reference using factory method with LEI")
    void shouldCreateCounterpartyRefUsingFactoryWithLei() {
        // Given
        String counterpartyId = "CP-11111";
        String name = "Test Company";
        String leiCode = "213800TESTCOMPANY123";
        
        // When
        CounterpartyRef counterpartyRef = CounterpartyRef.of(counterpartyId, name, leiCode);
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isPresent());
        assertEquals(leiCode, counterpartyRef.leiCode().get());
    }

    @Test
    @DisplayName("Should create counterparty reference using factory method without LEI")
    void shouldCreateCounterpartyRefUsingFactoryWithoutLei() {
        // Given
        String counterpartyId = "CP-22222";
        String name = "Another Company";
        
        // When
        CounterpartyRef counterpartyRef = CounterpartyRef.of(counterpartyId, name);
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should handle null LEI code in factory method")
    void shouldHandleNullLeiCodeInFactory() {
        // Given
        String counterpartyId = "CP-33333";
        String name = "Null LEI Company";
        String leiCode = null;
        
        // When
        CounterpartyRef counterpartyRef = CounterpartyRef.of(counterpartyId, name, leiCode);
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty LEI code in factory method")
    void shouldHandleEmptyLeiCodeInFactory() {
        // Given
        String counterpartyId = "CP-44444";
        String name = "Empty LEI Company";
        String leiCode = "";
        
        // When
        CounterpartyRef counterpartyRef = CounterpartyRef.of(counterpartyId, name, leiCode);
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should handle whitespace-only LEI code in factory method")
    void shouldHandleWhitespaceOnlyLeiCodeInFactory() {
        // Given
        String counterpartyId = "CP-55555";
        String name = "Whitespace LEI Company";
        String leiCode = "   ";
        
        // When
        CounterpartyRef counterpartyRef = CounterpartyRef.of(counterpartyId, name, leiCode);
        
        // Then
        assertEquals(counterpartyId, counterpartyRef.counterpartyId());
        assertEquals(name, counterpartyRef.name());
        assertTrue(counterpartyRef.leiCode().isEmpty());
    }

    @Test
    @DisplayName("Should reject null counterparty ID")
    void shouldRejectNullCounterpartyId() {
        // Given
        String counterpartyId = null;
        String name = "Test Company";
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
        // Given
        String counterpartyId = "CP-12345";
        String name = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
    }

    @Test
    @DisplayName("Should reject null LEI optional")
    void shouldRejectNullLeiOptional() {
        // Given
        String counterpartyId = "CP-12345";
        String name = "Test Company";
        Optional<String> leiCode = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new CounterpartyRef(counterpartyId, name, leiCode));
    }

    @Test
    @DisplayName("Should reject empty counterparty ID")
    void shouldRejectEmptyCounterpartyId() {
        // Given
        String counterpartyId = "";
        String name = "Test Company";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
        assertEquals("Counterparty ID cannot be empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", " \t \n "})
    @DisplayName("Should reject whitespace-only counterparty ID")
    void shouldRejectWhitespaceOnlyCounterpartyId(String counterpartyId) {
        // Given
        String name = "Test Company";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
        assertEquals("Counterparty ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject empty name")
    void shouldRejectEmptyName() {
        // Given
        String counterpartyId = "CP-12345";
        String name = "";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
        assertEquals("Counterparty name cannot be empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", " \t \n "})
    @DisplayName("Should reject whitespace-only name")
    void shouldRejectWhitespaceOnlyName(String name) {
        // Given
        String counterpartyId = "CP-12345";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CounterpartyRef(counterpartyId, name, Optional.empty()));
        assertEquals("Counterparty name cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should be equal when same values")
    void shouldBeEqualWhenSameValues() {
        // Given
        String counterpartyId = "CP-12345";
        String name = "Test Company";
        String leiCode = "213800TESTCOMPANY123";
        CounterpartyRef ref1 = CounterpartyRef.of(counterpartyId, name, leiCode);
        CounterpartyRef ref2 = CounterpartyRef.of(counterpartyId, name, leiCode);
        
        // When & Then
        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different counterparty ID")
    void shouldNotBeEqualWhenDifferentCounterpartyId() {
        // Given
        String name = "Test Company";
        CounterpartyRef ref1 = CounterpartyRef.of("CP-12345", name);
        CounterpartyRef ref2 = CounterpartyRef.of("CP-67890", name);
        
        // When & Then
        assertNotEquals(ref1, ref2);
    }

    @Test
    @DisplayName("Should not be equal when different name")
    void shouldNotBeEqualWhenDifferentName() {
        // Given
        String counterpartyId = "CP-12345";
        CounterpartyRef ref1 = CounterpartyRef.of(counterpartyId, "Company A");
        CounterpartyRef ref2 = CounterpartyRef.of(counterpartyId, "Company B");
        
        // When & Then
        assertNotEquals(ref1, ref2);
    }

    @Test
    @DisplayName("Should not be equal when different LEI presence")
    void shouldNotBeEqualWhenDifferentLeiPresence() {
        // Given
        String counterpartyId = "CP-12345";
        String name = "Test Company";
        CounterpartyRef ref1 = CounterpartyRef.of(counterpartyId, name);
        CounterpartyRef ref2 = CounterpartyRef.of(counterpartyId, name, "213800TESTCOMPANY123");
        
        // When & Then
        assertNotEquals(ref1, ref2);
    }
}