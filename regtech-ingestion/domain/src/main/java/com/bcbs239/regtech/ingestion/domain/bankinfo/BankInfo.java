package com.bcbs239.regtech.ingestion.domain.bankinfo;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing bank information with business logic.
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
    
    /**
     * Checks if the bank is suspended.
     */
    public boolean isSuspended() {
        return bankStatus == BankStatus.SUSPENDED;
    }
    
    /**
     * Validates if the bank is eligible for processing.
     * A bank is eligible if it's active and not suspended.
     * 
     * @return Result with success if eligible, failure with error details otherwise
     */
    public Result<Void> validateEligibilityForProcessing() {
        if (bankStatus == BankStatus.INACTIVE) {
            return Result.failure(ErrorDetail.of(
                "INACTIVE_BANK",
                ErrorType.BUSINESS_RULE_ERROR,
                String.format("Bank '%s' is inactive and cannot process uploads", bankId.value()),
                "bank.inactive"
            ));
        }
        
        if (bankStatus == BankStatus.SUSPENDED) {
            return Result.failure(ErrorDetail.of(
                "SUSPENDED_BANK",
                ErrorType.BUSINESS_RULE_ERROR,
                String.format("Bank '%s' is suspended and cannot process uploads", bankId.value()),
                "bank.suspended"
            ));
        }
        
        if (bankStatus == BankStatus.NON_COMPLIANT) {
            return Result.failure(ErrorDetail.of(
                "NON_COMPLIANT_BANK",
                ErrorType.BUSINESS_RULE_ERROR,
                String.format("Bank '%s' is not in regulatory compliance", bankId.value()),
                "bank.non.compliant"
            ));
        }
        
        if (!isFresh()) {
            // Warning: stale data, but allow processing
            // In production, this might trigger a refresh event
            return Result.success(null);
        }
        
        return Result.success(null);
    }
    
    /**
     * Create a new BankInfo with updated status.
     * This supports event-driven updates to bank status.
     */
    public BankInfo withStatus(BankStatus newStatus) {
        return new BankInfo(
            this.bankId,
            this.bankName,
            this.bankCountry,
            newStatus,
            Instant.now() // Update timestamp when status changes
        );
    }
    
    /**
     * Create a new BankInfo with refreshed timestamp.
     */
    public BankInfo withRefreshedTimestamp() {
        return new BankInfo(
            this.bankId,
            this.bankName,
            this.bankCountry,
            this.bankStatus,
            Instant.now()
        );
    }
    
    public enum BankStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        NON_COMPLIANT // Added for regulatory compliance status
    }
}

