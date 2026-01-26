# GitHub Copilot Instructions: BCBS 239 File Management Backend

## Purpose
This document provides instructions for GitHub Copilot to implement a Spring Boot 4 REST API for BCBS 239 compliance file management. Use this as context when writing code with Copilot assistance.

---

## Project Overview

**Domain**: Banking regulatory compliance (BCBS 239 - Risk Data Aggregation and Reporting)
**Purpose**: Upload, validate, and report on compliance of risk data files from Italian banks
**Tech Stack**: Java 25, Spring Boot 4, PostgreSQL, AWS S3

---

## Existing Database Schema (DO NOT CREATE)

### Schema: `metrics`

You have 3 existing tables that store your data:

1. **`metrics.compliance_reports`** - Stores generated compliance reports
   - Primary key: `report_id` (VARCHAR 64)
   - Key fields: batch_id, bank_id, report_type, reporting_date, status
   - File references: html_s3_uri, xbrl_s3_uri, presigned URLs
   - Scores: overall_quality_score (NUMERIC 10,2)
   - Timestamps: generated_at, created_at, updated_at
   - Indexes: (bank_id, reporting_date), (bank_id, generated_at DESC)

2. **`metrics.dashboard_metrics`** - Aggregated metrics by bank and period
   - Composite primary key: (bank_id, period_start)
   - Scores: overall_score, data_quality_score, bcbs_rules_score, completeness_score
   - Counters: total_files_processed, total_violations, total_reports_generated, total_exposures, valid_exposures, total_errors
   - Version field for optimistic locking
   - TDigest blobs: data_quality_digest, completeness_digest (for percentile calculations)

3. **`metrics.metrics_file`** - Individual file processing records
   - Primary key: `id` (BIGINT auto-increment)
   - Key fields: filename, date, batch_id, bank_id, status
   - Scores: score (data quality), completeness_score

---

## What Needs to Be Built

### API Endpoints Required

#### 1. GET `/api/v1/files` - List files with filters
**Query Parameters:**
- status: filter by file status (all, completed, processing, error)
- period: time filter (last_month, last_3_months, last_6_months, last_year)
- format: file format filter (all, xlsx, csv, xml)
- search: search by filename
- page: page number (default 1)
- page_size: items per page (default 10)
- sort_by: field to sort by (upload_date, name, status, quality_score)
- sort_order: asc or desc (default desc)

**Response Structure:**
- List of files with: id, name, size, format, upload date, status, quality score, compliance score, violations count
- Pagination metadata: current page, total pages, total items, has_next, has_previous
- Statistics summary: total files, completed, processing, errors
- Applied filters summary

**Data Source**: Query `metrics.metrics_file` and join with `metrics.compliance_reports` for detailed metrics

#### 2. GET `/api/v1/files/{fileId}` - Get detailed file information
**Response Structure:**
- Complete file metadata
- Upload information (user, date, time)
- Processing status and stages (upload, parsing, validation, compliance_check)
- Record statistics (total, valid, invalid, duplicates)
- Quality and compliance scores
- Violations breakdown by severity (critical, high, medium, low) and principle (principle_1 through principle_14)
- BCBS 239 compliance per principle (14 principles)
- Data lineage (source system, extraction date, transformation pipeline)
- Report URLs (summary, detailed, violations)
- Audit trail

**Data Source**: Query `metrics.metrics_file` by id, join `metrics.compliance_reports` by batch_id

#### 3. GET `/api/v1/files/statistics` - Get aggregated statistics
**Query Parameters:**
- bank_id: filter by bank (optional)
- period: time period (optional)

**Response Structure:**
- Total files count by status (completed, processing, error)
- Files by format (xlsx, csv, xml)
- Files by period (today, this_week, this_month, last_3_months, etc.)
- Record statistics (total records, valid, invalid)
- Quality scores (average, median, min, max)
- Compliance scores (average, median, min, max, fully_compliant_count)
- Violations (total, by severity, by principle)
- Trends (quality_score_trend: improving/stable/declining, upload_volume_trend)

**Data Source**: Aggregate from `metrics.dashboard_metrics` for period-based metrics, `metrics.metrics_file` for file counts

#### 4. POST `/api/v1/files/upload` - Upload new file
**Request:**
- MultipartFile: the file to upload
- bank_id: the bank identifier
- source_system: optional source system name
- transformation_pipeline: optional pipeline identifier

**Process:**
1. Validate file (size, format)
2. Upload to S3 bucket (path: `uploads/{bank_id}/{year}/{month}/{unique_id}/{filename}`)
3. Create record in `metrics.metrics_file` with status "UPLOADED"
4. Trigger async processing
5. Return file metadata with id

#### 5. DELETE `/api/v1/files/{fileId}` - Delete file
**Process:**
1. Find file in `metrics.metrics_file`
2. Delete from S3
3. Delete database records (metrics_file, related compliance_reports)
4. Return success response

#### 6. GET `/api/v1/files/{fileId}/reports/{reportType}` - Download report
**Path Variables:**
- fileId: the file identifier
- reportType: summary, detailed, violations

