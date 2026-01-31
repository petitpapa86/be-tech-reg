package com.bcbs239.regtech.metrics.presentation.report.dto;

public record FiltersDto(
        String name,
        String generatedAt,
        String period,
        String status
) {}