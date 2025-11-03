package com.bcbs239.regtech.ingestion.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler.BankInfoEnrichmentService;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo.BankStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Implementation of bank info enrichment service.
 * Enriches and validates bank information for processing batches.
 */
@Service
public class BankInfoEnrichmentServiceImpl implements BankInfoEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(BankInfoEnrichmentServiceImpl.class);

    @Override
    public Result<BankInfo> enrichBankInfo(BankId bankId) {
        log.debug("Enriching bank info for bankId: {}", bankId);

        if (bankId == null) {
            return Result.failure(new ErrorDetail("NULL_BANK_ID", "Bank ID cannot be null"));
        }

        // For now, create a basic BankInfo with the bankId
        // In a real implementation, this would fetch additional bank details from a database or external service
        BankInfo bankInfo = new BankInfo(
            bankId,
            "Sample Bank Name", // This would come from a database lookup
            "USA", // 3-character country code
            BankStatus.ACTIVE, // Status
            Instant.now() // Last updated timestamp
        );

        log.debug("Enriched bank info: {}", bankInfo);
        return Result.success(bankInfo);
    }

    @Override
    public Result<Void> validateBankStatus(BankInfo bankInfo) {
        log.debug("Validating bank status for: {}", bankInfo);

        if (bankInfo == null) {
            return Result.failure(new ErrorDetail("NULL_BANK_INFO", "Bank info cannot be null"));
        }

        // Check if bank is active using the isActive() method
        if (!bankInfo.isActive()) {
            return Result.failure(new ErrorDetail("INACTIVE_BANK",
                String.format("Bank status '%s' is not active", bankInfo.bankStatus())));
        }

        // Additional validation could include:
        // - Checking if bank is not suspended
        // - Verifying regulatory compliance status
        // - Confirming bank has necessary permissions

        log.debug("Bank status validation passed for bank: {}", bankInfo.bankId());
        return Result.success(null);
    }
}