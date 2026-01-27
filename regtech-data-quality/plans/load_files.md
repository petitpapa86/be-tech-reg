# GitHub Copilot Instructions: GET /api/v1/files - DDD Architecture

## DDD Layered Architecture

```
dataquality/
├── presentation/          (Controllers, DTOs, API concerns)
├── application/           (Use cases, query handlers, application services)
├── domain/               (Entities, value objects, domain logic)
└── infrastructure/       (Repositories, persistence, external services)
```

---

## Layer Responsibilities

### 📱 Presentation Layer
**Responsibility:** HTTP handling, request/response mapping, API contracts
- Controllers (RouterFunction)
- Request/Response DTOs
- Query parameter extraction
- HTTP status codes

### 🎯 Application Layer
**Responsibility:** Use cases, orchestration, application logic
- Query handlers (use cases)
- Commands/Queries (CQRS)
- Application services (coordination)
- DTOs for inter-layer communication

### 💎 Domain Layer
**Responsibility:** Business rules, domain logic, entities
- Entities (QualityReportEntity? or better as domain model)
- Value Objects
- Domain Services
- Domain Events
- Repository Interfaces (port)

### 🔧 Infrastructure Layer
**Responsibility:** Technical implementation, persistence, external systems
- JPA Repositories (adapter)
- Database configuration
- External API clients
- File storage

---

## Implementation Structure

### Presentation Layer

```
presentation/
└── filemanagement/
    ├── FileManagementController.java
    └── dto/
        ├── FileListResponse.java
        ├── FileResponse.java
        ├── PaginationResponse.java
        └── FiltersApplied.java
```

**FileManagementController.java**
```java
package com.bcbs239.regtech.dataquality.presentation.filemanagement;

import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.dataquality.application.query.ListFilesQuery;
import com.bcbs239.regtech.dataquality.application.query.ListFilesQueryHandler;
import com.bcbs239.regtech.dataquality.presentation.filemanagement.dto.FileListResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.*;

/**
 * Presentation Layer: HTTP endpoint for file management.
 * Responsibility: Extract request parameters, delegate to application layer.
 */
@Component
public class FileManagementController {
    
    private final ListFilesQueryHandler listFilesQueryHandler;
    
    public FileManagementController(ListFilesQueryHandler listFilesQueryHandler) {
        this.listFilesQueryHandler = listFilesQueryHandler;
    }
    
    public RouterFunction<ServerResponse> mapEndpoints() {
        return RouterFunctions.route(
            RequestPredicates.GET("/api/v1/files"),
            this::getFiles
        );
    }
    
    public ServerResponse getFiles(ServerRequest request) {
        // Extract parameters (presentation concern)
        String bankId = extractBankId(request);
        
        // Build query (bridge to application layer)
        ListFilesQuery query = ListFilesQuery.of(
            bankId,
            request.param("status").orElse(null),
            request.param("period").orElse(null),
            request.param("format").orElse(null),
            request.param("search").orElse(null),
            request.param("page").map(Integer::parseInt).orElse(1),
            request.param("page_size").map(Integer::parseInt).orElse(10),
            request.param("sort_by").orElse(null),
            request.param("sort_order").orElse(null)
        );
        
        // Delegate to application layer
        FileListResponse response = listFilesQueryHandler.handle(query);
        
        // Return HTTP response
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ResponseUtils.success(response));
    }
    
    private String extractBankId(ServerRequest request) {
        // TODO: Extract from JWT token
        return request.param("bankId").orElse("BANK001");
    }
}
```

---

### Application Layer

```
application/
├── query/
│   ├── ListFilesQuery.java              (Query DTO)
│   ├── ListFilesQueryHandler.java       (Use case handler)
│   └── mapper/
│       └── FileResponseMapper.java      (Entity → DTO mapper)
└── service/
    └── (future application services)
```

**ListFilesQuery.java** (Query DTO)
```java
package com.bcbs239.regtech.dataquality.application.query;

/**
 * Application Layer: Query object for listing files.
 * Immutable DTO that represents the use case request.
 */
public record ListFilesQuery(
    String bankId,
    String statusFilter,
    String periodFilter,
    String formatFilter,
    String searchQuery,
    int page,
    int pageSize,
    String sortBy,
    String sortOrder
) {
    /**
     * Factory method with validation and defaults
     */
    public static ListFilesQuery of(
            String bankId,
            String statusFilter,
            String periodFilter,
            String formatFilter,
            String searchQuery,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder) {
        
        // Validate
        if (bankId == null || bankId.isBlank()) {
            throw new IllegalArgumentException("Bank ID is required");
        }
        
        // Apply defaults
        return new ListFilesQuery(
            bankId,
            statusFilter != null ? statusFilter : "all",
            periodFilter != null ? periodFilter : "last_month",
            formatFilter != null ? formatFilter : "all",
            searchQuery,
            Math.max(1, page),
            Math.max(1, Math.min(100, pageSize)),
            sortBy != null ? sortBy : "upload_date",
            sortOrder != null ? sortOrder : "desc"
        );
    }
}
```

