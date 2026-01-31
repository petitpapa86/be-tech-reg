package com.bcbs239.regtech.metrics.infrastructure.specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.bcbs239.regtech.metrics.infrastructure.entity.ComplianceReportEntity;

public class ComplianceReportSpecifications {

    public static Specification<ComplianceReportEntity> withFilters(
            String name, LocalDate generatedAt, LocalDate reportingDate, String status) {

        return Specification.where(nameContains(name))
                .and(generatedAtEquals(generatedAt))
                .and(reportingDateEquals(reportingDate))
                .and(statusEquals(status))
                .and(orderByUpdatedAtDesc());
    }

    private static Specification<ComplianceReportEntity> nameContains(String name) {
        return (root, query, cb) -> name == null ? null :
                cb.like(cb.lower(root.get("reportType")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<ComplianceReportEntity> generatedAtEquals(LocalDate generatedAt) {
        return (root, query, cb) -> generatedAt == null ? null :
                cb.equal(cb.function("DATE", java.sql.Date.class, root.get("updatedAt")), generatedAt);
    }

    private static Specification<ComplianceReportEntity> reportingDateEquals(LocalDate reportingDate) {
        return (root, query, cb) -> reportingDate == null ? null :
                cb.equal(root.get("reportingDate"), reportingDate);
    }

    private static Specification<ComplianceReportEntity> statusEquals(String status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }

    private static Specification<ComplianceReportEntity> orderByUpdatedAtDesc() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("updatedAt")));
            return null;
        };
    }
}