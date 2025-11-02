package com.bcbs239.regtech.ingestion.application.service;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;

/**
 * Service interface for enriching bank information.
 * Manages bank information retrieval from database with freshness validation.
 */
public interface BankInfoEnrichmentService {
    
    /**
     * Enrich bank information by checking database for existing data with freshness validation.
     * 
     * @param bankId the bank identifier
     * @return Result containing BankInfo if successful, or error details if failed
     */
    Result<BankInfo> enrichBankInfo(BankId bankId);
    
    /**
     * Validate that a bank is active and can process uploads.
     * 
     * @param bankInfo the bank information to validate
     * @return Result indicating success or failure with error details
     */
    Result<Void> validateBankStatus(BankInfo bankInfo);
    
    /**
     * Refresh bank information from database.
     * Currently equivalent to enrichBankInfo, but reserved for future external refresh capabilities.
     * 
     * @param bankId the bank identifier
     * @return Result containing BankInfo if successful, or error details if failed
     */
    Result<BankInfo> refreshBankInfo(BankId bankId);
}