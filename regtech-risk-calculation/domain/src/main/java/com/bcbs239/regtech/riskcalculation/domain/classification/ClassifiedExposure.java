package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;

import java.util.Objects;

/**
 * Value object representing an exposure that has been classified by geographic region and economic sector
 * Part of the Classification Service bounded context
 * 
 * This is the output of the classification process, containing:
 * - The exposure identifier
 * - The net exposure amount in EUR (after mitigations)
 * - The geographic region classification
 * - The economic sector classification
 */
public record ClassifiedExposure(
    ExposureId exposureId,
    EurAmount netExposure,
    GeographicRegion region,
    EconomicSector sector
) {
    
    public ClassifiedExposure {
        Objects.requireNonNull(exposureId, "Exposure ID cannot be null");
        Objects.requireNonNull(netExposure, "Net exposure cannot be null");
        Objects.requireNonNull(region, "Geographic region cannot be null");
        Objects.requireNonNull(sector, "Economic sector cannot be null");
    }
    
    /**
     * Factory method to create a classified exposure
     */
    public static ClassifiedExposure of(
        ExposureId exposureId,
        EurAmount netExposure,
        GeographicRegion region,
        EconomicSector sector
    ) {
        return new ClassifiedExposure(exposureId, netExposure, region, sector);
    }
}
