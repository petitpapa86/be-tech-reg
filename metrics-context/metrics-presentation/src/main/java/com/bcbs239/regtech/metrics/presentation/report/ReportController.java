package com.bcbs239.regtech.metrics.presentation.report;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.metrics.application.report.ListReportsUseCase;
import com.bcbs239.regtech.metrics.application.report.UpdateReportUseCase;
import com.bcbs239.regtech.metrics.presentation.report.dto.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

@Component
public class ReportController {

    private final ListReportsUseCase listReportsUseCase;
    private final UpdateReportUseCase updateReportUseCase;

    public ReportController(ListReportsUseCase listReportsUseCase, UpdateReportUseCase updateReportUseCase) {
        this.listReportsUseCase = listReportsUseCase;
        this.updateReportUseCase = updateReportUseCase;
    }

    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterFunctions.route(
                RequestPredicates.GET("/api/v1/reports"),
                this::listReports
        ).andRoute(
                RequestPredicates.PUT("/api/v1/reports/{id}"),
                this::updateReport
        );
    }

    public ServerResponse listReports(ServerRequest request) {
        // Extract query parameters
        String name = request.param("name").orElse(null);
        String generatedAt = request.param("generatedAt").orElse(null);
        String referencePeriod = request.param("reference period").orElse(null);
        String status = request.param("status").orElse(null);
        int page = request.param("page").map(Integer::parseInt).orElse(1);
        int pageSize = request.param("pageSize").map(Integer::parseInt).orElse(10);

        // Create request DTO
        ListReportsRequest listRequest = new ListReportsRequest(
                name,
                generatedAt,
                referencePeriod,
                status,
                page,
                pageSize
        );

        // Create command
        ListReportsUseCase.ListReportsCommand command = new ListReportsUseCase.ListReportsCommand(
                listRequest.name(),
                listRequest.getGeneratedAtAsDate(),
                listRequest.getReferencePeriodAsDate(),
                listRequest.status(),
                listRequest.getPageZeroBased(),
                listRequest.pageSize()
        );

        // Execute use case
        Result<ListReportsUseCase.ListReportsResponse> result = listReportsUseCase.execute(command);

        if (result.isFailure()) {
            return ServerResponse.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ResponseUtils.success(null, "Failed to retrieve reports"));
        }

        // Convert to presentation DTOs
        ListReportsUseCase.ListReportsResponse useCaseResponse = result.getValue().orElseThrow();

        List<ReportDto> reportDtos = useCaseResponse.reports().stream()
                .map(report -> new ReportDto(
                        report.id(),
                        report.name(),
                        report.size(),
                        report.presignedS3Url(),
                        report.reportType(),
                        report.status(),
                        report.generatedAt(),
                        report.period()
                ))
                .toList();

        PaginationDto paginationDto = new PaginationDto(
                useCaseResponse.pagination().currentPage() + 1, // Convert back to 1-based
                useCaseResponse.pagination().pageSize(),
                useCaseResponse.pagination().totalPages(),
                useCaseResponse.pagination().totalItems(),
                useCaseResponse.pagination().hasNext(),
                useCaseResponse.pagination().hasPrevious()
        );

        FiltersDto filtersDto = new FiltersDto(
                useCaseResponse.filters().name(),
                useCaseResponse.filters().generatedAt(),
                useCaseResponse.filters().period(),
                useCaseResponse.filters().status()
        );

        ListReportsResponse response = new ListReportsResponse(
                reportDtos,
                paginationDto,
                filtersDto
        );

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtils.success(response, "Reports retrieved successfully"));
    }

    public ServerResponse updateReport(ServerRequest request) throws ServletException, IOException {
        String reportId = request.pathVariable("id");

        // Parse request body
        UpdateReportRequest updateRequest = request.body(UpdateReportRequest.class);

        // Create command
        UpdateReportUseCase.UpdateReportCommand command = new UpdateReportUseCase.UpdateReportCommand(
                reportId,
                updateRequest.status()
        );

        // Execute use case
        Result<UpdateReportUseCase.UpdateReportResponse> result = updateReportUseCase.execute(command);

        if (result.isFailure()) {
            return ServerResponse.status(400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ResponseUtils.success(null, result.getError().get().getMessage()));
        }

        // Convert to presentation DTO
        UpdateReportUseCase.UpdateReportResponse useCaseResponse = result.getValue().orElseThrow();

        ReportDto reportDto = new ReportDto(
                useCaseResponse.id(),
                useCaseResponse.name(),
                useCaseResponse.size(),
                useCaseResponse.presignedS3Url(),
                useCaseResponse.reportType(),
                useCaseResponse.status(),
                useCaseResponse.generatedAt(),
                useCaseResponse.period()
        );

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtils.success(reportDto, "Report updated successfully"));
    }
}