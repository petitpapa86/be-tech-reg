package com.bcbs239.regtech.riskcalculation.domain.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Breakdown value object
 */
class BreakdownTest {
    
    @Test
    @DisplayName("Should create breakdown from amounts")
    void shouldCreateBreakdownFromAmounts() {
        Map<String, BigDecimal> amounts = Map.of(
            "ITALY", new BigDecimal("400"),
            "EU_OTHER", new BigDecimal("300"),
            "NON_EUROPEAN", new BigDecimal("300")
        );
        BigDecimal total = new BigDecimal("1000");
        
        Breakdown breakdown = Breakdown.from(amounts, total);
        
        assertNotNull(breakdown);
        assertEquals(3, breakdown.shares().size());
        assertTrue(breakdown.hasCategory("ITALY"));
        assertTrue(breakdown.hasCategory("EU_OTHER"));
        assertTrue(breakdown.hasCategory("NON_EUROPEAN"));
    }
    
    @Test
    @DisplayName("Should calculate correct shares")
    void shouldCalculateCorrectShares() {
        Map<String, BigDecimal> amounts = Map.of(
            "A", new BigDecimal("250"),
            "B", new BigDecimal("750")
        );
        BigDecimal total = new BigDecimal("1000");
        
        Breakdown breakdown = Breakdown.from(amounts, total);
        
        Share shareA = breakdown.getShare("A");
        Share shareB = breakdown.getShare("B");
        
        assertNotNull(shareA);
        assertNotNull(shareB);
        assertEquals(0, new BigDecimal("250").compareTo(shareA.amount().value()));
        assertEquals(new BigDecimal("25.0000"), shareA.percentage());
        assertEquals(0, new BigDecimal("750").compareTo(shareB.amount().value()));
        assertEquals(new BigDecimal("75.0000"), shareB.percentage());
    }
    
    @Test
    @DisplayName("Should handle enum keys")
    void shouldHandleEnumKeys() {
        enum TestEnum { CATEGORY_A, CATEGORY_B }
        
        Map<TestEnum, BigDecimal> amounts = Map.of(
            TestEnum.CATEGORY_A, new BigDecimal("600"),
            TestEnum.CATEGORY_B, new BigDecimal("400")
        );
        BigDecimal total = new BigDecimal("1000");
        
        Breakdown breakdown = Breakdown.from(amounts, total);
        
        assertNotNull(breakdown);
        assertTrue(breakdown.hasCategory("CATEGORY_A"));
        assertTrue(breakdown.hasCategory("CATEGORY_B"));
    }
    
    @Test
    @DisplayName("Should return null for non-existent category")
    void shouldReturnNullForNonExistentCategory() {
        Map<String, BigDecimal> amounts = Map.of("A", new BigDecimal("1000"));
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        assertNull(breakdown.getShare("B"));
    }
    
    @Test
    @DisplayName("Should create immutable shares map")
    void shouldCreateImmutableSharesMap() {
        Map<String, BigDecimal> amounts = Map.of("A", new BigDecimal("1000"));
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        assertThrows(UnsupportedOperationException.class, 
            () -> breakdown.shares().put("B", Share.calculate(BigDecimal.TEN, BigDecimal.TEN)));
    }
    
    @Test
    @DisplayName("Should throw exception for null amounts")
    void shouldThrowExceptionForNullAmounts() {
        assertThrows(NullPointerException.class, 
            () -> Breakdown.from(null, BigDecimal.TEN));
    }
    
    @Test
    @DisplayName("Should throw exception for null total")
    void shouldThrowExceptionForNullTotal() {
        Map<String, BigDecimal> amounts = Map.of("A", BigDecimal.TEN);
        assertThrows(NullPointerException.class, 
            () -> Breakdown.from(amounts, null));
    }
}
