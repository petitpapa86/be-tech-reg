package com.bcbs239.regtech.metrics.application.port;

import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.BankId;
import java.util.List;

/**
 * Port interface for file persistence used by application layer.
 */
/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.dashboard.port}.
 */
@Deprecated(forRemoval = true)
public interface FileRepository {
    List<ComplianceFile> findAll();
    List<ComplianceFile> findByBankId(BankId bankId);
    List<ComplianceFile> findByBankIdAndDateBetween(BankId bankId, String startDate, String endDate);
    ComplianceFile save(ComplianceFile file);
}
