package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.service.BankInfoEnrichmentService;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BankInfo;
import com.bcbs239.regtech.ingestion.domain.repository.BankInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of BankInfoEnrichmentService.
 * Manages bank information retrieval from database with freshness validation.
 */
@Service
@Transactional
public class BankInfoEnrichmentServiceImpl implements BankInfoEnrichmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankInfoEnrichmentServiceImpl.class);
    
    private final BankInfoRepository bankInfoRepository;
    
    public BankInfoEnrichmentServiceImpl(BankInfoRepository bankInfoRepository) {
        this.bankInfoRepository = bankInfoRepository;
    }
    
    @Override
    public Result<BankInfo> enrichBankInfo(BankId bankId) {
        logger.debug("Enriching bank information for bank ID: {}", bankId.value());
        
        // Step 1: Check for fresh bank data (< 24 hours)
        Optional<BankInfo> freshBankInfo = bankInfoRepository.findFreshBankInfo(bankId);
        if (freshBankInfo.isPresent()) {
            logger.debug("Found fresh bank information for bank ID: {}", bankId.value());
            return Result.success(freshBankInfo.get());
        }
        
        // Step 2: Check for any bank data (even if stale)
        Optional<BankInfo> bankInfo = bankInfoRepository.findByBankId(bankId);
        if (bankInfo.isPresent()) {
            logger.debug("Found bank information for bank ID: {} (may be stale)", bankId.value());
            return Result.success(bankInfo.get());
        }
        
        // Step 3: No bank information found
        logger.warn("No bank information found for bank ID: {}", bankId.value());
        String message = String.format("Bank ID %s is not registered in the system", bankId.value());
        return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", message));
    }
    
    @Override
    public Result<Void> validateBankStatus(BankInfo bankInfo) {
        logger.debug("Validating bank status for bank ID: {}", bankInfo.bankId().value());
        
        if (!bankInfo.isActive()) {
            String message = String.format("Bank %s (ID: %s) has status %s and cannot process uploads", 
                bankInfo.bankName(), bankInfo.bankId().value(), bankInfo.bankStatus());
            
            logger.warn("Bank status validation failed: {}", message);
            
            return Result.failure(ErrorDetail.of("BANK_INACTIVE", message));
        }
        
        logger.debug("Bank status validation passed for bank ID: {}", bankInfo.bankId().value());
        return Result.success(null);
    }
    
    @Override
    public Result<BankInfo> refreshBankInfo(BankId bankId) {
        logger.debug("Refreshing bank information for bank ID: {}", bankId.value());
        
        // Since we're working with database only, this is the same as enrichBankInfo
        // In the future, this could trigger an external refresh process
        return enrichBankInfo(bankId);
    }
}