**Process:**
1. Find compliance_report by batch_id (from metrics_file)
2. Get S3 URI based on report type
3. Generate presigned URL (1 hour validity)
4. Return file stream or redirect to presigned URL

---

## Service Layer Architecture

### FileManagementService
**Responsibilities:**
- Handle file upload workflow
- Coordinate with S3StorageService for file storage
- Create initial records in metrics.metrics_file
- Trigger async processing
- Handle file deletion
- Provide file list with filtering and pagination
- Retrieve detailed file information

**Key Methods:**
- uploadFile(MultipartFile, bankId, userId, sourceSystem, pipeline) → FileResponse
- getFiles(filters, pageable) → FileListResponse
- getFileDetails(fileId) → FileDetailsResponse
- deleteFile(fileId, userId) → void
- downloadFile(fileId) → Resource

### FileProcessingService
**Responsibilities:**
- Process uploaded files asynchronously using virtual threads
- Parse Excel files (Apache POI) and CSV files (OpenCSV)
- Extract record counts, column information
- Update processing status in metrics.metrics_file
- Handle processing errors

**Key Methods:**
- processFileAsync(fileId) → void (async)
- parseExcelFile(InputStream, filename) → ParsedFileData
- parseCsvFile(InputStream, filename) → ParsedFileData
- updateProgress(fileId, percentage, stage) → void
- handleProcessingError(fileId, errorCode, message) → void

### ComplianceValidationService
**Responsibilities:**
- Validate data quality (completeness, accuracy, consistency)
- Check BCBS 239 compliance (all 14 principles)
- Calculate quality score (0-100)
- Calculate compliance score (0-100)
- Identify violations with severity and principle mapping
- Update metrics.metrics_file with scores

**Key Methods:**
- validateDataQuality(ParsedFileData) → DataQualityResult
- checkBcbsCompliance(ParsedFileData) → BcbsComplianceResult
- calculateQualityScore(DataQualityResult) → double
- calculateComplianceScore(BcbsComplianceResult) → double
- identifyViolations(ParsedFileData) → List<Violation>

### ReportGenerationService
**Responsibilities:**
- Generate HTML compliance reports
- Generate XBRL reports (if needed)
- Upload reports to S3
- Create records in metrics.compliance_reports
- Calculate report file sizes and generation duration

**Key Methods:**
- generateSummaryReport(fileId) → ComplianceReport
- generateDetailedReport(fileId) → ComplianceReport
- generateViolationsReport(fileId) → ComplianceReport
- uploadReportToS3(report, content, type) → String (S3 URI)

### S3StorageService
**Responsibilities:**
- Upload files to AWS S3
- Generate presigned URLs for secure downloads (1 hour validity)
- Delete files from S3
- Download files from S3

**Key Methods:**
- uploadFile(InputStream, filename, contentType, bankId) → String (S3 URI)
- generatePresignedUrl(s3Uri, expirationMinutes) → String (URL)
- deleteFile(s3Uri) → void
- downloadFile(s3Uri) → InputStream

### StatisticsService
**Responsibilities:**
- Aggregate statistics from metrics tables
- Calculate trends over time
- Update dashboard_metrics periodically
- Handle TDigest serialization/deserialization for percentile calculations

**Key Methods:**
- getStatistics(bankId, period) → StatisticsResponse
- updateDashboardMetrics(bankId, periodStart) → void
- calculateTrends(bankId) → Map<String, String>
- aggregateFileMetrics(bankId, startDate, endDate) → AggregatedMetrics

---

## Repository Layer

### ComplianceReportRepository extends JpaRepository<ComplianceReport, String>
**Custom Queries Needed:**
- findByBankIdOrderByGeneratedAtDesc(bankId) → List<ComplianceReport>
- findByBankIdAndReportingDateBetween(bankId, startDate, endDate) → List<ComplianceReport>
- countByBankIdAndStatus(bankId, status) → long
- findByBatchId(batchId) → Optional<ComplianceReport>

### DashboardMetricsRepository extends JpaRepository<DashboardMetrics, DashboardMetricsId>
**Custom Queries Needed:**
- findByBankIdOrderByPeriodStartDesc(bankId) → List<DashboardMetrics>
- findByBankIdAndPeriodStartBetween(bankId, startDate, endDate) → List<DashboardMetrics>
- findLatestByBankId(bankId) → Optional<DashboardMetrics>

### MetricsFileRepository extends JpaRepository<MetricsFile, Long>
**Custom Queries Needed:**
- findByBankIdOrderByDateDesc(bankId) → List<MetricsFile>
- findByBankIdAndStatus(bankId, status) → List<MetricsFile>
- countByBankIdAndStatus(bankId, status) → long
- findByBatchId(batchId) → Optional<MetricsFile>
- findByBankIdWithFilters(bankId, status, dateFrom, dateTo, filename, pageable) → Page<MetricsFile>
- calculateStatsByBankId(bankId) → AggregatedStatistics (custom native query)

---

## Entity Mapping Requirements

