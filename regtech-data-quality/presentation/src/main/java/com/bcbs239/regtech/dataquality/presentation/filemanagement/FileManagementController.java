package com.bcbs239.regtech.dataquality.presentation.filemanagement;

import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.dataquality.application.filemanagement.ListFilesQuery;
import com.bcbs239.regtech.dataquality.application.filemanagement.ListFilesQueryHandler;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FileListResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.RequestPredicates;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Presentation Layer: HTTP endpoint for file management.
 * Responsibility: Extract request parameters, delegate to application layer.
 */
@Configuration
public class FileManagementController {

    private final ListFilesQueryHandler listFilesQueryHandler;

    public FileManagementController(ListFilesQueryHandler listFilesQueryHandler) {
        this.listFilesQueryHandler = listFilesQueryHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> fileManagementRoutes() {
        return RouterFunctions.route(
            RequestPredicates.GET("/api/v1/files"),
            this::getFiles
        );
    }

    public ServerResponse getFiles(ServerRequest request) {
        // Extract parameters
        String bankIdStr = extractBankId(request);
        BankId bankId = new BankId(bankIdStr); // Assuming BankId constructor string
        
        String status = request.param("status").orElse(null);
        String period = request.param("period").orElse("last_month");
        String format = request.param("format").orElse("all");
        String search = request.param("search").orElse(null);
        int page = request.param("page").map(Integer::parseInt).orElse(1);
        int pageSize = request.param("page_size").map(Integer::parseInt).orElse(10);
        String sortBy = request.param("sort_by").orElse("upload_date");
        String sortOrder = request.param("sort_order").orElse("desc");
        
        // Calculate dateFrom based on period
        Instant dateFrom = calculateDateFrom(period);

        // Build query
        ListFilesQuery query = new ListFilesQuery(
            bankId,
            status,
            dateFrom,
            format,
            search,
            page,
            pageSize,
            sortBy,
            sortOrder
        );

        // Delegate to application layer
        FileListResponse response = listFilesQueryHandler.handle(query);

        // Return HTTP response
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ResponseUtils.success(response));
    }

    private String extractBankId(ServerRequest request) {
        // Extract from X-Bank-Id header
        return request.headers().header("X-Bank-Id").stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing required header: X-Bank-Id"));
    }

    private Instant calculateDateFrom(String period) {
        if (period == null) return null;
        return switch (period) {
            case "last_month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "last_3_months" -> Instant.now().minus(90, ChronoUnit.DAYS);
            case "last_6_months" -> Instant.now().minus(180, ChronoUnit.DAYS);
            case "last_year" -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> null;
        };
    }
}
