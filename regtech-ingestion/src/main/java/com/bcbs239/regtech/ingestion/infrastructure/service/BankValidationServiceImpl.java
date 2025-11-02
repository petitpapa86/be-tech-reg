package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.service.BankInfoEnrichmentService;
import com.bcbs239.regtech.ingestion.application.service.BankValidationService;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of BankValidationService.
 * Provides comprehensive bank validation including status and registration checks.
 */
@Service
public class BankValidationServiceImpl implements BankValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankValidationServiceImpl.class);
    
    private final BankInfoEnrichmentService bankInfoEnrichmentService;
    
    public BankValidationServiceImpl(BankInfoEnrichmentService bankInfoEnrichmentService) {
        this.bankInfoEnrichmentService = bankInfoEnrichmentService;
    }
    
    @Override
    public Result<Void> validateBankStatus(BankInfo bankInfo) {
        logger.debug("Validating bank status for bank ID: {}", bankInfo.bankId().value());
        
        switch (bankInfo.bankStatus()) {
            case ACTIVE:
                logger.debug("Bank status validation passed for bank ID: {}", bankInfo.bankId().value());
                return Result.success(null);
                
            case INACTIVE:
                String inactiveMessage = String.format(
                    "Bank %s (ID: %s) is INACTIVE and cannot process uploads. Please contact support to reactivate your account.", 
                    bankInfo.bankName(), bankInfo.bankId().value());
                logger.warn("Bank status validation failed - INACTIVE: {}", inactiveMessage);
                return Result.failure(ErrorDetail.of("BANK_INACTIVE", inactiveMessage));
                
            case SUSPENDED:
                String suspendedMessage = String.format(
                    "Bank %s (ID: %s) is SUSPENDED and cannot process uploads. Please contact support to resolve account issues.", 
                    bankInfo.bankName(), bankInfo.bankId().value());
                logger.warn("Bank status validation failed - SUSPENDED: {}", suspendedMessage);
                return Result.failure(ErrorDetail.of("BANK_SUSPENDED", suspendedMessage));
                
            default:
                String unknownMessage = String.format(
                    "Bank %s (ID: %s) has unknown status %s and cannot process uploads.", 
                    bankInfo.bankName(), bankInfo.bankId().value(), bankInfo.bankStatus());
                logger.error("Bank status validation failed - UNKNOWN STATUS: {}", unknownMessage);
                return Result.failure(ErrorDetail.of("BANK_UNKNOWN_STATUS", unknownMessage));
        }
    }
    
    @Override
    public Result<BankInfo> validateBankRegistration(BankId bankId) {
        logger.debug("Validating bank registration for bank ID: {}", bankId.value());
        
        // Use enrichment service to get bank information
        Result<BankInfo> enrichmentResult = bankInfoEnrichmentService.enrichBankInfo(bankId);
        
        if (enrichmentResult.isFailure()) {
            logger.warn("Bank registration validation failed for bank ID: {}", bankId.value());
            
            // Check if it's a "bank not found" error and provide more specific message
            if (enrichmentResult.getError().get().getCode().equals("BANK_NOT_FOUND")) {
                String message = String.format(
                    "Bank ID %s is not registered in the system. Please ensure your bank is properly registered before uploading files.", 
                    bankId.value());
                return Result.failure(ErrorDetail.of("BANK_NOT_REGISTERED", message));
            }
            
            // Return the original error for other cases (service unavailable, etc.)
            return enrichmentResult;
        }
        
        BankInfo bankInfo = enrichmentResult.getValue().get();
        logger.debug("Bank registration validation passed for bank ID: {} ({})", 
            bankId.value(), bankInfo.bankName());
        
        return Result.success(bankInfo);
    }
    
    @Override
    public Result<BankInfo> validateBankForUpload(BankId bankId) {
        logger.debug("Performing comprehensive bank validation for upload, bank ID: {}", bankId.value());
        
        // Step 1: Validate bank registration
        Result<BankInfo> registrationResult = validateBankRegistration(bankId);
        if (registrationResult.isFailure()) {
            return registrationResult;
        }
        
        BankInfo bankInfo = registrationResult.getValue().get();
        
        // Step 2: Validate bank status
        Result<Void> statusResult = validateBankStatus(bankInfo);
        if (statusResult.isFailure()) {
            return Result.failure(statusResult.getError().get());
        }
        
        logger.debug("Comprehensive bank validation passed for bank ID: {} ({})", 
            bankId.value(), bankInfo.bankName());
        
        return Result.success(bankInfo);
    }
}