package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.infrastructure.entity.ComplianceReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataComplianceReportRepository extends JpaRepository<ComplianceReportEntity, String> {

    List<ComplianceReportEntity> findTop10ByBankIdAndReportingDateBetweenOrderByGeneratedAtDesc(
            String bankId,
            LocalDate start,
            LocalDate end
    );

    int countByBankIdAndReportingDateBetween(String bankId, LocalDate start, LocalDate end);
}
