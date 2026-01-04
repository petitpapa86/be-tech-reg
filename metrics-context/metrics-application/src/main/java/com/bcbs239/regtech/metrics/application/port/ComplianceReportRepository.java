package com.bcbs239.regtech.metrics.application.port;

import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;

import java.time.LocalDate;
import java.util.List;

/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.compliance.port}.
 */
@Deprecated(forRemoval = true)
public interface ComplianceReportRepository {

    ComplianceReport save(ComplianceReport report);

    List<ComplianceReport> findRecentForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int limit);

    int countForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd);
}
