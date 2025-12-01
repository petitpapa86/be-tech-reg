package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Share value object
 */
class ShareTest {
    
    @Test
    @DisplayName("Should calculate share correctly")
    void shouldCalculateShareCorrectly() {
        BigDecimal amount = new BigDecimal("250.00");
        BigDecimal total = new BigDecimal("1000.00");
        
        Share share = Share.calculate(amount, total);
        
        assertNotNull(share);
        assertEquals(new BigDecimal("250.00"), share.amount().value());
        assertEquals(new BigDecimal("25.0000"), share.percentage());
    }
    
    @Test
    @DisplayName("Should handle zero total")
    void shouldHandleZeroTotal() {
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal total = BigDecimal.ZERO;
        
        Share share = Share.calculate(amount, total);
        
        assertNotNull(share);
        assertEquals(BigDecimal.ZERO, share.amount().value());
        assertEquals(BigDecimal.ZERO, share.percentage());
    }
    
    @Test
    @DisplayName("Should calculate 100% share")
    void shouldCalculateFullShare() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal total = new BigDecimal("1000.00");
        
        Share share = Share.calculate(amount, total);
        
        assertNotNull(share);
        assertEquals(new BigDecimal("1000.00"), share.amount().value());
        assertEquals(new BigDecimal("100.0000"), share.percentage());
    }
    
    @Test
    @DisplayName("Should get decimal share for HHI calculation")
    void shouldGetDecimalShare() {
        BigDecimal amount = new BigDecimal("250.00");
        BigDecimal total = new BigDecimal("1000.00");
        
        Share share = Share.calculate(amount, total);
        BigDecimal decimal = share.getDecimalShare();
        
        assertEquals(new BigDecimal("0.250000"), decimal);
    }
    
    @Test
    @DisplayName("Should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        assertThrows(NullPointerException.class, 
            () -> Share.calculate(null, BigDecimal.TEN));
    }
    
    @Test
    @DisplayName("Should throw exception for null total")
    void shouldThrowExceptionForNullTotal() {
        assertThrows(NullPointerException.class, 
            () -> Share.calculate(BigDecimal.TEN, null));
    }
    
    @Test
    @DisplayName("Should throw exception for negative percentage")
    void shouldThrowExceptionForNegativePercentage() {
        assertThrows(IllegalArgumentException.class, 
            () -> new Share(EurAmount.zero(), new BigDecimal("-1")));
    }
    
    @Test
    @DisplayName("Should throw exception for percentage over 100")
    void shouldThrowExceptionForPercentageOver100() {
        assertThrows(IllegalArgumentException.class, 
            () -> new Share(EurAmount.zero(), new BigDecimal("101")));
    }
}
