package com.bcbs239.regtech.metrics.application.compliance.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;

public interface ComplianceReportRepository {

    ComplianceReport save(ComplianceReport report);

    Optional<ComplianceReport> findById(String reportId);

    List<ComplianceReport> findRecentForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int limit);

    List<ComplianceReport> findForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int page, int size);

    int countForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd);

    List<ComplianceReport> findAllWithFilters(String name, LocalDate generatedAt, LocalDate reportingDate, String status, int page, int size);

    int countAllWithFilters(String name, LocalDate generatedAt, LocalDate reportingDate, String status);
}