**ListFilesQueryHandler.java** (Use Case)
```java
package com.bcbs239.regtech.dataquality.application.query;

import com.bcbs239.regtech.dataquality.domain.QualityReport;
import com.bcbs239.regtech.dataquality.domain.QualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.valueobject.BankId;
import com.bcbs239.regtech.dataquality.domain.valueobject.QualityStatus;
import com.bcbs239.regtech.dataquality.presentation.filemanagement.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Application Layer: Use case handler for listing files.
 * Orchestrates the query execution and response mapping.
 */
@Component
public class ListFilesQueryHandler {
    
    private final QualityReportRepository qualityReportRepository;
    private final FileResponseMapper fileResponseMapper;
    
    public ListFilesQueryHandler(
            QualityReportRepository qualityReportRepository,
            FileResponseMapper fileResponseMapper) {
        this.qualityReportRepository = qualityReportRepository;
        this.fileResponseMapper = fileResponseMapper;
    }
    
    /**
     * Execute the query use case
     */
    public FileListResponse handle(ListFilesQuery query) {
        // Convert to domain types
        BankId bankId = new BankId(query.bankId());
        QualityStatus status = convertStatusFilter(query.statusFilter());
        Instant dateFrom = calculateDateFrom(query.periodFilter());
        
        // Create pageable
        Pageable pageable = createPageable(query);
        
        // Query domain repository (through port)
        Page<QualityReport> reportPage = qualityReportRepository.findWithFilters(
            bankId,
            status,
            dateFrom,
            query.formatFilter(),
            query.searchQuery(),
            pageable
        );
        
        // Map domain models to DTOs
        List<FileResponse> files = reportPage.getContent().stream()
            .map(fileResponseMapper::toDto)
            .toList();
        
        // Build response
        return new FileListResponse(
            files,
            buildPagination(reportPage, query.page(), query.pageSize()),
            buildFiltersApplied(query)
        );
    }
    
    // Helper methods for filters, pagination, etc.
    // (Same as before, but using domain types)
    
    private QualityStatus convertStatusFilter(String statusFilter) {
        if (statusFilter == null || "all".equals(statusFilter)) {
            return null;
        }
        return switch (statusFilter.toLowerCase()) {
            case "completed" -> QualityStatus.COMPLETED;
            case "processing" -> QualityStatus.PROCESSING;
            case "error" -> QualityStatus.FAILED;
            default -> null;
        };
    }
    
    private Instant calculateDateFrom(String periodFilter) {
        if (periodFilter == null) return null;
        return switch (periodFilter) {
            case "last_month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "last_3_months" -> Instant.now().minus(90, ChronoUnit.DAYS);
            case "last_6_months" -> Instant.now().minus(180, ChronoUnit.DAYS);
            case "last_year" -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> null;
        };
    }
    
    private Pageable createPageable(ListFilesQuery query) {
        int springPage = Math.max(0, query.page() - 1);
        String sortField = mapSortField(query.sortBy());
        
        Sort sort = query.sortOrder().equalsIgnoreCase("asc")
            ? Sort.by(sortField).ascending()
            : Sort.by(sortField).descending();
        
        return PageRequest.of(springPage, query.pageSize(), sort);
    }
    
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "upload_date" -> "createdAt";
            case "name" -> "filename";
            case "quality_score" -> "overallScore";
            case "status" -> "status";
            default -> "createdAt";
        };
    }
    
    private PaginationResponse buildPagination(Page<?> page, int currentPage, int pageSize) {
        return new PaginationResponse(
            currentPage,
            pageSize,
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.hasPrevious(),
            page.hasNext(),
            page.hasPrevious() ? currentPage - 1 : null,
            page.hasNext() ? currentPage + 1 : null
        );
    }
    
    private FiltersApplied buildFiltersApplied(ListFilesQuery query) {
        return new FiltersApplied(
            query.statusFilter(),
            query.periodFilter(),
            query.formatFilter(),
            query.searchQuery()
        );
    }
}
```

**FileResponseMapper.java** (Mapper)
```java
package com.bcbs239.regtech.dataquality.application.query.mapper;

import com.bcbs239.regtech.dataquality.domain.QualityReport;
import com.bcbs239.regtech.dataquality.presentation.filemanagement.dto.*;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Application Layer: Maps domain models to presentation DTOs.
 * Responsibility: Domain → DTO transformation.
 */
@Component
public class FileResponseMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALIAN);
    
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");
    
    public FileResponse toDto(QualityReport report) {
        // Map domain model to DTO
        // (Same implementation as before, but using domain model)
        
        return new FileResponse(
            report.getReportId().value(),
            report.getFilename(),
            report.getFilename(),
            report.getFileSize(),
            formatBytes(report.getFileSize()),
            report.getFileFormat(),
            report.getMimeType(),
            report.getCreatedAt(),
            formatDate(report.getCreatedAt()),
            formatTime(report.getCreatedAt()),
            buildUploadedBy(report),
            report.getStatus().toDisplayString(),
            buildProcessingInfo(report),
            buildRecordStatistics(report),
            report.getQualityScore(),
            report.getComplianceScore(),
            buildViolationSummary(report),
            buildMetadata(report),
            buildReportLinks(report),
            buildActionPermissions(report)
        );
    }
    
    // Helper methods...
    // (Same as before)
}
```

