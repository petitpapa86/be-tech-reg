package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.application.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import com.bcbs239.regtech.metrics.infrastructure.entity.ComplianceReportEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class JpaComplianceReportRepository implements ComplianceReportRepository {

    private final SpringDataComplianceReportRepository springDataRepository;

    public JpaComplianceReportRepository(SpringDataComplianceReportRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ComplianceReport save(ComplianceReport report) {
        ComplianceReportEntity entity = new ComplianceReportEntity();
        entity.setReportId(report.getReportId());
        entity.setBatchId(report.getBatchId());
        entity.setBankId(report.getBankId().getValue());
        entity.setReportType(report.getReportType());
        entity.setReportingDate(report.getReportingDate());
        entity.setStatus(report.getStatus());
        entity.setGeneratedAt(report.getGeneratedAt());
        entity.setHtmlS3Uri(report.getHtmlS3Uri());
        entity.setXbrlS3Uri(report.getXbrlS3Uri());
        entity.setHtmlPresignedUrl(report.getHtmlPresignedUrl());
        entity.setXbrlPresignedUrl(report.getXbrlPresignedUrl());
        entity.setHtmlFileSize(report.getHtmlFileSize());
        entity.setXbrlFileSize(report.getXbrlFileSize());
        entity.setOverallQualityScore(report.getOverallQualityScore());
        entity.setComplianceStatus(report.getComplianceStatus());
        entity.setGenerationDurationMillis(report.getGenerationDurationMillis());

        Instant now = Instant.now();
        ComplianceReportEntity existing = springDataRepository.findById(report.getReportId()).orElse(null);
        if (existing == null) {
            entity.setCreatedAt(now);
        } else {
            entity.setCreatedAt(existing.getCreatedAt());
        }
        entity.setUpdatedAt(now);

        ComplianceReportEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ComplianceReport> findRecentForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd, int limit) {
        // Current repository supports top 10. Keep limit bounded and simple.
        List<ComplianceReportEntity> entities = springDataRepository
                .findTop10ByBankIdAndReportingDateBetweenOrderByGeneratedAtDesc(bankId.getValue(), periodStart, periodEnd);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public int countForMonth(BankId bankId, LocalDate periodStart, LocalDate periodEnd) {
        return springDataRepository.countByBankIdAndReportingDateBetween(bankId.getValue(), periodStart, periodEnd);
    }

    private ComplianceReport toDomain(ComplianceReportEntity entity) {
        return new ComplianceReport(
                entity.getReportId(),
                entity.getBatchId(),
                BankId.of(entity.getBankId()),
                entity.getReportingDate(),
                entity.getReportType(),
                entity.getStatus(),
                entity.getGeneratedAt(),
                entity.getHtmlS3Uri(),
                entity.getXbrlS3Uri(),
                entity.getHtmlPresignedUrl(),
                entity.getXbrlPresignedUrl(),
                entity.getHtmlFileSize(),
                entity.getXbrlFileSize(),
                entity.getOverallQualityScore(),
                entity.getComplianceStatus(),
                entity.getGenerationDurationMillis()
        );
    }
}
