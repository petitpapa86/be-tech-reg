package com.bcbs239.regtech.ingestion.domain.bankinfo;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing bank information.
 */
public record BankInfo(
    BankId bankId,
    String bankName,
    String bankCountry,
    BankStatus bankStatus,
    Instant lastUpdated
) {
    
    public BankInfo {
        Objects.requireNonNull(bankId, "Bank ID cannot be null");
        Objects.requireNonNull(bankName, "Bank name cannot be null");
        Objects.requireNonNull(bankCountry, "Bank country cannot be null");
        Objects.requireNonNull(bankStatus, "Bank status cannot be null");
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");
        
        if (bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name cannot be empty");
        }
        if (bankCountry.length() != 3) {
            throw new IllegalArgumentException("Bank country must be a 3-character ISO code");
        }
    }
    
    /**
     * Checks if the bank information is fresh (less than 24 hours old).
     */
    public boolean isFresh() {
        return lastUpdated.isAfter(Instant.now().minusSeconds(24 * 60 * 60));
    }
    
    /**
     * Checks if the bank is active and can process uploads.
     */
    public boolean isActive() {
        return bankStatus == BankStatus.ACTIVE;
    }
    
    public enum BankStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}