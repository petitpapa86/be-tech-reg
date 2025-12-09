package com.bcbs239.regtech.dataquality.domain.validation.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LeiValidator Tests")
class LeiValidatorTest {

    @Test
    @DisplayName("Should validate correct LEI format")
    void shouldValidateCorrectLeiFormat() {
        // Valid LEIs should be 20 alphanumeric characters
        assertTrue(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R12"));
        assertTrue(LeiValidator.isValidFormat("213800WAVVOPS85N2205"));
    }

    @Test
    @DisplayName("Should reject null LEI")
    void shouldRejectNullLei() {
        assertFalse(LeiValidator.isValidFormat(null));
    }

    @Test
    @DisplayName("Should reject empty LEI")
    void shouldRejectEmptyLei() {
        assertFalse(LeiValidator.isValidFormat(""));
        assertFalse(LeiValidator.isValidFormat("   "));
    }

    @Test
    @DisplayName("Should reject LEI with incorrect length")
    void shouldRejectLeiWithIncorrectLength() {
        assertFalse(LeiValidator.isValidFormat("123456789")); // Too short
        assertFalse(LeiValidator.isValidFormat("12345678901234567890A")); // Too long (21 chars)
        assertFalse(LeiValidator.isValidFormat("1234567890")); // Only 10 chars
    }

    @Test
    @DisplayName("Should reject LEI with invalid characters")
    void shouldRejectLeiWithInvalidCharacters() {
        assertFalse(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R1@")); // Contains @
        assertFalse(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R1$")); // Contains $
        assertFalse(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R-1")); // Contains -
        assertFalse(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R 1")); // Contains space
    }

    @Test
    @DisplayName("Should handle case-insensitive LEI validation")
    void shouldHandleCaseInsensitiveLeiValidation() {
        String leiUpper = "5493001KJTIIGC8Y1R12";
        String leiLower = "5493001kjtiigc8y1r12";
        String leiMixed = "5493001KjTiIgC8y1R12";
        
        // All should be valid if they have correct format
        assertTrue(LeiValidator.isValidFormat(leiUpper));
        assertTrue(LeiValidator.isValidFormat(leiLower));
        assertTrue(LeiValidator.isValidFormat(leiMixed));
    }

    @Test
    @DisplayName("Should normalize valid LEI")
    void shouldNormalizeValidLei() {
        String lei = "5493001kjtiigc8y1r12";
        String normalized = LeiValidator.normalize(lei);
        
        assertNotNull(normalized);
        assertEquals("5493001KJTIIGC8Y1R12", normalized);
        assertEquals(20, normalized.length());
    }

    @Test
    @DisplayName("Should return null for invalid LEI normalization")
    void shouldReturnNullForInvalidLeiNormalization() {
        assertNull(LeiValidator.normalize(null));
        assertNull(LeiValidator.normalize(""));
        assertNull(LeiValidator.normalize("INVALID"));
        assertNull(LeiValidator.normalize("123")); // Too short
    }

    @Test
    @DisplayName("Should normalize LEI with whitespace")
    void shouldNormalizeLeiWithWhitespace() {
        String lei = "  5493001KJTIIGC8Y1R12  ";
        String normalized = LeiValidator.normalize(lei);
        
        assertNotNull(normalized);
        assertEquals("5493001KJTIIGC8Y1R12", normalized);
    }

    @Test
    @DisplayName("Should extract LOU identifier from valid LEI")
    void shouldExtractLouFromValidLei() {
        String lei = "5493001KJTIIGC8Y1R12";
        String lou = LeiValidator.extractLOU(lei);
        
        assertNotNull(lou);
        assertEquals(4, lou.length());
        assertEquals("5493", lou);
    }

    @Test
    @DisplayName("Should return null when extracting LOU from invalid LEI")
    void shouldReturnNullWhenExtractingLouFromInvalidLei() {
        assertNull(LeiValidator.extractLOU(null));
        assertNull(LeiValidator.extractLOU(""));
        assertNull(LeiValidator.extractLOU("INVALID"));
        assertNull(LeiValidator.extractLOU("123")); // Too short
    }

    @Test
    @DisplayName("Should extract entity identifier from valid LEI")
    void shouldExtractEntityIdentifierFromValidLei() {
        String lei = "5493001KJTIIGC8Y1R12";
        String entityId = LeiValidator.extractEntityIdentifier(lei);
        
        assertNotNull(entityId);
        assertEquals(12, entityId.length());
        // Entity identifier is characters 7-18 (0-indexed: 6-17)
        assertEquals("1KJTIIGC8Y1R", entityId);
    }

    @Test
    @DisplayName("Should return null when extracting entity identifier from invalid LEI")
    void shouldReturnNullWhenExtractingEntityIdentifierFromInvalidLei() {
        assertNull(LeiValidator.extractEntityIdentifier(null));
        assertNull(LeiValidator.extractEntityIdentifier(""));
        assertNull(LeiValidator.extractEntityIdentifier("INVALID"));
    }

    @Test
    @DisplayName("Should check if LEI is from specific LOU")
    void shouldCheckIfLeiIsFromSpecificLou() {
        String lei = "5493001KJTIIGC8Y1R12";
        
        assertTrue(LeiValidator.isFromLOU(lei, "5493"));
        assertTrue(LeiValidator.isFromLOU(lei, "5493")); // Exact match
        assertFalse(LeiValidator.isFromLOU(lei, "2138"));
        assertFalse(LeiValidator.isFromLOU(lei, "XXXX"));
    }

    @Test
    @DisplayName("Should handle case-insensitive LOU check")
    void shouldHandleCaseInsensitiveLouCheck() {
        String lei = "5493001KJTIIGC8Y1R12";
        
        assertTrue(LeiValidator.isFromLOU(lei, "5493"));
        assertTrue(LeiValidator.isFromLOU(lei.toLowerCase(), "5493"));
    }

    @Test
    @DisplayName("Should return false for LOU check with invalid LEI")
    void shouldReturnFalseForLouCheckWithInvalidLei() {
        assertFalse(LeiValidator.isFromLOU(null, "5493"));
        assertFalse(LeiValidator.isFromLOU("", "5493"));
        assertFalse(LeiValidator.isFromLOU("INVALID", "5493"));
    }

    @Test
    @DisplayName("Should validate alphanumeric LEI codes")
    void shouldValidateAlphanumericLeiCodes() {
        // LEI can contain both letters and numbers (using real valid LEIs)
        assertTrue(LeiValidator.isValidFormat("5493001KJTIIGC8Y1R12")); // Mix of letters and numbers
        assertTrue(LeiValidator.isValidFormat("213800WAVVOPS85N2205")); // Mix of letters and numbers
        assertTrue(LeiValidator.isValidFormat("549300E9PC51EN656011")); // Apple Inc LEI
    }

    @Test
    @DisplayName("Should handle LEI with all numbers")
    void shouldHandleLeiWithAllNumbers() {
        // LEI with all numbers will fail check digit validation in most cases
        // This test verifies that the validator correctly rejects invalid check digits
        String allNumbers = "12345678901234567890";
        assertFalse(LeiValidator.isValidFormat(allNumbers)); // Invalid check digits
    }

    @Test
    @DisplayName("Should handle LEI with all letters")
    void shouldHandleLeiWithAllLetters() {
        // LEI with all letters will fail check digit validation (check digits must be numeric)
        // This test verifies that the validator correctly rejects invalid format
        String allLetters = "ABCDEFGHIJKLMNOPQRST";
        assertFalse(LeiValidator.isValidFormat(allLetters)); // Last 2 chars must be digits
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "5493001KJTIIGC8Y1R12",
        "213800WAVVOPS85N2205",
        "  5493001KJTIIGC8Y1R12  ",
        "5493001kjtiigc8y1r12"
    })
    @DisplayName("Should handle various valid LEI formats")
    void shouldHandleVariousValidLeiFormats(String lei) {
        String normalized = LeiValidator.normalize(lei);
        assertNotNull(normalized);
        assertEquals(20, normalized.length());
        assertTrue(normalized.matches("^[A-Z0-9]{20}$"));
    }

    @Test
    @DisplayName("Should validate that LEI structure is correct")
    void shouldValidateLeiStructure() {
        String lei = "5493001KJTIIGC8Y1R12";
        
        // LOU identifier (first 4 chars)
        assertEquals("5493", LeiValidator.extractLOU(lei));
        
        // Entity identifier (chars 7-18)
        assertEquals("1KJTIIGC8Y1R", LeiValidator.extractEntityIdentifier(lei));
        
        // Full LEI should be 20 characters
        assertEquals(20, LeiValidator.normalize(lei).length());
    }
}