---

### Domain Layer

for domai layer we have already class and object values  or may be you can read the core module for value objects

**QualityReport.java** (Domain Model - Aggregate Root)
```java
package com.bcbs239.regtech.dataquality.domain;

import com.bcbs239.regtech.dataquality.domain.valueobject.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain Layer: QualityReport aggregate root.
 * Contains business logic and invariants.
 */
@Getter
public class QualityReport {
    
    private final ReportId reportId;
    private final BankId bankId;
    private final String batchId;
    
    private String filename;
    private Long fileSize;
    private String fileFormat;
    private String mimeType;
    
    private QualityStatus status;
    private BigDecimal overallScore;
    
    private Integer totalExposures;
    private Integer validExposures;
    private Integer totalErrors;
    
    private Instant processingStartTime;
    private Instant processingEndTime;
    private Long processingDurationMs;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    // Private constructor - enforce creation through factory methods
    private QualityReport(ReportId reportId, BankId bankId, String batchId) {
        this.reportId = reportId;
        this.bankId = bankId;
        this.batchId = batchId;
        this.status = QualityStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Factory method
    public static QualityReport create(ReportId reportId, BankId bankId, String batchId) {
        return new QualityReport(reportId, bankId, batchId);
    }
    
    // Business methods
    
    public void startProcessing() {
        if (this.status != QualityStatus.PENDING) {
            throw new IllegalStateException("Can only start processing from PENDING state");
        }
        this.status = QualityStatus.PROCESSING;
        this.processingStartTime = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void completeProcessing(BigDecimal overallScore, Integer totalExposures, Integer validExposures) {
        if (this.status != QualityStatus.PROCESSING) {
            throw new IllegalStateException("Can only complete from PROCESSING state");
        }
        this.status = QualityStatus.COMPLETED;
        this.overallScore = overallScore;
        this.totalExposures = totalExposures;
        this.validExposures = validExposures;
        this.processingEndTime = Instant.now();
        this.processingDurationMs = calculateDuration();
        this.updatedAt = Instant.now();
    }
    
    public void failProcessing(String errorMessage) {
        this.status = QualityStatus.FAILED;
        this.processingEndTime = Instant.now();
        this.processingDurationMs = calculateDuration();
        this.updatedAt = Instant.now();
    }
    
    public boolean isCompleted() {
        return this.status == QualityStatus.COMPLETED;
    }
    
    public boolean canBeReprocessed() {
        return this.status == QualityStatus.FAILED;
    }
    
    public Integer getInvalidExposures() {
        if (totalExposures == null || validExposures == null) {
            return null;
        }
        return totalExposures - validExposures;
    }
    
    public Double getQualityScore() {
        return overallScore != null ? overallScore.doubleValue() : null;
    }
    
    public Double getComplianceScore() {
        // Business logic for compliance score
        if (overallScore == null) return null;
        return overallScore.multiply(BigDecimal.valueOf(0.9)).doubleValue();
    }
    
    private Long calculateDuration() {
        if (processingStartTime == null || processingEndTime == null) {
            return null;
        }
        return processingEndTime.toEpochMilli() - processingStartTime.toEpochMilli();
    }
    
}
```

**QualityReportRepository.java** (Repository Interface - Port)
```java
package com.bcbs239.regtech.dataquality.domain;

import com.bcbs239.regtech.dataquality.domain.valueobject.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

/**
 * Domain Layer: Repository interface (Port).
 * Defines contract for persistence without implementation details.
 */
public interface QualityReportRepository {
    
    QualityReport save(QualityReport report);
    
    Optional<QualityReport> findById(ReportId reportId);
    
    Page<QualityReport> findWithFilters(
        BankId bankId,
        QualityStatus status,
        Instant dateFrom,
        String format,
        String searchQuery,
        Pageable pageable
    );
    
    void delete(QualityReport report);
}
```


---

### Infrastructure Layer

```
infrastructure/
 C:\Users\alseny\Desktop\react projects\regtech\regtech-data-quality\infrastructure\src\main\java\com\bcbs239\regtech\dataquality\infrastructure\reporting\QualityReportEntity.java

for repo you can look here C:\Users\alseny\Desktop\react projects\regtech\regtech-data-quality\infrastructure\src\main\java\com\bcbs239\regtech\dataquality\infrastructure\reporting

---

## Summary: DDD Layers

### ✅ Proper Separation:

**Presentation** → Handles HTTP, DTOs
**Application** → Use cases, orchestration
**Domain** → Business logic, rules
**Infrastructure** → Persistence, external systems

### ✅ Dependency Rule:
```
Presentation → Application → Domain ← Infrastructure
```
- Presentation depends on Application
- Application depends on Domain
- Infrastructure depends on Domain (implements ports)
- Domain depends on NOTHING

### ✅ Benefits:
- Clean separation of concerns
- Testable (mock repositories)
- Domain logic is isolated
- Infrastructure can change

This is proper DDD! 🎯

---

END OF INSTRUCTIONS