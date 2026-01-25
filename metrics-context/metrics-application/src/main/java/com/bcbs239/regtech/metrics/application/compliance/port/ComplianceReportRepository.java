package com.bcbs239.regtech.metrics.application.compliance.port;

import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;

import java.time.LocalDate;
import java.util.List;

public interface ComplianceReportRepository {

    ComplianceReport save(ComplianceReport report);

    List<ComplianceReport> findRecentForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int limit);

    List<ComplianceReport> findForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int page, int size);

    int countForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd);
}
