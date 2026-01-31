package com.bcbs239.regtech.metrics.presentation.report.dto;

import java.time.LocalDate;

public record ListReportsRequest(
        String name,
        String generatedAt,
        String referencePeriod,
        String status,
        Integer page,
        Integer pageSize
) {
    public ListReportsRequest {
        page = page != null ? page : 1;
        pageSize = pageSize != null ? pageSize : 10;
    }

    public LocalDate getGeneratedAtAsDate() {
        return generatedAt != null ? LocalDate.parse(generatedAt) : null;
    }

    public LocalDate getReferencePeriodAsDate() {
        return referencePeriod != null ? LocalDate.parse(referencePeriod) : null;
    }

    public int getPageZeroBased() {
        return Math.max(0, page - 1);
    }
}