package com.bcbs239.regtech.riskcalculation.domain.exposure;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for Exposure Recording bounded context
 * Captures immutable facts about financial instruments
 * Represents a generic financial exposure (loan, bond, derivative, guarantee, etc.)
 */
public class ExposureRecording {
    
    private final ExposureId id;
    private final InstrumentId instrumentId;
    private final CounterpartyRef counterparty;
    private final MonetaryAmount exposureAmount;
    private final ExposureClassification classification;
    private final Instant recordedAt;
    
    public ExposureRecording(
            ExposureId id,
            InstrumentId instrumentId,
            CounterpartyRef counterparty,
            MonetaryAmount exposureAmount,
            ExposureClassification classification,
            Instant recordedAt
    ) {
        this.id = Objects.requireNonNull(id, "Exposure ID cannot be null");
        this.instrumentId = Objects.requireNonNull(instrumentId, "Instrument ID cannot be null");
        this.counterparty = Objects.requireNonNull(counterparty, "Counterparty cannot be null");
        this.exposureAmount = Objects.requireNonNull(exposureAmount, "Exposure amount cannot be null");
        this.classification = Objects.requireNonNull(classification, "Classification cannot be null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "Recorded timestamp cannot be null");
    }
    
    /**
     * Factory method to create a new exposure recording
     */
    public static ExposureRecording create(
        ExposureId id,
        InstrumentId instrumentId,
        CounterpartyRef counterparty,
        MonetaryAmount exposureAmount,
        ExposureClassification classification
    ) {
        return new ExposureRecording(
            id,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            Instant.now()
        );
    }
    
    /**
     * Factory method to reconstitute an exposure recording from persistence
     */
    public static ExposureRecording reconstitute(
        ExposureId id,
        InstrumentId instrumentId,
        CounterpartyRef counterparty,
        MonetaryAmount exposureAmount,
        ExposureClassification classification,
        Instant recordedAt
    ) {
        return new ExposureRecording(
            id,
            instrumentId,
            counterparty,
            exposureAmount,
            classification,
            recordedAt
        );
    }
    
    // Getters only - immutable aggregate
    
    public ExposureId getId() {
        return id;
    }
    
    public InstrumentId getInstrumentId() {
        return instrumentId;
    }
    
    public CounterpartyRef getCounterparty() {
        return counterparty;
    }
    
    public MonetaryAmount getExposureAmount() {
        return exposureAmount;
    }
    
    public ExposureClassification getClassification() {
        return classification;
    }
    
    public Instant getRecordedAt() {
        return recordedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExposureRecording that = (ExposureRecording) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ExposureRecording{" +
                "id=" + id +
                ", instrumentId=" + instrumentId +
                ", counterparty=" + counterparty +
                ", exposureAmount=" + exposureAmount +
                ", classification=" + classification +
                ", recordedAt=" + recordedAt +
                '}';
    }
}
