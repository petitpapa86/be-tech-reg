package com.bcbs239.regtech.riskcalculation.domain.protection;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Mitigation entity
 * Validates Requirements 3.2 (mitigation currency conversion)
 */
class MitigationTest {
    
    private static final ExchangeRateProvider MOCK_RATE_PROVIDER = (from, to) -> 
        ExchangeRate.of(BigDecimal.valueOf(1.2), from, to, LocalDate.now());
    
    @Test
    void create_withEurCurrency_noConversionNeeded() {
        // Arrange
        MitigationType type = MitigationType.FINANCIAL_COLLATERAL;
        BigDecimal value = BigDecimal.valueOf(50000);
        String currency = "EUR";
        
        // Act
        Mitigation mitigation = Mitigation.create(type, value, currency, MOCK_RATE_PROVIDER);
        
        // Assert
        assertEquals(type, mitigation.getType());
        assertEquals(0, value.compareTo(mitigation.getEurValue().value()));
    }
    
    @Test
    void create_withNonEurCurrency_convertsToEur() {
        // Arrange
        MitigationType type = MitigationType.GUARANTEE;
        BigDecimal value = BigDecimal.valueOf(10000); // USD
        String currency = "USD";
        
        // Act
        Mitigation mitigation = Mitigation.create(type, value, currency, MOCK_RATE_PROVIDER);
        
        // Assert - Validates Requirement 3.2
        // 10000 USD * 1.2 = 12000 EUR
        BigDecimal expectedEur = BigDecimal.valueOf(12000);
        assertEquals(type, mitigation.getType());
        assertEquals(0, expectedEur.compareTo(mitigation.getEurValue().value()));
    }
    
    @Test
    void fromRawData_createsCorrectMitigation() {
        // Arrange
        RawMitigationData rawData = RawMitigationData.of(
            MitigationType.PHYSICAL_ASSET,
            BigDecimal.valueOf(25000),
            "EUR"
        );
        
        // Act
        Mitigation mitigation = Mitigation.fromRawData(rawData, MOCK_RATE_PROVIDER);
        
        // Assert
        assertEquals(MitigationType.PHYSICAL_ASSET, mitigation.getType());
        assertEquals(0, BigDecimal.valueOf(25000).compareTo(mitigation.getEurValue().value()));
    }
    
    @Test
    void reconstitute_createsCorrectMitigation() {
        // Arrange
        MitigationType type = MitigationType.REAL_ESTATE;
        EurAmount eurValue = EurAmount.of(75000);
        
        // Act
        Mitigation mitigation = Mitigation.reconstitute(type, eurValue);
        
        // Assert
        assertEquals(type, mitigation.getType());
        assertEquals(eurValue, mitigation.getEurValue());
    }
    
    @Test
    void create_withNullType_throwsException() {
        // Arrange
        BigDecimal value = BigDecimal.valueOf(10000);
        String currency = "EUR";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            Mitigation.create(null, value, currency, MOCK_RATE_PROVIDER)
        );
    }
    
    @Test
    void create_withNullValue_throwsException() {
        // Arrange
        MitigationType type = MitigationType.FINANCIAL_COLLATERAL;
        String currency = "EUR";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            Mitigation.create(type, null, currency, MOCK_RATE_PROVIDER)
        );
    }
    
    @Test
    void create_withNegativeValue_throwsException() {
        // Arrange
        MitigationType type = MitigationType.GUARANTEE;
        BigDecimal value = BigDecimal.valueOf(-1000);
        String currency = "EUR";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            Mitigation.create(type, value, currency, MOCK_RATE_PROVIDER)
        );
    }
    
    @Test
    void create_withNullCurrency_throwsException() {
        // Arrange
        MitigationType type = MitigationType.PHYSICAL_ASSET;
        BigDecimal value = BigDecimal.valueOf(10000);
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            Mitigation.create(type, value, null, MOCK_RATE_PROVIDER)
        );
    }
    
    @Test
    void create_withNullRateProvider_throwsException() {
        // Arrange
        MitigationType type = MitigationType.REAL_ESTATE;
        BigDecimal value = BigDecimal.valueOf(10000);
        String currency = "USD";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            Mitigation.create(type, value, currency, null)
        );
    }
}
