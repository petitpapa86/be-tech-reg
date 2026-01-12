package com.bcbs239.regtech.dataquality.domain.validation.consistency;

/**
 * Defines the types of consistency checks performed on batch data.
 * These checks evaluate cross-field relationships and data uniformity.
 */
public enum ConsistencyCheckType {
    
    /**
     * Check 1: Exposure Count Verification
     * Verifies that declared total_exposures matches actual array length.
     */
    EXPOSURE_COUNT_MATCH("Conteggio Esposizioni", "Verifica che il conteggio dichiarato corrisponda all'effettivo"),
    
    /**
     * Check 2: CRM to Exposure Mapping
     * Verifies that every CRM reference corresponds to an existing exposure.
     */
    CRM_EXPOSURE_MAPPING("Mappatura CRM → Esposizioni", "Verifica che ogni riferimento CRM corrisponda a un'esposizione esistente"),
    
    /**
     * Check 3: LEI to CounterpartyId Relationship (1:1)
     * Verifies that same LEI always maps to same counterpartyId.
     */
    LEI_COUNTERPARTY_CONSISTENCY("Relazione LEI ↔ counterpartyId (1:1)", "Verifica che lo stesso LEI abbia sempre lo stesso counterpartyId"),
    
    /**
     * Check 4: Currency Consistency
     * Verifies currency consistency between exposures and CRM.
     */
    CURRENCY_CONSISTENCY("Coerenza Valutaria", "Verifica la coerenza delle valute tra esposizioni e CRM");

    private final String displayName;
    private final String description;

    ConsistencyCheckType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
