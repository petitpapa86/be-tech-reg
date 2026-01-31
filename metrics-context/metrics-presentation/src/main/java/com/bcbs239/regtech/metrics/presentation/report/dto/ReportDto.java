package com.bcbs239.regtech.metrics.presentation.report.dto;

public record ReportDto(
        String id,
        String name,
        String size,
        String presignedS3Url,
        String reportType,
        String status,
        String generatedAt,
        String period
) {}