package com.bcbs239.regtech.metrics.infrastructure.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bcbs239.regtech.metrics.infrastructure.entity.ComplianceReportEntity;

public interface SpringDataComplianceReportRepository extends JpaRepository<ComplianceReportEntity, String> {

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

    @Query("SELECT r FROM ComplianceReportEntity r WHERE " +
           "(:name IS NULL OR LOWER(r.reportType) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:generatedAt IS NULL OR DATE(r.updatedAt) = :generatedAt) AND " +
           "(:reportingDate IS NULL OR r.reportingDate = :reportingDate) AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.updatedAt DESC")
    org.springframework.data.domain.Page<ComplianceReportEntity> findAllWithFilters(
            @Param("name") String name,
            @Param("generatedAt") LocalDate generatedAt,
            @Param("reportingDate") LocalDate reportingDate,
            @Param("status") String status,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM ComplianceReportEntity r WHERE " +
           "(:name IS NULL OR LOWER(r.reportType) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:generatedAt IS NULL OR DATE(r.updatedAt) = :generatedAt) AND " +
           "(:reportingDate IS NULL OR r.reportingDate = :reportingDate) AND " +
           "(:status IS NULL OR r.status = :status)")
    long countAllWithFilters(
            @Param("name") String name,
            @Param("generatedAt") LocalDate generatedAt,
            @Param("reportingDate") LocalDate reportingDate,
            @Param("status") String status
    );
}
