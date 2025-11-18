package com.bcbs239.regtech.dataquality.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BankId Tests")
class BankIdTest {

    @Test
    @DisplayName("Should create valid BankId")
    void shouldCreateValidBankId() {
        BankId bankId = BankId.of("BANK001");
        assertEquals("BANK001", bankId.value());
    }

    @Test
    @DisplayName("Should create BankId with different valid formats")
    void shouldCreateBankIdWithDifferentFormats() {
        assertDoesNotThrow(() -> BankId.of("B123"));
        assertDoesNotThrow(() -> BankId.of("BANK-456"));
        assertDoesNotThrow(() -> BankId.of("ABC"));
        assertDoesNotThrow(() -> BankId.of("12345678901234567890")); // 20 chars
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(NullPointerException.class, () -> BankId.of(null));
        assertThrows(NullPointerException.class, () -> new BankId(null));
    }

    @Test
    @DisplayName("Should reject empty value")
    void shouldRejectEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> BankId.of(""));
        assertThrows(IllegalArgumentException.class, () -> BankId.of("   "));
    }

    @Test
    @DisplayName("Should reject value exceeding 20 characters")
    void shouldRejectValueExceeding20Characters() {
        String tooLong = "A".repeat(21);
        assertThrows(IllegalArgumentException.class, () -> BankId.of(tooLong));
    }

    @Test
    @DisplayName("Should handle boundary length values")
    void shouldHandleBoundaryLengthValues() {
        assertDoesNotThrow(() -> BankId.of("A")); // 1 char
        assertDoesNotThrow(() -> BankId.of("A".repeat(20))); // 20 chars - max
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        BankId bankId1 = BankId.of("BANK001");
        BankId bankId2 = BankId.of("BANK001");
        BankId bankId3 = BankId.of("BANK002");
        
        assertEquals(bankId1, bankId2);
        assertNotEquals(bankId1, bankId3);
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        BankId bankId1 = BankId.of("BANK001");
        BankId bankId2 = BankId.of("BANK001");
        
        assertEquals(bankId1.hashCode(), bankId2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        BankId bankId = BankId.of("BANK001");
        assertEquals("BANK001", bankId.toString());
    }

    @Test
    @DisplayName("Should be usable as map key")
    void shouldBeUsableAsMapKey() {
        BankId key1 = BankId.of("BANK001");
        BankId key2 = BankId.of("BANK001");
        BankId key3 = BankId.of("BANK002");
        
        java.util.Map<BankId, String> map = new java.util.HashMap<>();
        map.put(key1, "Value1");
        
        assertEquals("Value1", map.get(key2)); // Same BankId should retrieve value
        assertNull(map.get(key3)); // Different BankId should not
    }
}