### ComplianceReport Entity
- Map to table `metrics.compliance_reports`
- Use @Table(name = "compliance_reports", schema = "metrics")
- Map report_id as @Id
- Use Instant for timestamp fields (generated_at, created_at, updated_at)
- Use LocalDate for reporting_date
- Use BigDecimal for overall_quality_score
- Add @PrePersist and @PreUpdate for timestamps

### DashboardMetrics Entity
- Map to table `metrics.dashboard_metrics`
- Use @Table(name = "dashboard_metrics", schema = "metrics")
- Composite key using @IdClass(DashboardMetricsId.class)
- Use @Version for optimistic locking on version field
- Map TDigest fields as byte[] (data_quality_digest, completeness_digest)
- Use LocalDate for period_start

### DashboardMetricsId (Composite Key Class)
- Implement Serializable
- Fields: bankId (String), periodStart (LocalDate)
- Override equals() and hashCode()

### MetricsFile Entity
- Map to table `metrics.metrics_file`
- Use @Table(name = "metrics_file", schema = "metrics")
- Use @GeneratedValue(strategy = GenerationType.IDENTITY) for id
- Map date as String (since it's VARCHAR in DB - you might want to convert this later)
- Use Double for score and completeness_score

---

## DTO Response Structures

### FileResponse
Fields needed for file list view:
- id, name, originalName, size, sizeFormatted, format, mimeType
- uploadDate, uploadDateFormatted, uploadTimeFormatted
- uploadedBy: {id, name, email}
- status
- processing: {startedAt, completedAt, durationSeconds, progressPercentage}
- records: {total, valid, invalid, duplicates}
- qualityScore, complianceScore
- violations: {total, critical, high, medium, low}
- bcbs239: {overallCompliance, principleCompliance: {principle_1: true, ...}}
- metadata: {sheetCount, columnCount, dataLineageVerified}
- reports: {summaryReportUrl, detailedReportUrl, violationsReportUrl}
- actions: {canView, canDownload, canDelete, canReprocess, canExportReport}

### FileListResponse
- files: List<FileResponse>
- pagination: {currentPage, pageSize, totalItems, totalPages, hasNext, hasPrevious}
- statistics: {totalFiles, completedFiles, processingFiles, errorFiles}
- filtersApplied: {status, period, format, search}

### StatisticsResponse
- totalFiles, completedFiles, processingFiles, errorFiles
- byFormat: Map<String, Integer> (xlsx: 98, csv: 52, xml: 6)
- byPeriod: Map<String, Integer> (today: 3, this_week: 12, etc.)
- records: {total, valid, invalid}
- quality: {averageScore, medianScore, minScore, maxScore}
- compliance: {averageScore, medianScore, minScore, maxScore, fullyCompliantFiles}
- violations: {total, critical, high, medium, low, byPrinciple: Map<String, Integer>}
- trends: {qualityScoreTrend, complianceScoreTrend, uploadVolumeTrend}

### ApiResponse<T> (Generic Wrapper)
- success: boolean
- data: T
- error: {code, message, details} (when error occurs)
- timestamp: String (ISO format)
- requestId: String (for tracing)

---

## Data Mapping Logic

### From metrics.metrics_file to FileResponse:
- id → String.valueOf(metrics_file.id)
- name → metrics_file.filename
- uploadDate → parse metrics_file.date (handle VARCHAR date format)
- status → metrics_file.status
- qualityScore → metrics_file.score (multiply by 100 if stored as 0.0-1.0)
- complianceScore → calculate from compliance_reports if available

### From metrics.compliance_reports to FileResponse:
- reports.summaryReportUrl → generate presigned URL from html_s3_uri
- violations → parse from compliance_status or derive from quality score
- bcbs239.overallCompliance → overall_quality_score

### Aggregating statistics from metrics.dashboard_metrics:
- Sum total_files_processed across periods for totalFiles
- Sum total_violations for violations statistics
- Average overall_score for quality.averageScore
- Use data_quality_digest and completeness_digest for percentile calculations (if implementing TDigest)

---

## Configuration Requirements

### application.yml structure needed:
```
aws:
  s3:
    bucket-name: compliancecore-files-{env}
    region: eu-south-1
    presigned-url-duration-minutes: 60
    
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compliancecore
    username: myuser
    schema: metrics
  jpa:
    properties:
      hibernate:
        default_schema: metrics
        jdbc:
          time_zone: UTC
          
file:
  upload:
    max-size-mb: 100
    allowed-formats: xlsx,csv,xml
    
processing:
  async:
    core-pool-size: 4
    max-pool-size: 8
    use-virtual-threads: true
```

### Security Configuration:
- JWT-based authentication
- Extract bank_id from JWT token claims
- Validate user has access to bank_id in requests
- Audit all file operations (upload, delete, download)

### S3 Configuration:
- Use AWS SDK v2
- Configure S3Client bean with region and credentials
- Implement retry logic with exponential backoff
- Use virtual-hosted-style URLs for presigned URLs

---

## Processing Pipeline Flow

### 1. File Upload Flow:
```
User uploads file → Controller validates → Service saves to S3 → 
Create metrics_file record (status: UPLOADED) → Return response → 
Trigger async processing (virtual thread)
```

### 2. Async Processing Flow:
```
Download from S3 → Parse file (POI/OpenCSV) → 
Extract records → Validate data quality → 
Check BCBS compliance → Calculate scores → 
Identify violations → Update metrics_file → 
Generate reports → Upload reports to S3 → 
Create compliance_reports record → Update dashboard_metrics → 
Mark as COMPLETED or ERROR
```

### 3. Statistics Aggregation:
```
Scheduled job (daily) → Query metrics_file for period → 
Aggregate scores, counts, violations → 
Calculate trends → Update/Insert dashboard_metrics → 
Serialize TDigest if using percentiles
```

---

## Error Handling Strategy

### Use Result Pattern:
- Create Result<T> class with success/failure states
- Service methods return Result<T> instead of throwing exceptions
- Controller maps Result to ApiResponse

### Error Categories:
- FILE_TOO_LARGE: file exceeds max size
- INVALID_FORMAT: unsupported file format
- PARSING_ERROR: file cannot be parsed
- VALIDATION_FAILED: data quality issues
- S3_UPLOAD_FAILED: S3 operation failed
- DATABASE_ERROR: database operation failed
- FILE_NOT_FOUND: file id does not exist
- UNAUTHORIZED: user lacks permission

### Error Response Format:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "File validation failed",
    "details": "127 critical violations detected in principle_4"
  },
  "timestamp": "2025-01-26T10:30:00Z",
  "requestId": "req_abc123"
}
```

---

## Testing Requirements

### Unit Tests:
- Service layer methods (mock repositories)
- Compliance validation logic
- Score calculation algorithms
- File parsing logic
- DTO mapping

### Integration Tests:
- Repository queries against test database
- S3 operations against LocalStack
- Full upload and processing flow
- Statistics aggregation accuracy

### Test Data:
- Sample Excel files with valid/invalid data
- Sample CSV files with various encoding issues
- Mock compliance reports
- Historical dashboard_metrics for trend calculation

---

## Performance Considerations

### Database Query Optimization:
- Use pagination for all list endpoints
- Add indexes on (bank_id, date) for metrics_file
- Use batch inserts when creating multiple records
- Consider read replicas for statistics queries
- Cache dashboard_metrics for current period

### Async Processing:
- Use virtual threads (Java 25 feature)
- Process files in background
- Implement progress tracking (0-100%)
- Handle long-running operations (>5 minutes)
- Implement timeout and retry logic

### S3 Optimization:
- Use multipart upload for large files (>5MB)
- Implement file chunking for downloads
- Cache presigned URLs (up to 1 hour)
- Use S3 transfer acceleration if needed
- Implement S3 lifecycle policies for old reports

---

## BCBS 239 Principles Reference

The 14 principles to validate:
1. Governance - Data oversight and accountability
2. Data Architecture - Integrated data architecture
3. Accuracy and Integrity - Data accuracy
4. Completeness - Comprehensive data coverage
5. Timeliness - Timely data provisioning
6. Adaptability - Flexible data structure
7. Accuracy - Accurate reporting
8. Comprehensiveness - Complete reporting
9. Clarity - Clear and understandable
10. Frequency - Regular reporting
11. Distribution - Appropriate distribution
12. Confidentiality - Secure data handling
13. Validation - Data validation processes
14. Governance Framework - Risk governance

Each principle should have a compliance check that returns:
- compliant: boolean
- score: 0-100
- description: text explanation
- violations: list of specific issues

---

## MapStruct Mapping Instructions

### Create Mappers for:
1. MetricsFile → FileResponse
2. ComplianceReport → ReportInfo
3. DashboardMetrics → StatisticsResponse
4. List<MetricsFile> + Page → FileListResponse

### Mapping Annotations to Use:
- @Mapper(componentModel = "spring")
- @Mapping(source = "...", target = "...")
- @Named for complex mappings
- @AfterMapping for post-processing

### Custom Mapping Logic:
- Date formatting (Instant → "15 Gen 2025", "14:30")
- File size formatting (bytes → "2.4 MB")
- Score conversion (0.0-1.0 → 0-100)
- Status translation (DB codes → display labels)

---

## Logging Strategy

### Log Levels:
- INFO: File upload started, processing completed, reports generated
- WARN: Validation warnings, partial data issues
- ERROR: Processing failures, S3 errors, database errors
- DEBUG: Detailed processing steps, query execution

### Log Format:
Include in every log: requestId, userId, bankId, fileId (when applicable)

Example: `[req_abc123][user_xyz][bank_001][file_789] File processing started`

### Audit Logging:
Log to separate audit table:
- All file uploads (who, when, file metadata)
- All file deletions (who, when, reason)
- All file downloads (who, when)
- All report generations (when, duration, status)

---

## Deployment Considerations

### Environment Variables:
- DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
- AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION
- S3_BUCKET_NAME
- JWT_SECRET_KEY
- MAX_FILE_SIZE_MB
- ASYNC_PROCESSING_THREADS

### Health Checks:
- /actuator/health/db - Database connectivity
- /actuator/health/s3 - S3 connectivity
- /actuator/health/processing - Processing queue status

### Metrics to Expose:
- Files uploaded per minute
- Average processing time
- S3 upload/download latency
- Database query performance
- Error rate by type

---

## Implementation Priority Order

1. **Phase 1 - Core Setup:**
   - Entities, Repositories
   - Basic CRUD for metrics_file
   - S3 upload/download
   - GET /api/v1/files endpoint

2. **Phase 2 - File Processing:**
   - Excel/CSV parsing
   - Async processing with virtual threads
   - POST /api/v1/files/upload endpoint
   - Status tracking

3. **Phase 3 - Compliance:**
   - Compliance validation service
   - Score calculation
   - Violation identification
   - Update metrics during processing

4. **Phase 4 - Reporting:**
   - Report generation service
   - HTML report creation
   - S3 report storage
   - GET /api/v1/files/{id}/reports/{type}

5. **Phase 5 - Statistics:**
   - Statistics aggregation
   - Dashboard metrics updates
   - Trend calculation
   - GET /api/v1/files/statistics

6. **Phase 6 - Polish:**
   - Error handling
   - Validation
   - Testing
   - Documentation

---

## Key Business Rules

1. **File Size Limit:** Maximum 100MB per file
2. **Supported Formats:** .xlsx, .csv, .xml only
3. **Processing Timeout:** 30 minutes maximum per file
4. **Report Retention:** Keep reports for 7 years (compliance requirement)
5. **Presigned URL Expiry:** 1 hour for security
6. **Score Range:** All scores are 0.00 to 100.00
7. **Violation Severity:** CRITICAL blocks approval, HIGH requires review
8. **BCBS Compliance Threshold:** ≥95% for full compliance
9. **Data Retention:** Raw files kept for 5 years
10. **Audit Trail:** All operations logged permanently

---

## Important Notes for Copilot

- **Always use the existing schema** - Do not suggest creating new tables
- **Map fields correctly** - Pay attention to VARCHAR vs numeric types
- **Handle the date field** - metrics_file.date is VARCHAR, not DATE
- **Composite key** - dashboard_metrics uses (bank_id, period_start) as PK
- **Optimistic locking** - Use @Version on dashboard_metrics.version
- **TDigest handling** - These are binary blobs, may need special serialization
- **S3 URIs** - Store full S3 URIs (s3://bucket/key), generate presigned URLs on demand
- **Timezones** - All timestamps should be UTC
- **Italian locale** - Date formatting in "15 Gen 2025" format
- **Security** - Always validate bank_id matches JWT token claims

---

END OF INSTRUCTIONS

package com.bcbs239.regtech.metrics.presentation.filemanagement;

import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bcbs239.regtech.metrics.presentation.filemanagement.dto.*;

@Component
public class FileManagementController {

    public RouterFunction<ServerResponse> mapEndpoints() {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/v1/files"), this::getFiles)
                .andRoute(RequestPredicates.GET("/api/v1/files/{fileId}"), this::getFileDetails)
                .andRoute(RequestPredicates.GET("/api/v1/files/statistics"), this::getStatistics);
    }

    public ServerResponse getFiles(ServerRequest request) {
        // Extract query parameters
        String status = request.param("status").orElse("all");
        String period = request.param("period").orElse("last_month");
        String format = request.param("format").orElse("all");
        String search = request.param("search").orElse(null);
        int page = request.param("page").map(Integer::parseInt).orElse(1);
        int pageSize = request.param("page_size").map(Integer::parseInt).orElse(10);
        String sortBy = request.param("sort_by").orElse("upload_date");
        String sortOrder = request.param("sort_order").orElse("desc");

        // Create fake files data
        List<FileResponse> files = List.of(
                createFakeFile(
                        "file_7x9k2m4p",
                        "risk_data_Q1_2025.xlsx",
                        2516582L,
                        "xlsx",
                        "2025-01-15T14:30:00Z",
                        "completed",
                        45892,
                        43597,
                        2295,
                        0,
                        95.0,
                        98.0,
                        3,
                        0,
                        1,
                        2,
                        0
                ),
                createFakeFile(
                        "file_5n8j1q3r",
                        "compliance_check_jan.csv",
                        1887436L,
                        "csv",
                        "2025-01-26T09:15:00Z",
                        "processing",
                        32456,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                createFakeFile(
                        "file_2m7h5k9p",
                        "data_quality_report.xlsx",
                        3250790L,
                        "xlsx",
                        "2025-01-24T16:45:00Z",
                        "error",
                        58123,
                        26134,
                        31989,
                        1842,
                        45.0,
                        38.0,
                        127,
                        45,
                        52,
                        23,
                        7
                ),
                createFakeFile(
                        "file_8a2b3c4d",
                        "monthly_exposure_report.xlsx",
                        4123456L,
                        "xlsx",
                        "2025-01-20T10:20:00Z",
                        "completed",
                        78945,
                        76234,
                        2711,
                        0,
                        88.0,
                        92.0,
                        12,
                        0,
                        3,
                        7,
                        2
                ),
                createFakeFile(
                        "file_9e5f6g7h",
                        "liquidity_data_jan.csv",
                        1234567L,
                        "csv",
                        "2025-01-18T08:45:00Z",
                        "completed",
                        23456,
                        22890,
                        566,
                        0,
                        92.0,
                        96.0,
                        5,
                        0,
                        2,
                        3,
                        0
                )
        );

        // Create pagination info
        PaginationResponse pagination = new PaginationResponse(
                page,
                pageSize,
                156,
                16,
                page > 1,
                page < 16,
                page > 1 ? page - 1 : null,
                page < 16 ? page + 1 : null
        );

        // Create statistics summary
        StatisticsSummary statistics = new StatisticsSummary(
                156,
                142,
                8,
                6
        );

        // Create filters applied
        FiltersApplied filtersApplied = new FiltersApplied(
                status,
                period,
                format,
                search
        );

        FileListResponse response = new FileListResponse(
                files,
                pagination,
                statistics,
                filtersApplied
        );

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtils.success(response));
    }

    public ServerResponse getFileDetails(ServerRequest request) {
        String fileId = request.pathVariable("fileId");

        // Create fake detailed file data
        FileDetailsResponse details = createFakeFileDetails(fileId);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtils.success(details));
    }

    public ServerResponse getStatistics(ServerRequest request) {
        String bankId = request.param("bankId").orElse(null);
        String period = request.param("period").orElse(null);

        // Create fake statistics
        StatisticsResponse statistics = createFakeStatistics();

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtils.success(statistics));
    }

    // Helper methods to create fake data

    private FileResponse createFakeFile(
            String id,
            String filename,
            Long size,
            String format,
            String uploadDate,
            String status,
            Integer totalRecords,
            Integer validRecords,
            Integer invalidRecords,
            Integer duplicateRecords,
            Double qualityScore,
            Double complianceScore,
            Integer totalViolations,
            Integer criticalViolations,
            Integer highViolations,
            Integer mediumViolations,
            Integer lowViolations
    ) {
        Instant uploadInstant = Instant.parse(uploadDate);

        UploadedByInfo uploadedBy = new UploadedByInfo(
                "user_abc123",
                "Mario Rossi",
                "mario.rossi@bank.it"
        );

        ProcessingInfo processing = null;
        if ("completed".equals(status)) {
            processing = new ProcessingInfo(
                    uploadInstant.plusSeconds(15),
                    uploadInstant.plusSeconds(327),
                    312,
                    100,
                    null,
                    null
            );
        } else if ("processing".equals(status)) {
            processing = new ProcessingInfo(
                    uploadInstant.plusSeconds(30),
                    null,
                    null,
                    67,
                    "validation",
                    uploadInstant.plusSeconds(600)
            );
        } else if ("error".equals(status)) {
            processing = new ProcessingInfo(
                    uploadInstant.plusSeconds(20),
                    uploadInstant.plusSeconds(410),
                    390,
                    100,
                    null,
                    null
            );
        }

        RecordStatistics records = new RecordStatistics(
                totalRecords,
                validRecords,
                invalidRecords,
                duplicateRecords
        );

        ViolationSummary violations = totalViolations != null
                ? new ViolationSummary(totalViolations, criticalViolations, highViolations, mediumViolations, lowViolations)
                : null;

        BcbsComplianceInfo bcbs239 = complianceScore != null
                ? new BcbsComplianceInfo(complianceScore, createFakePrincipleCompliance())
                : null;

        FileMetadata metadata = new FileMetadata(
                format.equals("xlsx") ? 3 : null,
                47,
                true
        );

        ReportLinks reports = "completed".equals(status) || "error".equals(status)
                ? new ReportLinks(
                "/api/v1/files/" + id + "/reports/summary",
                "/api/v1/files/" + id + "/reports/detailed",
                "/api/v1/files/" + id + "/reports/violations"
        )
                : null;

        ActionPermissions actions = new ActionPermissions(
                !"processing".equals(status),
                true,
                true,
                true,
                !"processing".equals(status)
        );

        return new FileResponse(
                id,
                filename,
                filename,
                size,
                formatBytes(size),
                format,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                uploadInstant,
                formatDate(uploadInstant),
                formatTime(uploadInstant),
                uploadedBy,
                status,
                processing,
                records,
                qualityScore,
                complianceScore,
                violations,
                bcbs239,
                metadata,
                reports,
                actions
        );
    }

    private FileDetailsResponse createFakeFileDetails(String fileId) {
        Instant uploadInstant = Instant.parse("2025-01-15T14:30:00Z");

        UploadedByInfo uploadedBy = new UploadedByInfo(
                "user_abc123",
                "Mario Rossi",
                "mario.rossi@bank.it"
        );

        ProcessingInfo processing = new ProcessingInfo(
                uploadInstant.plusSeconds(15),
                uploadInstant.plusSeconds(327),
                312,
                100,
                null,
                null
        );

        List<ProcessingStage> stages = List.of(
                new ProcessingStage("upload", "completed", uploadInstant, uploadInstant.plusSeconds(15)),
                new ProcessingStage("parsing", "completed", uploadInstant.plusSeconds(15), uploadInstant.plusSeconds(65)),
                new ProcessingStage("validation", "completed", uploadInstant.plusSeconds(65), uploadInstant.plusSeconds(210)),
                new ProcessingStage("compliance_check", "completed", uploadInstant.plusSeconds(210), uploadInstant.plusSeconds(327))
        );

        RecordStatistics records = new RecordStatistics(45892, 43597, 2295, 0);

        List<ViolationDetail> violationDetails = List.of(
                new ViolationDetail(
                        "viol_1x2y3z",
                        "principle_4",
                        "high",
                        "Completezza dei dati: 5% dei record mancano del campo obbligatorio 'counterparty_id'",
                        2295,
                        "Aggiungere il campo 'counterparty_id' per tutti i record"
                ),
                new ViolationDetail(
                        "viol_4a5b6c",
                        "principle_4",
                        "medium",
                        "Formato data non uniforme nella colonna 'transaction_date'",
                        1250,
                        "Standardizzare il formato data a ISO 8601 (YYYY-MM-DD)"
                )
        );

        ViolationSummary violations = new ViolationSummary(3, 0, 1, 2, 0);

        Map<String, PrincipleCompliance> principleCompliance = createFakePrincipleCompliance();

        BcbsComplianceDetailed bcbs239 = new BcbsComplianceDetailed(
                principleCompliance,
                98.5,
                "Il file rispetta 13 su 14 principi BCBS 239. Sono state rilevate violazioni minori sul Principio 4 (Completezza)."
        );

        DataLineage dataLineage = new DataLineage(
                "Core Banking System v4.2",
                Instant.parse("2025-01-15T12:00:00Z"),
                "ETL_Risk_Data_v2.5",
                true
        );

        Map<String, ReportInfo> reports = Map.of(
                "summary", new ReportInfo("/api/v1/files/" + fileId + "/reports/summary", "pdf", uploadInstant.plusSeconds(342)),
                "detailed", new ReportInfo("/api/v1/files/" + fileId + "/reports/detailed", "pdf", uploadInstant.plusSeconds(357)),
                "violations", new ReportInfo("/api/v1/files/" + fileId + "/reports/violations", "xlsx", uploadInstant.plusSeconds(372))
        );

        List<AuditEntry> auditTrail = List.of(
                new AuditEntry(uploadInstant, "file_uploaded", "Mario Rossi", "File caricato tramite interfaccia web"),
                new AuditEntry(uploadInstant.plusSeconds(327), "processing_completed", "system", "Elaborazione completata con successo"),
                new AuditEntry(uploadInstant.plusSeconds(372), "reports_generated", "system", "Report di conformità generati")
        );

        return new FileDetailsResponse(
                fileId,
                "risk_data_Q1_2025.xlsx",
                "risk_data_Q1_2025.xlsx",
                2516582L,
                formatBytes(2516582L),
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                uploadInstant,
                uploadedBy,
                "completed",
                processing,
                stages,
                records,
                95.0,
                98.0,
                violations,
                violationDetails,
                bcbs239,
                dataLineage,
                reports,
                auditTrail
        );
    }

    private StatisticsResponse createFakeStatistics() {
        return new StatisticsResponse(
                156,
                142,
                8,
                6,
                Map.of("xlsx", 98, "csv", 52, "xml", 6),
                Map.of("today", 3, "this_week", 12, "this_month", 34, "last_3_months", 89),
                new RecordStatistics(7234567, 6789234, 445333, 0),
                new QualityStatistics(87.5, 92.0, 23.0, 100.0),
                new ComplianceStatistics(91.3, 95.0, 38.0, 100.0, 128),
                new ViolationStatistics(
                        245,
                        18,
                        67,
                        112,
                        48,
                        Map.of(
                                "principle_1", 12,
                                "principle_2", 8,
                                "principle_3", 34,
                                "principle_4", 89,
                                "principle_5", 23
                        )
                ),
                Map.of(
                        "qualityScoreTrend", "improving",
                        "complianceScoreTrend", "stable",
                        "uploadVolumeTrend", "increasing"
                )
        );
    }

    private Map<String, PrincipleCompliance> createFakePrincipleCompliance() {
        return Map.of(
                "principle_1", new PrincipleCompliance(true, 100.0, "Governance - Completamente conforme"),
                "principle_2", new PrincipleCompliance(true, 100.0, "Data Architecture - Conforme"),
                "principle_3", new PrincipleCompliance(true, 100.0, "Accuracy and Integrity - Conforme"),
                "principle_4", new PrincipleCompliance(false, 85.0, "Completeness - Parzialmente conforme (violazioni minori)"),
                "principle_5", new PrincipleCompliance(true, 100.0, "Timeliness - Conforme")
        );
    }

    // Formatting helper methods

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.ROOT, "%.1f GB", gb);
    }

    private String formatDate(Instant instant) {
        if (instant == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALIAN);
        return formatter.format(instant.atZone(ZoneId.of("Europe/Rome")));
    }

    private String formatTime(Instant instant) {
        if (instant == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN);
        return formatter.format(instant.atZone(ZoneId.of("Europe/Rome")));
    }
}

package com.bcbs239.regtech.metrics.presentation.filemanagement.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

// Main response for file list
public record FileListResponse(
List<FileResponse> files,
PaginationResponse pagination,
StatisticsSummary statistics,
FiltersApplied filtersApplied
) {}

// Individual file in the list
public record FileResponse(
String id,
String name,
String originalName,
Long size,
String sizeFormatted,
String format,
String mimeType,
Instant uploadDate,
String uploadDateFormatted,
String uploadTimeFormatted,
UploadedByInfo uploadedBy,
String status,
ProcessingInfo processing,
RecordStatistics records,
Double qualityScore,
Double complianceScore,
ViolationSummary violations,
BcbsComplianceInfo bcbs239,
FileMetadata metadata,
ReportLinks reports,
ActionPermissions actions
) {}

// Detailed file response
public record FileDetailsResponse(
String id,
String name,
String originalName,
Long size,
String sizeFormatted,
String format,
String mimeType,
Instant uploadDate,
UploadedByInfo uploadedBy,
String status,
ProcessingInfo processing,
List<ProcessingStage> stages,
RecordStatistics records,
Double qualityScore,
Double complianceScore,
ViolationSummary violations,
List<ViolationDetail> violationDetails,
BcbsComplianceDetailed bcbs239,
DataLineage dataLineage,
Map<String, ReportInfo> reports,
List<AuditEntry> auditTrail
) {}

// Pagination info
public record PaginationResponse(
int currentPage,
int pageSize,
int totalItems,
int totalPages,
boolean hasPrevious,
boolean hasNext,
Integer previousPage,
Integer nextPage
) {}

// Statistics summary for file list
public record StatisticsSummary(
int totalFiles,
int completedFiles,
int processingFiles,
int errorFiles
) {}

// Applied filters
public record FiltersApplied(
String status,
String period,
String format,
String search
) {}

// User who uploaded the file
public record UploadedByInfo(
String id,
String name,
String email
) {}

// Processing information
public record ProcessingInfo(
Instant startedAt,
Instant completedAt,
Integer durationSeconds,
Integer progressPercentage,
String currentStage,
Instant estimatedCompletion
) {}

// Processing stage (for detailed view)
public record ProcessingStage(
String name,
String status,
Instant startedAt,
Instant completedAt
) {}

// Record statistics
public record RecordStatistics(
Integer total,
Integer valid,
Integer invalid,
Integer duplicates
) {}

// Violation summary
public record ViolationSummary(
int total,
Integer critical,
Integer high,
Integer medium,
Integer low
) {}

// Violation detail (for detailed view)
public record ViolationDetail(
String id,
String principle,
String severity,
String description,
int affectedRecords,
String recommendation
) {}

// BCBS compliance info (summary)
public record BcbsComplianceInfo(
Double overallCompliance,
Map<String, PrincipleCompliance> principleCompliance
) {}

// BCBS compliance detailed
public record BcbsComplianceDetailed(
Map<String, PrincipleCompliance> principleCompliance,
Double overallCompliance,
String complianceSummary
) {}

// Individual principle compliance
public record PrincipleCompliance(
boolean compliant,
Double score,
String description
) {}

// File metadata
public record FileMetadata(
Integer sheetCount,
Integer columnCount,
boolean dataLineageVerified
) {}

// Data lineage (for detailed view)
public record DataLineage(
String sourceSystem,
Instant extractionDate,
String transformationPipeline,
boolean verified
) {}

// Report links (summary)
public record ReportLinks(
String summaryReportUrl,
String detailedReportUrl,
String violationsReportUrl
) {}

// Report info (detailed)
public record ReportInfo(
String url,
String format,
Instant generatedAt
) {}

// Action permissions
public record ActionPermissions(
boolean canView,
boolean canDownload,
boolean canDelete,
boolean canReprocess,
boolean canExportReport
) {}

// Audit entry (for detailed view)
public record AuditEntry(
Instant timestamp,
String action,
String user,
String details
) {}

// Statistics response
public record StatisticsResponse(
int totalFiles,
int completedFiles,
int processingFiles,
int errorFiles,
Map<String, Integer> byFormat,
Map<String, Integer> byPeriod,
RecordStatistics records,
QualityStatistics quality,
ComplianceStatistics compliance,
ViolationStatistics violations,
Map<String, String> trends
) {}

// Quality statistics
public record QualityStatistics(
Double averageScore,
Double medianScore,
Double minScore,
Double maxScore
) {}

// Compliance statistics
public record ComplianceStatistics(
Double averageScore,
Double medianScore,
Double minScore,
Double maxScore,
int fullyCompliantFiles
) {}

// Violation statistics
public record ViolationStatistics(
int total,
int critical,
int high,
int medium,
int low,
Map<String, Integer> byPrinciple
) {}