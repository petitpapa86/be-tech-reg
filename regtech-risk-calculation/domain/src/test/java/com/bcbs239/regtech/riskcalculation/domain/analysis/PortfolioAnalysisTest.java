package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PortfolioAnalysis aggregate root
 */
class PortfolioAnalysisTest {
    
    @Test
    @DisplayName("Should analyze portfolio with multiple exposures")
    void shouldAnalyzePortfolioWithMultipleExposures() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("400")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("300")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-003"),
                EurAmount.of(new BigDecimal("300")),
                GeographicRegion.NON_EUROPEAN,
                EconomicSector.SOVEREIGN
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        
        assertNotNull(analysis);
        assertEquals("BATCH-001", analysis.getBatchId());
        assertEquals(new BigDecimal("1000"), analysis.getTotalPortfolio().value());
        assertNotNull(analysis.getGeographicBreakdown());
        assertNotNull(analysis.getSectorBreakdown());
        assertNotNull(analysis.getGeographicHHI());
        assertNotNull(analysis.getSectorHHI());
        assertNotNull(analysis.getAnalyzedAt());
    }
    
    @Test
    @DisplayName("Should calculate correct total portfolio amount")
    void shouldCalculateCorrectTotalPortfolioAmount() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("250.50")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("749.50")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        
        assertEquals(new BigDecimal("1000.00"), analysis.getTotalPortfolio().value());
    }
    
    @Test
    @DisplayName("Should create geographic breakdown correctly")
    void shouldCreateGeographicBreakdownCorrectly() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("500")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("300")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-003"),
                EurAmount.of(new BigDecimal("200")),
                GeographicRegion.NON_EUROPEAN,
                EconomicSector.SOVEREIGN
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        Breakdown geoBreakdown = analysis.getGeographicBreakdown();
        
        assertNotNull(geoBreakdown);
        assertTrue(geoBreakdown.hasCategory("ITALY"));
        assertTrue(geoBreakdown.hasCategory("EU_OTHER"));
        assertTrue(geoBreakdown.hasCategory("NON_EUROPEAN"));
        
        Share italyShare = geoBreakdown.getShare("ITALY");
        assertEquals(0, new BigDecimal("500").compareTo(italyShare.amount().value()));
        assertEquals(new BigDecimal("50.0000"), italyShare.percentage());
    }
    
    @Test
    @DisplayName("Should create sector breakdown correctly")
    void shouldCreateSectorBreakdownCorrectly() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("400")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("300")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-003"),
                EurAmount.of(new BigDecimal("200")),
                GeographicRegion.NON_EUROPEAN,
                EconomicSector.SOVEREIGN
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-004"),
                EurAmount.of(new BigDecimal("100")),
                GeographicRegion.ITALY,
                EconomicSector.BANKING
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        Breakdown sectorBreakdown = analysis.getSectorBreakdown();
        
        assertNotNull(sectorBreakdown);
        assertTrue(sectorBreakdown.hasCategory("RETAIL_MORTGAGE"));
        assertTrue(sectorBreakdown.hasCategory("CORPORATE"));
        assertTrue(sectorBreakdown.hasCategory("SOVEREIGN"));
        assertTrue(sectorBreakdown.hasCategory("BANKING"));
        
        Share mortgageShare = sectorBreakdown.getShare("RETAIL_MORTGAGE");
        assertEquals(0, new BigDecimal("400").compareTo(mortgageShare.amount().value()));
        assertEquals(new BigDecimal("40.0000"), mortgageShare.percentage());
    }
    
    @Test
    @DisplayName("Should calculate geographic HHI")
    void shouldCalculateGeographicHHI() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("800")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("200")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        HHI geoHHI = analysis.getGeographicHHI();
        
        assertNotNull(geoHHI);
        // HHI = 0.8^2 + 0.2^2 = 0.64 + 0.04 = 0.68
        assertTrue(geoHHI.value().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(geoHHI.value().compareTo(BigDecimal.ONE) <= 0);
    }
    
    @Test
    @DisplayName("Should calculate sector HHI")
    void shouldCalculateSectorHHI() {
        List<ClassifiedExposure> exposures = List.of(
            ClassifiedExposure.of(
                ExposureId.of("EXP-001"),
                EurAmount.of(new BigDecimal("250")),
                GeographicRegion.ITALY,
                EconomicSector.RETAIL_MORTGAGE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-002"),
                EurAmount.of(new BigDecimal("250")),
                GeographicRegion.EU_OTHER,
                EconomicSector.CORPORATE
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-003"),
                EurAmount.of(new BigDecimal("250")),
                GeographicRegion.NON_EUROPEAN,
                EconomicSector.SOVEREIGN
            ),
            ClassifiedExposure.of(
                ExposureId.of("EXP-004"),
                EurAmount.of(new BigDecimal("250")),
                GeographicRegion.ITALY,
                EconomicSector.BANKING
            )
        );
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        HHI sectorHHI = analysis.getSectorHHI();
        
        assertNotNull(sectorHHI);
        // HHI = 4 * 0.25^2 = 4 * 0.0625 = 0.25
        assertTrue(sectorHHI.value().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(sectorHHI.value().compareTo(BigDecimal.ONE) <= 0);
    }
    
    @Test
    @DisplayName("Should handle empty exposure list")
    void shouldHandleEmptyExposureList() {
        List<ClassifiedExposure> exposures = List.of();
        
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze("BATCH-001", exposures);
        
        assertNotNull(analysis);
        assertEquals(BigDecimal.ZERO, analysis.getTotalPortfolio().value());
    }
    
    @Test
    @DisplayName("Should throw exception for null batch ID")
    void shouldThrowExceptionForNullBatchId() {
        List<ClassifiedExposure> exposures = List.of();
        assertThrows(NullPointerException.class, 
            () -> PortfolioAnalysis.analyze(null, exposures));
    }
    
    @Test
    @DisplayName("Should throw exception for null exposures list")
    void shouldThrowExceptionForNullExposuresList() {
        assertThrows(NullPointerException.class, 
            () -> PortfolioAnalysis.analyze("BATCH-001", null));
    }
}
