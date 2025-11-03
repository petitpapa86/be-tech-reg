package com.bcbs239.regtech.modules.ingestion.application.batch;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;

/**
 * Application-level abstraction for security checks in the ingestion module.
 * Implementations live in the infrastructure layer (e.g. IngestionSecurityService implemented there).
 */
public interface IngestionSecurityService {

    Result<BankId> validateTokenAndExtractBankId(String token);

    Result<Void> verifyIngestionPermissions(String operation);

    Result<Void> verifyBatchAccess(BatchId batchId, BankId bankId);
}

