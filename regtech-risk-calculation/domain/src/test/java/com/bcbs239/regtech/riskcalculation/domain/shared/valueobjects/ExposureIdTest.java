package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExposureId Value Object Tests")
class ExposureIdTest {

    @Test
    @DisplayName("Should create valid exposure ID")
    void shouldCreateValidExposureId() {
        // Given
        String value = "EXP-12345";
        
        // When
        ExposureId exposureId = new ExposureId(value);
        
        // Then
        assertEquals(value, exposureId.value());
    }

    @Test
    @DisplayName("Should create exposure ID using factory method")
    void shouldCreateExposureIdUsingFactory() {
        // Given
        String value = "EXP-67890";
        
        // When
        ExposureId exposureId = ExposureId.of(value);
        
        // Then
        assertEquals(value, exposureId.value());
    }

    @Test
    @DisplayName("Should generate unique exposure ID")
    void shouldGenerateUniqueExposureId() {
        // When
        ExposureId exposureId1 = ExposureId.generate();
        ExposureId exposureId2 = ExposureId.generate();
        
        // Then
        assertNotNull(exposureId1.value());
        assertNotNull(exposureId2.value());
        assertNotEquals(exposureId1.value(), exposureId2.value());
        // UUID format check (36 characters with hyphens)
        assertEquals(36, exposureId1.value().length());
        assertTrue(exposureId1.value().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        // Given
        String value = null;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new ExposureId(value));
        assertEquals("ExposureId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject empty value")
    void shouldRejectEmptyValue() {
        // Given
        String value = "";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new ExposureId(value));
        assertEquals("ExposureId cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", " \t \n "})
    @DisplayName("Should reject whitespace-only values")
    void shouldRejectWhitespaceOnlyValues(String value) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new ExposureId(value));
        assertEquals("ExposureId cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should be equal when same value")
    void shouldBeEqualWhenSameValue() {
        // Given
        String value = "EXP-12345";
        ExposureId exposureId1 = new ExposureId(value);
        ExposureId exposureId2 = new ExposureId(value);
        
        // When & Then
        assertEquals(exposureId1, exposureId2);
        assertEquals(exposureId1.hashCode(), exposureId2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when different value")
    void shouldNotBeEqualWhenDifferentValue() {
        // Given
        ExposureId exposureId1 = new ExposureId("EXP-12345");
        ExposureId exposureId2 = new ExposureId("EXP-67890");
        
        // When & Then
        assertNotEquals(exposureId1, exposureId2);
    }

    @Test
    @DisplayName("Should handle special characters in value")
    void shouldHandleSpecialCharactersInValue() {
        // Given
        String value = "EXP-2024/12/09-ABC_123";
        
        // When
        ExposureId exposureId = ExposureId.of(value);
        
        // Then
        assertEquals(value, exposureId.value());
    }

    @Test
    @DisplayName("Should preserve case sensitivity")
    void shouldPreserveCaseSensitivity() {
        // Given
        ExposureId exposureId1 = ExposureId.of("exp-123");
        ExposureId exposureId2 = ExposureId.of("EXP-123");
        
        // When & Then
        assertNotEquals(exposureId1, exposureId2);
        assertEquals("exp-123", exposureId1.value());
        assertEquals("EXP-123", exposureId2.value());
    }
}