package com.bcbs239.regtech.ingestion.application.service;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;

/**
 * Service interface for bank validation operations.
 * Handles bank status validation and registration checks.
 */
public interface BankValidationService {
    
    /**
     * Validate that a bank is active and can process uploads.
     * 
     * @param bankInfo the bank information to validate
     * @return Result indicating success or failure with error details
     */
    Result<Void> validateBankStatus(BankInfo bankInfo);
    
    /**
     * Validate bank registration before processing uploads.
     * This includes checking if the bank exists and is properly registered.
     * 
     * @param bankId the bank identifier to validate
     * @return Result containing BankInfo if valid, or error details if invalid
     */
    Result<BankInfo> validateBankRegistration(BankId bankId);
    
    /**
     * Comprehensive validation that combines registration and status checks.
     * 
     * @param bankId the bank identifier to validate
     * @return Result containing validated BankInfo if successful, or error details if failed
     */
    Result<BankInfo> validateBankForUpload(BankId bankId);
}