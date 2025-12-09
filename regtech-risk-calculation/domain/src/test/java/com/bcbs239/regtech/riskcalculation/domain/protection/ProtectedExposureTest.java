package com.bcbs239.regtech.riskcalculation.domain.protection;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProtectedExposure aggregate root
 * Validates Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
class ProtectedExposureTest {
    
    private static final ExchangeRateProvider MOCK_RATE_PROVIDER = (from, to) -> 
        ExchangeRate.of(BigDecimal.valueOf(1.2), from, to, LocalDate.now());
    
    @Test
    void calculate_withNoMitigations_netExposureEqualsGross() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-001");
        EurAmount grossExposure = EurAmount.of(100000);
        List<Mitigation> mitigations = Collections.emptyList();
        
        // Act
        ProtectedExposure result = ProtectedExposure.calculate(exposureId, grossExposure, mitigations);
        
        // Assert - Validates Requirement 3.5
        assertEquals(grossExposure.value(), result.getNetExposure().value());
        assertEquals(grossExposure.value(), result.getGrossExposure().value());
        assertFalse(result.hasMitigations());
    }
    
    @Test
    void calculate_withMitigations_netExposureIsGrossMinusMitigations() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-002");
        EurAmount grossExposure = EurAmount.of(100000);
        
        Mitigation mitigation1 = Mitigation.create(
            MitigationType.FINANCIAL_COLLATERAL,
            BigDecimal.valueOf(20000),
            "EUR",
            MOCK_RATE_PROVIDER
        );
        
        Mitigation mitigation2 = Mitigation.create(
            MitigationType.GUARANTEE,
            BigDecimal.valueOf(30000),
            "EUR",
            MOCK_RATE_PROVIDER
        );
        
        List<Mitigation> mitigations = Arrays.asList(mitigation1, mitigation2);
        
        // Act
        ProtectedExposure result = ProtectedExposure.calculate(exposureId, grossExposure, mitigations);
        
        // Assert - Validates Requirement 3.3
        BigDecimal expectedNet = BigDecimal.valueOf(50000); // 100000 - 20000 - 30000
        assertEquals(0, expectedNet.compareTo(result.getNetExposure().value()));
        assertTrue(result.hasMitigations());
        assertEquals(2, result.getMitigations().size());
    }
    
    @Test
    void calculate_whenMitigationsExceedGross_netExposureIsZero() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-003");
        EurAmount grossExposure = EurAmount.of(50000);
        
        Mitigation mitigation = Mitigation.create(
            MitigationType.REAL_ESTATE,
            BigDecimal.valueOf(80000),
            "EUR",
            MOCK_RATE_PROVIDER
        );
        
        List<Mitigation> mitigations = Collections.singletonList(mitigation);
        
        // Act
        ProtectedExposure result = ProtectedExposure.calculate(exposureId, grossExposure, mitigations);
        
        // Assert - Validates Requirement 3.4: Net exposure cannot be negative
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getNetExposure().value()));
        assertTrue(result.isFullyCovered());
    }
    
    @Test
    void calculate_withNonEurMitigation_convertsToEur() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-004");
        EurAmount grossExposure = EurAmount.of(100000);
        
        // Mitigation in USD, rate is 1.2 USD to EUR
        Mitigation mitigation = Mitigation.create(
            MitigationType.FINANCIAL_COLLATERAL,
            BigDecimal.valueOf(12000), // USD
            "USD",
            MOCK_RATE_PROVIDER
        );
        
        List<Mitigation> mitigations = Collections.singletonList(mitigation);
        
        // Act
        ProtectedExposure result = ProtectedExposure.calculate(exposureId, grossExposure, mitigations);
        
        // Assert - Validates Requirement 3.2: Mitigation currency conversion
        // 12000 USD * 1.2 = 14400 EUR
        // Net = 100000 - 14400 = 85600
        BigDecimal expectedNet = BigDecimal.valueOf(85600);
        assertEquals(0, expectedNet.compareTo(result.getNetExposure().value()));
    }
    
    @Test
    void withoutMitigations_createsProtectedExposureWithNoMitigations() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-005");
        EurAmount grossExposure = EurAmount.of(75000);
        
        // Act
        ProtectedExposure result = ProtectedExposure.withoutMitigations(exposureId, grossExposure);
        
        // Assert
        assertEquals(grossExposure.value(), result.getNetExposure().value());
        assertFalse(result.hasMitigations());
        assertEquals(0, result.getMitigations().size());
    }
    
    @Test
    void getTotalMitigation_returnsCorrectSum() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-006");
        EurAmount grossExposure = EurAmount.of(100000);
        
        Mitigation mitigation1 = Mitigation.create(
            MitigationType.FINANCIAL_COLLATERAL,
            BigDecimal.valueOf(15000),
            "EUR",
            MOCK_RATE_PROVIDER
        );
        
        Mitigation mitigation2 = Mitigation.create(
            MitigationType.GUARANTEE,
            BigDecimal.valueOf(25000),
            "EUR",
            MOCK_RATE_PROVIDER
        );
        
        List<Mitigation> mitigations = Arrays.asList(mitigation1, mitigation2);
        
        // Act
        ProtectedExposure result = ProtectedExposure.calculate(exposureId, grossExposure, mitigations);
        
        // Assert
        BigDecimal expectedTotal = BigDecimal.valueOf(40000);
        assertEquals(0, expectedTotal.compareTo(result.getTotalMitigation().value()));
    }
    
    @Test
    void calculate_withNullExposureId_throwsException() {
        // Arrange
        EurAmount grossExposure = EurAmount.of(100000);
        List<Mitigation> mitigations = Collections.emptyList();
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            ProtectedExposure.calculate(null, grossExposure, mitigations)
        );
    }
    
    @Test
    void calculate_withNullGrossExposure_throwsException() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-007");
        List<Mitigation> mitigations = Collections.emptyList();
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            ProtectedExposure.calculate(exposureId, null, mitigations)
        );
    }
    
    @Test
    void calculate_withNullMitigationsList_throwsException() {
        // Arrange
        ExposureId exposureId = ExposureId.of("EXP-008");
        EurAmount grossExposure = EurAmount.of(100000);
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            ProtectedExposure.calculate(exposureId, grossExposure, null)
        );
    }
}
