package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Repository Port: Risk Parameters Repository
 * 
 * Domain interface for risk parameters persistence.
 * Implementation in infrastructure layer.
 */
public interface RiskParametersRepository {
    
    /**
     * Save or update risk parameters
     */
    @NonNull
    RiskParameters save(@NonNull RiskParameters parameters);
    
    /**
     * Find risk parameters by ID
     */
    @NonNull
    Optional<RiskParameters> findById(@NonNull RiskParametersId id);
    
    /**
     * Find risk parameters by bank ID
     */
    @NonNull
    Optional<RiskParameters> findByBankId(@NonNull String bankId);
    
    /**
     * Check if risk parameters exist for bank
     */
    boolean existsByBankId(@NonNull String bankId);
}
