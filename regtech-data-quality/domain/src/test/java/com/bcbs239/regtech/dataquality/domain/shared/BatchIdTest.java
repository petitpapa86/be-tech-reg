package com.bcbs239.regtech.dataquality.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BatchId Tests")
class BatchIdTest {

    @Test
    @DisplayName("Should generate valid BatchId with correct format")
    void shouldGenerateValidBatchId() {
        BatchId batchId = BatchId.generate();
        
        assertNotNull(batchId);
        assertNotNull(batchId.value());
        assertTrue(batchId.value().startsWith("batch_"));
    }

    @Test
    @DisplayName("Should generate unique BatchIds")
    void shouldGenerateUniqueBatchIds() {
        BatchId batchId1 = BatchId.generate();
        BatchId batchId2 = BatchId.generate();
        
        assertNotEquals(batchId1, batchId2);
        assertNotEquals(batchId1.value(), batchId2.value());
    }

    @Test
    @DisplayName("Should create BatchId from valid string")
    void shouldCreateBatchIdFromValidString() {
        String validBatchId = "batch_20231115_143000_123e4567-e89b-12d3-a456-426614174000";
        BatchId batchId = BatchId.of(validBatchId);
        
        assertEquals(validBatchId, batchId.value());
    }

    @Test
    @DisplayName("Should accept simple batch prefix format")
    void shouldAcceptSimpleBatchFormat() {
        assertDoesNotThrow(() -> BatchId.of("batch_test"));
        assertDoesNotThrow(() -> BatchId.of("batch_123"));
        assertDoesNotThrow(() -> BatchId.of("batch_20231115_143000"));
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(NullPointerException.class, () -> BatchId.of(null));
        assertThrows(NullPointerException.class, () -> new BatchId(null));
    }

    @Test
    @DisplayName("Should reject empty value")
    void shouldRejectEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> BatchId.of(""));
        assertThrows(IllegalArgumentException.class, () -> BatchId.of("   "));
    }

    @Test
    @DisplayName("Should reject value without batch prefix")
    void shouldRejectValueWithoutBatchPrefix() {
        assertThrows(IllegalArgumentException.class, () -> BatchId.of("notbatch_123"));
        assertThrows(IllegalArgumentException.class, () -> BatchId.of("BATCH_123")); // wrong case
        assertThrows(IllegalArgumentException.class, () -> BatchId.of("123_batch"));
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        String batchValue = "batch_20231115_143000_uuid";
        BatchId batchId1 = BatchId.of(batchValue);
        BatchId batchId2 = BatchId.of(batchValue);
        BatchId batchId3 = BatchId.of("batch_20231115_143001_uuid");
        
        assertEquals(batchId1, batchId2);
        assertNotEquals(batchId1, batchId3);
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        String batchValue = "batch_20231115_143000_uuid";
        BatchId batchId1 = BatchId.of(batchValue);
        BatchId batchId2 = BatchId.of(batchValue);
        
        assertEquals(batchId1.hashCode(), batchId2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        String batchValue = "batch_20231115_143000_uuid";
        BatchId batchId = BatchId.of(batchValue);
        
        assertEquals(batchValue, batchId.toString());
    }

    @Test
    @DisplayName("Generated BatchId should contain timestamp and UUID")
    void generatedBatchIdShouldContainTimestampAndUuid() {
        BatchId batchId = BatchId.generate();
        String value = batchId.value();
        
        // Should have format: batch_YYYYMMDD_HHMMSS_UUID
        String[] parts = value.split("_");
        assertTrue(parts.length >= 4, "Should have at least 4 parts separated by underscores");
        assertEquals("batch", parts[0]);
        // parts[1] should be date (YYYYMMDD)
        assertEquals(8, parts[1].length());
        // parts[2] should be time (HHMMSS)
        assertEquals(6, parts[2].length());
        // parts[3] onwards should be UUID
        assertTrue(parts[3].length() > 0);
    }

    @Test
    @DisplayName("Should be usable as map key")
    void shouldBeUsableAsMapKey() {
        BatchId key1 = BatchId.of("batch_test1");
        BatchId key2 = BatchId.of("batch_test1");
        BatchId key3 = BatchId.of("batch_test2");
        
        java.util.Map<BatchId, String> map = new java.util.HashMap<>();
        map.put(key1, "Value1");
        
        assertEquals("Value1", map.get(key2)); // Same BatchId should retrieve value
        assertNull(map.get(key3)); // Different BatchId should not
    }

    @Test
    @DisplayName("Should handle long batch identifiers")
    void shouldHandleLongBatchIdentifiers() {
        String longBatchId = "batch_" + "a".repeat(100);
        assertDoesNotThrow(() -> BatchId.of(longBatchId));
    }
}
