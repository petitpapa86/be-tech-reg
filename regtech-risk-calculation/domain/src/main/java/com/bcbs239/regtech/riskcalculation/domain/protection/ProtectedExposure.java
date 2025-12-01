package com.bcbs239.regtech.riskcalculation.domain.protection;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for Credit Protection bounded context
 * Represents an exposure with applied credit risk mitigations
 * 
 * Calculates net exposure by subtracting total mitigation value from gross exposure
 * Ensures net exposure cannot be negative (floor at zero)
 */
@Getter
public class ProtectedExposure {
    
    private final ExposureId exposureId;
    private final EurAmount grossExposure;
    private final List<Mitigation> mitigations;
    private final EurAmount netExposure;
    
    /**
     * Private constructor - use factory method
     */
    private ProtectedExposure(
        ExposureId exposureId,
        EurAmount grossExposure,
        List<Mitigation> mitigations,
        EurAmount netExposure
    ) {
        this.exposureId = Objects.requireNonNull(exposureId, "Exposure ID cannot be null");
        this.grossExposure = Objects.requireNonNull(grossExposure, "Gross exposure cannot be null");
        this.mitigations = Collections.unmodifiableList(
            Objects.requireNonNull(mitigations, "Mitigations list cannot be null")
        );
        this.netExposure = Objects.requireNonNull(netExposure, "Net exposure cannot be null");
    }
    
    /**
     * Factory method to calculate protected exposure
     * 
     * Calculates net exposure as: max(grossExposure - totalMitigations, 0)
     * Net exposure cannot be negative per regulatory requirements
     * 
     * @param exposureId The unique identifier for the exposure
     * @param grossExposure The gross exposure amount in EUR
     * @param mitigations List of mitigations to apply (already in EUR)
     * @return A new ProtectedExposure with calculated net exposure
     */
    public static ProtectedExposure calculate(
        ExposureId exposureId,
        EurAmount grossExposure,
        List<Mitigation> mitigations
    ) {
        Objects.requireNonNull(exposureId, "Exposure ID cannot be null");
        Objects.requireNonNull(grossExposure, "Gross exposure cannot be null");
        Objects.requireNonNull(mitigations, "Mitigations list cannot be null");
        
        // Calculate total mitigation value
        BigDecimal totalMitigation = mitigations.stream()
            .map(m -> m.getEurValue().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate net exposure: max(gross - mitigations, 0)
        // Net exposure cannot be negative per requirements 3.4
        BigDecimal net = grossExposure.value()
            .subtract(totalMitigation)
            .max(BigDecimal.ZERO);
        
        EurAmount netExposure = EurAmount.of(net);
        
        return new ProtectedExposure(
            exposureId,
            grossExposure,
            mitigations,
            netExposure
        );
    }
    
    /**
     * Factory method for exposure with no mitigations
     * Net exposure equals gross exposure
     */
    public static ProtectedExposure withoutMitigations(
        ExposureId exposureId,
        EurAmount grossExposure
    ) {
        return calculate(exposureId, grossExposure, Collections.emptyList());
    }
    
    // Getters only - immutable aggregate

    /**
     * Returns the total mitigation value in EUR
     */
    public EurAmount getTotalMitigation() {
        BigDecimal total = mitigations.stream()
            .map(m -> m.getEurValue().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return EurAmount.of(total);
    }
    
    /**
     * Checks if this exposure has any mitigations applied
     */
    public boolean hasMitigations() {
        return !mitigations.isEmpty();
    }
    
    /**
     * Checks if mitigations fully cover the gross exposure
     */
    public boolean isFullyCovered() {
        return netExposure.value().compareTo(BigDecimal.ZERO) == 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtectedExposure that = (ProtectedExposure) o;
        return Objects.equals(exposureId, that.exposureId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(exposureId);
    }
    
    @Override
    public String toString() {
        return "ProtectedExposure{" +
                "exposureId=" + exposureId +
                ", grossExposure=" + grossExposure +
                ", mitigations=" + mitigations.size() + " items" +
                ", netExposure=" + netExposure +
                '}';
    }
}
