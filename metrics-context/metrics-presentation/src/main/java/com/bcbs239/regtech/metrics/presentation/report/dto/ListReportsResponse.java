package com.bcbs239.regtech.metrics.presentation.report.dto;

import java.util.List;

public record ListReportsResponse(
        List<ReportDto> reports,
        PaginationDto pagination,
        FiltersDto filters
) {}