package com.bcbs239.regtech.metrics.application.dashboard.port;

import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;

import java.util.List;

/**
 * Port interface for file persistence used by application layer.
 */
public interface FileRepository {
    List<ComplianceFile> findAll();

    List<ComplianceFile> findByBankId(BankId bankId);

    List<ComplianceFile> findByBankIdAndDateBetween(BankId bankId, String startDate, String endDate);

    List<ComplianceFile> findByBankIdAndDateBetween(BankId bankId, String startDate, String endDate, int page, int size);

    ComplianceFile save(ComplianceFile file);
}
