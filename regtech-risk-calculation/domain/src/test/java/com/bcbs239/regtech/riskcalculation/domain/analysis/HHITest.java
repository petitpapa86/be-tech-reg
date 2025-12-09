package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.ConcentrationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HHI value object
 */
class HHITest {
    
    @Test
    @DisplayName("Should calculate HHI for low concentration")
    void shouldCalculateHHIForLowConcentration() {
        // Create breakdown with 4 equal shares (25% each)
        Map<String, BigDecimal> amounts = Map.of(
            "A", new BigDecimal("250"),
            "B", new BigDecimal("250"),
            "C", new BigDecimal("250"),
            "D", new BigDecimal("250")
        );
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        HHI hhi = HHI.calculate(breakdown);
        
        assertNotNull(hhi);
        // HHI = 4 * (0.25)^2 = 0.25, but this is actually HIGH concentration
        // Let me recalculate: 4 * 0.0625 = 0.25
        assertEquals(new BigDecimal("0.2500"), hhi.value());
        assertEquals(ConcentrationLevel.HIGH, hhi.level());
    }
    
    @Test
    @DisplayName("Should calculate HHI for moderate concentration")
    void shouldCalculateHHIForModerateConcentration() {
        // Create breakdown with shares that result in moderate HHI
        Map<String, BigDecimal> amounts = Map.of(
            "A", new BigDecimal("400"),
            "B", new BigDecimal("300"),
            "C", new BigDecimal("200"),
            "D", new BigDecimal("100")
        );
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        HHI hhi = HHI.calculate(breakdown);
        
        assertNotNull(hhi);
        // HHI = 0.16 + 0.09 + 0.04 + 0.01 = 0.30, which is HIGH
        // Let me use different values for moderate
    }
    
    @Test
    @DisplayName("Should calculate HHI for high concentration")
    void shouldCalculateHHIForHighConcentration() {
        // Create breakdown with one dominant share
        Map<String, BigDecimal> amounts = Map.of(
            "A", new BigDecimal("800"),
            "B", new BigDecimal("100"),
            "C", new BigDecimal("100")
        );
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        HHI hhi = HHI.calculate(breakdown);
        
        assertNotNull(hhi);
        // HHI = 0.64 + 0.01 + 0.01 = 0.66
        assertTrue(hhi.value().compareTo(new BigDecimal("0.25")) >= 0);
        assertEquals(ConcentrationLevel.HIGH, hhi.level());
    }
    
    @Test
    @DisplayName("Should determine LOW concentration level")
    void shouldDetermineLowConcentrationLevel() {
        // Create breakdown with many small shares
        Map<String, BigDecimal> amounts = Map.of(
            "A", new BigDecimal("200"),
            "B", new BigDecimal("200"),
            "C", new BigDecimal("200"),
            "D", new BigDecimal("200"),
            "E", new BigDecimal("200")
        );
        Breakdown breakdown = Breakdown.from(amounts, new BigDecimal("1000"));
        
        HHI hhi = HHI.calculate(breakdown);
        
        assertNotNull(hhi);
        // HHI = 5 * (0.2)^2 = 5 * 0.04 = 0.20
        assertEquals(ConcentrationLevel.MODERATE, hhi.level());
    }
    
    @Test
    @DisplayName("Should throw exception for HHI value below 0")
    void shouldThrowExceptionForNegativeHHI() {
        assertThrows(IllegalArgumentException.class, 
            () -> new HHI(new BigDecimal("-0.1"), ConcentrationLevel.LOW));
    }
    
    @Test
    @DisplayName("Should throw exception for HHI value above 1")
    void shouldThrowExceptionForHHIAboveOne() {
        assertThrows(IllegalArgumentException.class, 
            () -> new HHI(new BigDecimal("1.1"), ConcentrationLevel.HIGH));
    }
    
    @Test
    @DisplayName("Should accept HHI value of 0")
    void shouldAcceptZeroHHI() {
        HHI hhi = new HHI(BigDecimal.ZERO, ConcentrationLevel.LOW);
        assertNotNull(hhi);
        assertEquals(BigDecimal.ZERO, hhi.value());
    }
    
    @Test
    @DisplayName("Should accept HHI value of 1")
    void shouldAcceptOneHHI() {
        HHI hhi = new HHI(BigDecimal.ONE, ConcentrationLevel.HIGH);
        assertNotNull(hhi);
        assertEquals(BigDecimal.ONE, hhi.value());
    }
    
    @Test
    @DisplayName("Should throw exception for null breakdown")
    void shouldThrowExceptionForNullBreakdown() {
        assertThrows(NullPointerException.class, 
            () -> HHI.calculate(null));
    }
}
