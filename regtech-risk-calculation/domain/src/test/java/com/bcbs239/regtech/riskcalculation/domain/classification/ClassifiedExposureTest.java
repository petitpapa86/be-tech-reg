package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClassifiedExposure value object
 */
class ClassifiedExposureTest {
    
    @Test
    @DisplayName("Should create valid ClassifiedExposure")
    void shouldCreateValidClassifiedExposure() {
        ExposureId exposureId = ExposureId.of("EXP-001");
        EurAmount netExposure = EurAmount.of(BigDecimal.valueOf(100000));
        GeographicRegion region = GeographicRegion.ITALY;
        EconomicSector sector = EconomicSector.CORPORATE;
        
        ClassifiedExposure classified = new ClassifiedExposure(
            exposureId,
            netExposure,
            region,
            sector
        );
        
        assertNotNull(classified);
        assertEquals(exposureId, classified.exposureId());
        assertEquals(netExposure, classified.netExposure());
        assertEquals(region, classified.region());
        assertEquals(sector, classified.sector());
    }
    
    @Test
    @DisplayName("Should create ClassifiedExposure using factory method")
    void shouldCreateUsingFactoryMethod() {
        ExposureId exposureId = ExposureId.of("EXP-002");
        EurAmount netExposure = EurAmount.of(BigDecimal.valueOf(50000));
        GeographicRegion region = GeographicRegion.EU_OTHER;
        EconomicSector sector = EconomicSector.RETAIL_MORTGAGE;
        
        ClassifiedExposure classified = ClassifiedExposure.of(
            exposureId,
            netExposure,
            region,
            sector
        );
        
        assertNotNull(classified);
        assertEquals(exposureId, classified.exposureId());
        assertEquals(netExposure, classified.netExposure());
        assertEquals(region, classified.region());
        assertEquals(sector, classified.sector());
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null exposureId")
    void shouldThrowExceptionForNullExposureId() {
        assertThrows(NullPointerException.class, () -> 
            new ClassifiedExposure(
                null,
                EurAmount.of(BigDecimal.valueOf(100000)),
                GeographicRegion.ITALY,
                EconomicSector.CORPORATE
            )
        );
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null netExposure")
    void shouldThrowExceptionForNullNetExposure() {
        assertThrows(NullPointerException.class, () -> 
            new ClassifiedExposure(
                ExposureId.of("EXP-001"),
                null,
                GeographicRegion.ITALY,
                EconomicSector.CORPORATE
            )
        );
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null region")
    void shouldThrowExceptionForNullRegion() {
        assertThrows(NullPointerException.class, () -> 
            new ClassifiedExposure(
                ExposureId.of("EXP-001"),
                EurAmount.of(BigDecimal.valueOf(100000)),
                null,
                EconomicSector.CORPORATE
            )
        );
    }
    
    @Test
    @DisplayName("Should throw NullPointerException for null sector")
    void shouldThrowExceptionForNullSector() {
        assertThrows(NullPointerException.class, () -> 
            new ClassifiedExposure(
                ExposureId.of("EXP-001"),
                EurAmount.of(BigDecimal.valueOf(100000)),
                GeographicRegion.ITALY,
                null
            )
        );
    }
    
    @Test
    @DisplayName("Should support equality based on all fields")
    void shouldSupportEquality() {
        ExposureId exposureId = ExposureId.of("EXP-001");
        EurAmount netExposure = EurAmount.of(BigDecimal.valueOf(100000));
        GeographicRegion region = GeographicRegion.ITALY;
        EconomicSector sector = EconomicSector.CORPORATE;
        
        ClassifiedExposure classified1 = new ClassifiedExposure(
            exposureId, netExposure, region, sector
        );
        ClassifiedExposure classified2 = new ClassifiedExposure(
            exposureId, netExposure, region, sector
        );
        
        assertEquals(classified1, classified2);
        assertEquals(classified1.hashCode(), classified2.hashCode());
    }
    
    @Test
    @DisplayName("Should handle zero net exposure")
    void shouldHandleZeroNetExposure() {
        ClassifiedExposure classified = ClassifiedExposure.of(
            ExposureId.of("EXP-003"),
            EurAmount.zero(),
            GeographicRegion.NON_EUROPEAN,
            EconomicSector.BANKING
        );
        
        assertNotNull(classified);
        assertEquals(BigDecimal.ZERO, classified.netExposure().value());
    }
}
