package com.bcbs239.regtech.riskcalculation.domain.exposure;

import java.util.Objects;
import java.util.Optional;

/**
 * Reference to a counterparty in an exposure
 * Contains identifying information about the counterparty
 * Immutable value object
 */
public record CounterpartyRef(
    String counterpartyId,
    String name,
    Optional<String> leiCode
) {

    private static final String LEI_PATTERN = "^[A-Za-z0-9]{20}$";
    
    public CounterpartyRef {
        Objects.requireNonNull(counterpartyId, "Counterparty ID cannot be null");
        Objects.requireNonNull(name, "Counterparty name cannot be null");
        Objects.requireNonNull(leiCode, "LEI code optional cannot be null");
        
        if (counterpartyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Counterparty ID cannot be empty");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Counterparty name cannot be empty");
        }

        // If provided, LEI must be exactly 20 alphanumeric characters.
        leiCode = leiCode
            .map(String::trim)
            .filter(v -> !v.isEmpty())
            .map(String::toUpperCase);

        if (leiCode.isPresent() && !leiCode.get().matches(LEI_PATTERN)) {
            throw new IllegalArgumentException("LEI code must be exactly 20 alphanumeric characters");
        }
    }
    
    public static CounterpartyRef of(String counterpartyId, String name, String leiCode) {
        return new CounterpartyRef(
            counterpartyId,
            name,
            leiCode != null && !leiCode.trim().isEmpty() ? Optional.of(leiCode) : Optional.empty()
        );
    }
    
    public static CounterpartyRef of(String counterpartyId, String name) {
        return new CounterpartyRef(counterpartyId, name, Optional.empty());
    }
}
