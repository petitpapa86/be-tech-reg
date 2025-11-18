package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.SectorCategory;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import lombok.Getter;

import java.util.Objects;

/**
 * Individual exposure with calculated risk metrics
 * Entity representing a single exposure record with all calculated values
 */
@Getter
public class CalculatedExposure extends Entity {
    
    private final ExposureId exposureId;
    private final ClientName clientName;
    private final OriginalAmount originalAmount;
    private final OriginalCurrency originalCurrency;
    private AmountEur amountEur;
    private ExchangeRate exchangeRateUsed;
    private final Country country;
    private GeographicRegion geographicRegion;
    private final Sector sector;
    private SectorCategory sectorCategory;
    private PercentageOfTotal percentageOfTotal;
    
    /**
     * Constructor for creating a new calculated exposure
     */
    public CalculatedExposure(ExposureId exposureId, ClientName clientName,
                            OriginalAmount originalAmount, OriginalCurrency originalCurrency,
                            Country country, Sector sector) {
        this.exposureId = Objects.requireNonNull(exposureId, "ExposureId cannot be null");
        this.clientName = Objects.requireNonNull(clientName, "ClientName cannot be null");
        this.originalAmount = Objects.requireNonNull(originalAmount, "OriginalAmount cannot be null");
        this.originalCurrency = Objects.requireNonNull(originalCurrency, "OriginalCurrency cannot be null");
        this.country = Objects.requireNonNull(country, "Country cannot be null");
        this.sector = Objects.requireNonNull(sector, "Sector cannot be null");
    }
    
    /**
     * Constructor for reconstituting from persistence
     */
    public CalculatedExposure(ExposureId exposureId, ClientName clientName,
                            OriginalAmount originalAmount, OriginalCurrency originalCurrency,
                            AmountEur amountEur, ExchangeRate exchangeRateUsed,
                            Country country, GeographicRegion geographicRegion,
                            Sector sector, SectorCategory sectorCategory,
                            PercentageOfTotal percentageOfTotal) {
        this.exposureId = exposureId;
        this.clientName = clientName;
        this.originalAmount = originalAmount;
        this.originalCurrency = originalCurrency;
        this.amountEur = amountEur;
        this.exchangeRateUsed = exchangeRateUsed;
        this.country = country;
        this.geographicRegion = geographicRegion;
        this.sector = sector;
        this.sectorCategory = sectorCategory;
        this.percentageOfTotal = percentageOfTotal;
    }
    
    /**
     * Convert currency to EUR using exchange rate
     * DDD: Ask the object to do the work
     */
    public void convertCurrency(ExchangeRate exchangeRate) {
        Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null");
        
        if (originalCurrency.isEur()) {
            // No conversion needed for EUR
            this.amountEur = AmountEur.of(originalAmount.value());
            this.exchangeRateUsed = null;
        } else {
            // Convert using provided exchange rate
            this.amountEur = exchangeRate.convert(originalAmount.value());
            this.exchangeRateUsed = exchangeRate;
        }
    }
    
    /**
     * Apply geographic and sector classification
     * DDD: Ask the object to do the work
     */
    public void classify(GeographicRegion geographicRegion, SectorCategory sectorCategory) {
        this.geographicRegion = Objects.requireNonNull(geographicRegion, "Geographic region cannot be null");
        this.sectorCategory = Objects.requireNonNull(sectorCategory, "Sector category cannot be null");
    }
    
    /**
     * Set geographic region classification
     */
    public void setGeographicRegion(GeographicRegion geographicRegion) {
        this.geographicRegion = Objects.requireNonNull(geographicRegion, "Geographic region cannot be null");
    }
    
    /**
     * Set sector category classification
     */
    public void setSectorCategory(SectorCategory sectorCategory) {
        this.sectorCategory = Objects.requireNonNull(sectorCategory, "Sector category cannot be null");
    }
    
    /**
     * Calculate percentage of total portfolio
     * DDD: Ask the object to do the work
     */
    public void calculatePercentage(TotalAmountEur totalPortfolio) {
        Objects.requireNonNull(totalPortfolio, "Total portfolio cannot be null");
        Objects.requireNonNull(amountEur, "Amount EUR must be set before calculating percentage");
        
        this.percentageOfTotal = PercentageOfTotal.calculate(amountEur, totalPortfolio);
    }
    
    /**
     * Check if currency conversion has been applied
     */
    public boolean isCurrencyConverted() {
        return amountEur != null;
    }
    
    /**
     * Check if classification has been applied
     */
    public boolean isClassified() {
        return geographicRegion != null && sectorCategory != null;
    }
    
    /**
     * Check if percentage calculation has been applied
     */
    public boolean isPercentageCalculated() {
        return percentageOfTotal != null;
    }
    
    public ExposureId getId() {
        return exposureId;
    }
}