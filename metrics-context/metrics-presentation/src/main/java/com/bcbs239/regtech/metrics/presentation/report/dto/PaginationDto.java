package com.bcbs239.regtech.metrics.presentation.report.dto;

public record PaginationDto(
        int currentPage,
        int pageSize,
        int totalPages,
        int totalItems,
        boolean hasNext,
        boolean hasPrevious
) {}