package com.bcbs239.regtech.metrics.infrastructure.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bcbs239.regtech.metrics.infrastructure.entity.ComplianceReportEntity;

public interface SpringDataComplianceReportRepository extends JpaRepository<ComplianceReportEntity, String>, JpaSpecificationExecutor<ComplianceReportEntity> {

    List<ComplianceReportEntity> findTop10ByBankIdAndReportingDateBetweenOrderByGeneratedAtDesc(
            String bankId,
            LocalDate start,
            LocalDate end
    );

    org.springframework.data.domain.Page<ComplianceReportEntity> findByBankIdAndReportingDateBetweenOrderByReportingDateDescGeneratedAtDesc(
            String bankId,
            LocalDate start,
            LocalDate end,
            org.springframework.data.domain.Pageable pageable
    );

    int countByBankIdAndReportingDateBetween(String bankId, LocalDate start, LocalDate end);
}
