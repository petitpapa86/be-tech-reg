package com.bcbs239.regtech.riskcalculation.domain.exposure;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for Exposure Recording bounded context
 * Captures immutable facts about financial instruments
 * Represents a generic financial exposure (loan, bond, derivative, guarantee, etc.)
 */
public record ExposureRecording(ExposureId id, InstrumentId instrumentId, CounterpartyRef counterparty,
                                MonetaryAmount exposureAmount, ExposureClassification classification,
                                Instant recordedAt) {

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

    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from external data.
     * 
     * @param dto The ExposureDTO from inter-module communication
     * @return A new ExposureRecording with current timestamp
     */
    public static ExposureRecording fromDTO(ExposureDTO dto) {
        Objects.requireNonNull(dto, "ExposureDTO cannot be null");
        
        // Parse instrument type enum, default to OTHER if not recognized
        InstrumentType instrumentType;
        try {
            instrumentType = InstrumentType.valueOf(dto.instrumentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            instrumentType = InstrumentType.OTHER;
        }
        
        // Parse balance sheet type enum, default to ON_BALANCE if not recognized
        BalanceSheetType balanceSheetType;
        try {
            balanceSheetType = BalanceSheetType.valueOf(dto.balanceSheetType().toUpperCase());
        } catch (IllegalArgumentException e) {
            balanceSheetType = BalanceSheetType.ON_BALANCE;
        }
        
        return create(
            ExposureId.of(dto.exposureId()),
            InstrumentId.of(dto.instrumentId()),
            CounterpartyRef.of(dto.counterpartyId(), dto.counterpartyName(), dto.counterpartyLei()),
            MonetaryAmount.of(dto.exposureAmount(), dto.currency()),
            ExposureClassification.of(
                dto.productType(),
                instrumentType,
                balanceSheetType,
                dto.countryCode()
            )
        );
    }

    // Getters only - immutable aggregate

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
