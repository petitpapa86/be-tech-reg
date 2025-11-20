# Report Generation Module - Design Document

## Overview

The Report Generation Module is a bounded context within the RegTech platform responsible for generating regulatory compliance reports for Large Exposures (Grandi Esposizioni) in accordance with CRR Regulation (EU) No. 575/2013 and EBA ITS standards. The module produces dual-format outputs: professional HTML reports for internal analysis and XBRL-XML files for regulatory submission.

### Key Responsibilities

- Listen to integration events from Risk Calculation and Data Quality modules
- Coordinate dual event arrival before triggering report generation
- Retrieve calculation results and quality metrics from S3 storage
- Generate HTML reports with interactive visualizations using Thymeleaf and Chart.js
- Generate XBRL-XML reports conforming to EBA COREP taxonomy
- Validate XBRL output against EBA XSD schemas
- Upload generated reports to S3 with encryption and versioning
- Persist report metadata to database for audit trail
- Publish ReportGeneratedEvent using transactional outbox pattern
- Provide comprehensive monitoring, metrics, and health checks

### Design Principles

1. **Event-Driven Architecture**: React to upstream events with idempotent processing
2. **Reliability First**: Automatic retry with exponential backoff for transient failures
3. **Circuit Breaker Pattern**: Prevent cascading failures during S3 service degradation
4. **Transactional Outbox**: Ensure reliable event publication with eventual consistency
5. **DDD Tactical Patterns**: Aggregates, value objects, domain events, and repositories
6. **Separation of Concerns**: 4-layer architecture (Domain → Application → Infrastructure → Presentation)
7. **Observability**: Structured logging, metrics, distributed tracing, and health checks

## Architecture

### Module Structure

The module follows a 4-layer architecture with each layer as a separate Maven module:

```
regtech-report-generation/
├── pom.xml (parent POM)
├── domain/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/domain/
│       ├── generation/          # Report generation aggregates and logic
│       ├── coordination/        # Event coordination logic
│       ├── validation/          # XBRL and data validation
│       ├── storage/             # Storage abstractions
│       └── shared/              # Shared value objects and exceptions
├── application/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/application/
│       ├── generation/          # Report generation commands and handlers
│       ├── coordination/        # Event coordination service
│       ├── integration/         # Event listeners and publishers
│       └── monitoring/          # Performance monitoring
├── infrastructure/
│   ├── pom.xml
│   └── src/main/java/.../reportgeneration/infrastructure/
│       ├── database/            # JPA repositories and entities
│       ├── filestorage/         # S3 and local file storage implementations
│       ├── templates/           # Thymeleaf configuration and templates
│       ├── xbrl/                # XBRL generation and validation
│       └── config/              # Spring configuration
└── presentation/
    ├── pom.xml
    └── src/main/java/.../reportgeneration/presentation/
        ├── health/              # Health check indicators
        └── monitoring/          # Metrics collectors
```

**Dependency Flow**: Domain ← Application ← Infrastructure ← Presentation

Each layer is organized by domain concepts (generation, coordination, validation) rather than technical patterns (controllers, services, repositories).

### Event Flow Diagram

```
┌─────────────────┐         ┌─────────────────┐
│ Risk Calculation│         │  Data Quality   │
│     Module      │         │     Module      │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │ BatchCalculationCompleted │
         │                           │ BatchQualityCompleted
         ▼                           ▼
    ┌────────────────────────────────────┐
    │      Event Listener Layer          │
    │  - Validate events                 │
    │  - Check idempotency               │
    └────────────┬───────────────────────┘
                 │
                 ▼
    ┌────────────────────────────────────┐
    │   Report Coordinator Service       │
    │  - Track pending events            │
    │  - Wait for both events            │
    └────────────┬───────────────────────┘
                 │ Both events received
                 ▼
    ┌────────────────────────────────────┐
    │   Report Generation Service        │
    │  1. Download calculation results   │
    │  2. Generate HTML report           │
    │  3. Generate XBRL report           │
    │  4. Upload to S3                   │
    │  5. Save metadata to DB            │
    │  6. Raise domain event             │
    └────────────┬───────────────────────┘
                 │
                 ▼
    ┌────────────────────────────────────┐
    │      Outbox Processor              │
    │  - Poll outbox table               │
    │  - Publish ReportGeneratedEvent    │
    └────────────────────────────────────┘
```



## Components and Interfaces

### Domain Layer (regtech-report-generation/domain)

The domain layer is organized by domain concepts, not technical patterns. Each package represents a cohesive domain capability.

#### domain/generation/ - Report Generation Aggregates

**GeneratedReport** (Aggregate Root)

Following the "ask the object what it can do" principle, GeneratedReport encapsulates all report generation state and behavior. External code asks the aggregate to perform actions rather than manipulating its state directly.

```java
public class GeneratedReport extends BaseEntity {
    private ReportId reportId;
    private BatchId batchId;
    private BankId bankId;
    private ReportingDate reportingDate;
    private ReportStatus status;
    private HtmlReportMetadata htmlMetadata;
    private XbrlReportMetadata xbrlMetadata;
    private ProcessingTimestamps timestamps;
    private Optional<FailureReason> failureReason;
    
    // Factory method - "Create yourself"
    public static GeneratedReport create(BatchId batchId, BankId bankId, 
                                         ReportingDate reportingDate) {
        GeneratedReport report = new GeneratedReport();
        report.reportId = ReportId.generate();
        report.batchId = batchId;
        report.bankId = bankId;
        report.reportingDate = reportingDate;
        report.status = ReportStatus.PENDING;
        report.timestamps = ProcessingTimestamps.start();
        return report;
    }
    
    // Behavior methods - "Tell, don't ask"
    public void recordHtmlGeneration(S3Uri s3Uri, FileSize fileSize, 
                                     PresignedUrl presignedUrl) {
        this.htmlMetadata = new HtmlReportMetadata(s3Uri, fileSize, presignedUrl);
        this.checkIfCompleted();
    }
    
    public void recordXbrlGeneration(S3Uri s3Uri, FileSize fileSize, 
                                     PresignedUrl presignedUrl) {
        this.xbrlMetadata = new XbrlReportMetadata(s3Uri, fileSize, presignedUrl);
        this.checkIfCompleted();
    }
    
    public void markAsCompleted() {
        if (!this.hasHtmlReport() || !this.hasXbrlReport()) {
            throw new IllegalStateException("Cannot complete without both reports");
        }
        this.status = ReportStatus.COMPLETED;
        this.timestamps = this.timestamps.complete();
        this.raiseEvent(new ReportGeneratedEvent(this));
    }
    
    public void markAsPartial(String reason) {
        this.status = ReportStatus.PARTIAL;
        this.failureReason = Optional.of(new FailureReason(reason));
        this.timestamps = this.timestamps.complete();
    }
    
    public void markAsFailed(FailureReason reason) {
        this.status = ReportStatus.FAILED;
        this.failureReason = Optional.of(reason);
        this.timestamps = this.timestamps.complete();
        this.raiseEvent(new ReportGenerationFailedEvent(this.batchId, reason));
    }
    
    // Query methods - safe to ask about state
    public boolean isCompleted() { return status == ReportStatus.COMPLETED; }
    public boolean canRegenerate() { 
        return status == ReportStatus.FAILED || status == ReportStatus.PARTIAL; 
    }
    
    // Private helper - encapsulates business logic
    private void checkIfCompleted() {
        if (this.hasHtmlReport() && this.hasXbrlReport()) {
            this.markAsCompleted();
        }
    }
    
    private boolean hasHtmlReport() { return htmlMetadata != null; }
    private boolean hasXbrlReport() { return xbrlMetadata != null; }
}
```

#### domain/shared/valueobjects/ - Shared Value Objects

**ReportId**: Unique identifier for generated reports (UUID)
**ReportStatus**: Enumeration (PENDING, IN_PROGRESS, COMPLETED, PARTIAL, FAILED)
**HtmlReportMetadata**: S3 URI, file size, presigned URL, generation timestamp
**XbrlReportMetadata**: S3 URI, file size, presigned URL, validation status
**PresignedUrl**: Temporary authenticated URL with expiration (1 hour)
**FailureReason**: Categorized failure information (type, message, timestamp)
**ReportingDate**: Date for which the report is generated
**S3Uri**: Validated S3 URI (s3://bucket/key format)
**FileSize**: File size in bytes with human-readable formatting

#### domain/generation/ - Report Generation Services

**HtmlReportGenerator**
- Generates professional HTML reports using Thymeleaf templates
- Integrates Chart.js for interactive visualizations
- Applies Tailwind CSS styling
- Returns generated HTML as string

```java
public interface HtmlReportGenerator {
    String generate(CalculationResults results, ReportMetadata metadata);
}
```

**XbrlReportGenerator**
- Generates XBRL-XML conforming to EBA COREP taxonomy
- Creates contexts for each exposure with dimensions
- Populates LE1 and LE2 template facts
- Returns XML document

```java
public interface XbrlReportGenerator {
    Document generate(CalculationResults results, ReportMetadata metadata);
}
```

**XbrlValidator**
- Validates XBRL documents against EBA XSD schema
- Returns validation result with errors if any

```java
public interface XbrlValidator {
    ValidationResult validate(Document xbrlDocument);
}
```



#### domain/generation/events/ - Domain Events

**ReportGeneratedEvent**
```java
public class ReportGeneratedEvent extends BaseDomainEvent {
    private ReportId reportId;
    private BatchId batchId;
    private BankId bankId;
    private ReportingDate reportingDate;
    private PresignedUrl htmlPresignedUrl;
    private PresignedUrl xbrlPresignedUrl;
    private Instant generatedAt;
}
```

**ReportGenerationFailedEvent**
```java
public class ReportGenerationFailedEvent extends BaseDomainEvent {
    private BatchId batchId;
    private FailureReason failureReason;
    private Instant failedAt;
}
```

#### domain/generation/ - Repository Interfaces

**IGeneratedReportRepository**
```java
public interface IGeneratedReportRepository {
    Optional<GeneratedReport> findByBatchId(BatchId batchId);
    Optional<GeneratedReport> findByReportId(ReportId reportId);
    void save(GeneratedReport report);
    boolean existsByBatchId(BatchId batchId);
}
```

### Application Layer (regtech-report-generation/application)

The application layer orchestrates domain logic and coordinates with external systems. Organized by use cases and integration points.

#### application/integration/ - Event Listeners

**BatchCalculationCompletedEventListener**
- Listens to BatchCalculationCompletedEvent from Risk Calculation Module
- Validates event (not null, not stale > 24 hours)
- Checks idempotency (batch not already processed)
- Delegates to ReportCoordinatorService
- Uses @Async for non-blocking processing
- Saves EventProcessingFailure on errors for retry

```java
@Component
public class BatchCalculationCompletedEventListener {
    
    @Async("reportGenerationExecutor")
    @EventListener
    public void handleBatchCalculationCompleted(
        BatchCalculationCompletedEvent event) {
        // Validate event
        // Check idempotency
        // Delegate to coordinator
        // Handle errors with EventProcessingFailure
    }
}
```

**BatchQualityCompletedEventListener**
- Similar structure to calculation listener
- Listens to BatchQualityCompletedEvent from Data Quality Module
- Coordinates with ReportCoordinatorService

#### application/coordination/ - Coordination Services

**ReportCoordinatorService**
- Coordinates arrival of dual events (calculation + quality)
- Maintains in-memory tracking of pending events
- Triggers report generation when both events received
- Thread-safe with concurrent access handling

```java
@Service
public class ReportCoordinatorService {
    private final ConcurrentHashMap<BatchId, EventTracker> pendingEvents;
    
    public void registerCalculationEvent(BatchCalculationCompletedEvent event);
    public void registerQualityEvent(BatchQualityCompletedEvent event);
    private void triggerReportGeneration(BatchId batchId);
}
```

#### application/generation/ - Report Generation Services

**ReportGenerationService**
- Orchestrates the complete report generation workflow
- Downloads calculation results from S3
- Validates data integrity
- Generates HTML and XBRL reports
- Uploads reports to S3
- Persists metadata to database
- Handles errors with appropriate recovery strategies

```java
@Service
public class ReportGenerationService {
    
    public void generateReport(BatchId batchId, BankId bankId, 
                               ReportingDate reportingDate) {
        // 1. Download calculation results
        // 2. Validate data
        // 3. Generate HTML report
        // 4. Generate XBRL report
        // 5. Upload to S3
        // 6. Save metadata
        // 7. Publish event via outbox
    }
}
```



#### application/integration/ - Event Publishers

**ReportGenerationEventPublisher**
- Publishes ReportGeneratedEvent to integration event bus
- Uses transactional outbox pattern
- Relies on OutboxProcessor for retry and eventual delivery

```java
@Component
public class ReportGenerationEventPublisher {
    private final IIntegrationEventBus eventBus;
    
    public void publishReportGenerated(ReportGeneratedEvent event) {
        // Event is already in outbox via domain event
        // OutboxProcessor will publish to event bus
    }
}
```

### Infrastructure Layer (regtech-report-generation/infrastructure)

The infrastructure layer provides technical implementations of domain interfaces. Organized by technical capability.

#### infrastructure/database/ - Persistence

**JpaGeneratedReportRepository**
- Implements IGeneratedReportRepository
- Maps GeneratedReport aggregate to report_generation_summaries table
- Handles optimistic locking with version field

**SpringDataGeneratedReportRepository**
- Spring Data JPA interface for database operations
- Provides query methods by batch_id and report_id

**GeneratedReportEntity**
```java
@Entity
@Table(name = "report_generation_summaries")
public class GeneratedReportEntity {
    @Id
    private UUID reportId;
    private String batchId;
    private String bankId;
    private LocalDate reportingDate;
    private String status;
    private String htmlS3Uri;
    private Long htmlFileSize;
    private String htmlPresignedUrl;
    private String xbrlS3Uri;
    private Long xbrlFileSize;
    private String xbrlPresignedUrl;
    private Instant generatedAt;
    private Instant completedAt;
    private String failureReason;
    @Version
    private Long version;
}
```

#### infrastructure/filestorage/ - File Storage

**S3ReportStorageService**
- Uploads HTML and XBRL reports to S3
- Applies encryption (AES-256), metadata tags, and versioning
- Generates presigned URLs with 1-hour expiration
- Implements circuit breaker pattern with Resilience4j
- Falls back to local filesystem on circuit open

```java
@Service
public class S3ReportStorageService {
    
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadToLocalFallback")
    public S3UploadResult uploadHtmlReport(String content, String fileName);
    
    @CircuitBreaker(name = "s3-upload", fallbackMethod = "uploadToLocalFallback")
    public S3UploadResult uploadXbrlReport(Document xbrl, String fileName);
    
    public PresignedUrl generatePresignedUrl(S3Uri s3Uri, Duration expiration);
    
    private S3UploadResult uploadToLocalFallback(String content, String fileName, 
                                                 Exception ex);
}
```

**LocalFileStorageService**
- Fallback storage for deferred uploads
- Saves files to /tmp/deferred-reports/
- Scheduled job retries uploads when S3 recovers

#### infrastructure/templates/ - Template Engine

**ThymeleafConfiguration**
- Configures Thymeleaf template engine
- Sets template resolver for classpath:/templates/reports/
- Enables caching in production, disables in development

**HtmlReportGeneratorImpl**
- Implements HtmlReportGenerator domain service
- Uses Thymeleaf Context with model data
- Processes large-exposures-report.html template
- Includes Chart.js data for donut and bar charts



#### infrastructure/xbrl/ - XBRL Generation

**XbrlReportGeneratorImpl**
- Implements XbrlReportGenerator domain service
- Creates XML document with EBA COREP namespaces
- Adds schema reference to EBA taxonomy
- Creates contexts with dimensions (CP, CT, SC)
- Populates LE1 facts (counterparty details)
- Populates LE2 facts (exposure amounts)
- Formats XML with pretty-print

**XbrlSchemaValidator**
- Validates XBRL documents against EBA XSD schema
- Loads schema from classpath:/schemas/eba-corep.xsd
- Returns detailed validation errors with line numbers
- Attempts automatic corrections (trim whitespace, round decimals)

#### infrastructure/config/ - Circuit Breaker

**Resilience4jConfiguration**
- Configures circuit breaker for S3 operations
- Failure threshold: 10 consecutive failures OR 50% failure rate over 5 minutes
- Wait duration in open state: 5 minutes
- Permitted calls in half-open state: 1
- Emits metrics for circuit breaker state transitions

### Presentation Layer (regtech-report-generation/presentation)

The presentation layer exposes monitoring and observability endpoints.

#### presentation/health/ - Health Checks

**ReportGenerationHealthIndicator**
- Custom Spring Boot Actuator health indicator
- Checks database connectivity
- Checks S3 accessibility (HEAD request to bucket)
- Checks event tracker state (pending events count)
- Checks async executor queue size
- Returns UP, WARN, or DOWN status

```java
@Component
public class ReportGenerationHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check database
        // Check S3
        // Check event tracker
        // Check async executor
        // Return aggregate health
    }
}
```

#### presentation/monitoring/ - Metrics

**ReportGenerationMetricsCollector**
- Collects and emits Micrometer metrics
- Timers: report.generation.duration, report.html.generation, report.xbrl.generation, report.s3.upload
- Counters: report.generation.success, report.generation.failure, report.generation.partial, report.generation.retry
- Gauges: report.async.executor.queue.size, report.async.executor.active.threads, report.deferred.uploads.count

## Data Models

### Database Schema

**report_generation_summaries**
```sql
CREATE TABLE report_generation_summaries (
    report_id UUID PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    bank_id VARCHAR(20) NOT NULL,
    reporting_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    html_s3_uri TEXT,
    html_file_size BIGINT,
    html_presigned_url TEXT,
    xbrl_s3_uri TEXT,
    xbrl_file_size BIGINT,
    xbrl_presigned_url TEXT,
    generated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    failure_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(batch_id)
);

CREATE INDEX idx_report_batch_id ON report_generation_summaries(batch_id);
CREATE INDEX idx_report_bank_id ON report_generation_summaries(bank_id);
CREATE INDEX idx_report_status ON report_generation_summaries(status);
```

**report_metadata_failures** (for compensating transactions)
```sql
CREATE TABLE report_metadata_failures (
    id UUID PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    html_s3_uri TEXT,
    xbrl_s3_uri TEXT,
    failed_at TIMESTAMP NOT NULL,
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP
);
```



### DTOs and Integration Events

**CalculationResults** (from S3)
```java
public class CalculationResults {
    private String batchId;
    private String bankId;
    private LocalDate reportingDate;
    private BigDecimal tier1Capital;
    private List<CalculatedExposure> largeExposures;
    private int totalExposuresCount;
    private BigDecimal totalExposureAmount;
    private int limitBreachesCount;
    private Map<String, BigDecimal> sectorBreakdown;
    private Map<String, BigDecimal> geographicBreakdown;
}
```

**CalculatedExposure**
```java
public class CalculatedExposure {
    private String counterpartyName;
    private String leiCode;
    private String identifierType;
    private String countryCode;
    private String sectorCode;
    private String rating;
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal amountEur;
    private BigDecimal amountAfterCrm;
    private BigDecimal tradingBookPortion;
    private BigDecimal nonTradingBookPortion;
    private BigDecimal percentageOfCapital;
    private boolean compliant;
}
```

**BatchCalculationCompletedEvent** (Integration Event)
```java
public class BatchCalculationCompletedEvent extends BaseIntegrationEvent {
    private String batchId;
    private String bankId;
    private LocalDate reportingDate;
    private String calculationResultsS3Uri;
    private Instant calculatedAt;
}
```

**BatchQualityCompletedEvent** (Integration Event)
```java
public class BatchQualityCompletedEvent extends BaseIntegrationEvent {
    private String batchId;
    private String bankId;
    private String qualityStatus;
    private Instant validatedAt;
}
```

**ReportGeneratedEvent** (Integration Event)
```java
public class ReportGeneratedEvent extends BaseIntegrationEvent {
    private String reportId;
    private String batchId;
    private String bankId;
    private LocalDate reportingDate;
    private String htmlPresignedUrl;
    private String xbrlPresignedUrl;
    private Instant generatedAt;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*



### Property Reflection

After analyzing all acceptance criteria, several properties can be consolidated to eliminate redundancy:

- Properties 1.2 and 20.1 both test duplicate event detection - combine into single property
- Properties 1.5, 2.4, and 15.3 all test EventProcessingFailure persistence - combine into single property
- Properties 3.4 and 15.2 both test retry with exponential backoff - combine into single property
- Properties 4.1 and 14.2 both test malformed JSON handling - combine into single property
- Properties 4.2 and 14.3 both test validation failure handling - combine into single property
- Properties 9.4 and 12.3 both test presigned URL expiration - combine into single property
- Properties 11.1 and 12.1 both test event publication - combine into single property

### Correctness Properties

Property 1: Event validation accepts valid events and rejects invalid ones
*For any* BatchCalculationCompletedEvent or BatchQualityCompletedEvent, if the event is properly structured with non-null required fields, validation should succeed; if the event is null or missing required fields, validation should fail
**Validates: Requirements 1.1**

Property 2: Duplicate events are detected and skipped
*For any* event with a specific event ID, if the event is processed once and then received again with the same event ID, the second processing should be skipped without error
**Validates: Requirements 1.2, 20.1**

Property 3: Async processing executes on different thread
*For any* valid event, when the event listener processes it, the processing should occur on a thread different from the caller's thread
**Validates: Requirements 1.3**

Property 4: Stale events are rejected
*For any* event with a timestamp, if the event timestamp is older than 24 hours from current time, the event should be rejected and processing skipped
**Validates: Requirements 1.4**

Property 5: Failed event processing creates retry records
*For any* event processing that encounters an error, an EventProcessingFailure record should be created with the event payload for automatic retry
**Validates: Requirements 1.5, 2.4, 15.3**

Property 6: Concurrent events for different batches process independently
*For any* set of events for different batch IDs arriving concurrently, each batch should be processed independently without interference or shared state corruption
**Validates: Requirements 2.1**

Property 7: Retry processor successfully reprocesses failed events
*For any* EventProcessingFailure record, when EventRetryProcessor retries the event, the processing should complete successfully if the transient failure has resolved
**Validates: Requirements 2.5**

Property 8: File download uses correct storage based on environment
*For any* batch ID in production mode, the file path should be s3://risk-analysis/calculated/calc_batch_{id}.json; in development mode, the path should be /data/calculated/calc_batch_{id}.json
**Validates: Requirements 3.1, 3.2**

Property 9: File download failures trigger retry with exponential backoff
*For any* file download that fails due to transient errors, retry should occur with exponentially increasing backoff intervals
**Validates: Requirements 3.4, 15.2**

Property 10: Malformed JSON is saved and prevents report generation
*For any* JSON file that fails parsing, the malformed content should be saved to /tmp/malformed-json/ and no report should be generated
**Validates: Requirements 4.1, 14.2**

Property 11: Missing required fields prevent report generation
*For any* data with missing required fields, validation should fail with specific error messages and no report should be generated
**Validates: Requirements 4.2, 14.3**

Property 12: Checksum validation failures prevent report generation
*For any* data with invalid checksum, validation should fail and no report should be generated
**Validates: Requirements 4.3**

Property 13: Valid data is mapped to DTOs and processed
*For any* valid JSON data, the data should be successfully mapped to Java DTOs and report generation should proceed
**Validates: Requirements 4.4**

Property 14: File download timeout is enforced
*For any* file download operation, if the operation exceeds 30 seconds, it should timeout with an error
**Validates: Requirements 4.5**

Property 15: HTML report contains required header elements
*For any* generated HTML report, the content should include header section with bank information, reporting date, and regulatory references
**Validates: Requirements 5.2**

Property 16: HTML report contains all five summary cards
*For any* generated HTML report, the content should include exactly five executive summary cards for Tier 1 Capital, Large Exposures Count, Total Amount, Limit Breaches, and Sector Concentration
**Validates: Requirements 5.3**

Property 17: HTML report contains required chart configurations
*For any* generated HTML report, the content should include Chart.js configuration for a donut chart (sector distribution) and a horizontal bar chart (top exposures)
**Validates: Requirements 5.4**

Property 18: HTML report table contains all required columns
*For any* generated HTML report, the exposure table should include columns for counterparty, LEI, sector, rating, amount, percentage of capital, and compliance status
**Validates: Requirements 5.5**

Property 19: Compliance counts are calculated correctly
*For any* set of exposures with various percentages of capital, the compliant count should equal exposures ≤25% and non-compliant count should equal exposures >25%
**Validates: Requirements 6.1**

Property 20: Non-compliant exposures have warning badges
*For any* exposure exceeding 25% of capital, the HTML rendering should include a red warning badge
**Validates: Requirements 6.2**

Property 21: Concentration risks generate recommendations
*For any* data with concentration risks (high Herfindahl index or sector dominance), the risk analysis section should include specific recommendations
**Validates: Requirements 6.4**

Property 22: HTML report contains footer with timestamp and notice
*For any* generated HTML report, the footer should contain generation timestamp and confidentiality notice
**Validates: Requirements 6.5**

Property 23: XBRL contains LE1 and LE2 templates
*For any* generated XBRL document, the XML should contain elements for both LE1 and LE2 templates
**Validates: Requirements 7.1**

Property 24: XBRL contains all required namespaces
*For any* generated XBRL document, the XML should declare all required EBA COREP namespaces and schema reference
**Validates: Requirements 7.2**

Property 25: XBRL contexts match exposures with all dimensions
*For any* set of exposures, the XBRL should contain one context per exposure with dimensions for counterparty (CP), country (CT), and sector (SC)
**Validates: Requirements 7.3**

Property 26: XBRL LE1 facts are complete
*For any* exposure, the corresponding LE1 facts should include counterparty name, LEI code, identifier type, country code, and sector code
**Validates: Requirements 7.4**

Property 27: XBRL LE2 facts are complete
*For any* exposure, the corresponding LE2 facts should include original exposure amount, exposure after CRM, trading book portion, non-trading book portion, and percentage of eligible capital
**Validates: Requirements 7.5**

Property 28: XBRL validation is performed
*For any* generated XBRL document, validation against EBA XSD schema should be performed
**Validates: Requirements 8.1**

Property 29: XBRL validation errors are logged with details
*For any* XBRL document with validation errors, the errors should be logged with line numbers and error messages
**Validates: Requirements 8.2**

Property 30: Invalid XBRL is saved and status marked PARTIAL
*For any* XBRL document that fails validation, the invalid XML should be saved to filesystem and report status should be PARTIAL
**Validates: Requirements 8.3**

Property 31: Missing LEI uses CONCAT identifier
*For any* exposure without LEI code, the XBRL should use identifier type "CONCAT" with an alternate identifier
**Validates: Requirements 8.4**

Property 32: Valid XBRL is formatted with UTF-8 encoding
*For any* XBRL document that passes validation, the XML should be formatted with pretty-print indentation and UTF-8 encoding
**Validates: Requirements 8.5**

Property 33: S3 uploads use correct bucket paths
*For any* report upload, HTML files should be uploaded to s3://risk-analysis/reports/html/ and XBRL files to s3://risk-analysis/reports/xbrl/
**Validates: Requirements 9.1**

Property 34: File names follow required pattern
*For any* generated report file, the name should match pattern Large_Exposures_{ABI}_{YYYY-MM-DD}.{extension}
**Validates: Requirements 9.2**

Property 35: S3 uploads have correct metadata
*For any* S3 upload, the object should have appropriate Content-Type, AES-256 server-side encryption enabled, and metadata tags
**Validates: Requirements 9.3**

Property 36: Presigned URLs have 1-hour expiration
*For any* generated presigned URL, the expiration should be set to 1 hour from generation time
**Validates: Requirements 9.4, 12.3**

Property 37: S3 upload failures trigger local fallback
*For any* S3 upload that fails after 3 retry attempts, the file should be saved to local filesystem /tmp/deferred-reports/ and retry scheduled
**Validates: Requirements 9.5**

Property 38: Successful generation creates COMPLETED database record
*For any* report generation that completes successfully with both HTML and XBRL, a database record should be inserted with status COMPLETED
**Validates: Requirements 10.1**

Property 39: Partial generation creates PARTIAL database record
*For any* report generation where only one file generates successfully, a database record should be inserted with status PARTIAL
**Validates: Requirements 10.2**

Property 40: Failed generation creates FAILED database record
*For any* report generation that fails completely, a database record should be inserted with status FAILED and failure reason
**Validates: Requirements 10.3**

Property 41: Database records contain all required metadata
*For any* database record, it should include report ID, batch ID, bank ID, reporting date, S3 URIs, file sizes, presigned URLs, and generation timestamps
**Validates: Requirements 10.4**

Property 42: Domain events are raised on completion
*For any* report generation that completes, the GeneratedReport aggregate should raise a ReportGeneratedEvent domain event
**Validates: Requirements 11.1, 12.1**

Property 43: Aggregate and events persist atomically
*For any* GeneratedReport aggregate with domain events, when saved via unit of work, both the aggregate and events should be persisted in the same transaction
**Validates: Requirements 11.3**

Property 44: Outbox events are published
*For any* domain event in the outbox table, when OutboxProcessor runs, the event should be published to the integration event bus
**Validates: Requirements 11.4**

Property 45: Failed event publication triggers retry
*For any* event publication that fails, the OutboxProcessor should retry with exponential backoff up to 5 times
**Validates: Requirements 11.5**

Property 46: Published events contain all required fields
*For any* ReportGeneratedEvent, it should include report ID, batch ID, bank ID, reporting date, and presigned URLs
**Validates: Requirements 12.2**

Property 47: Event publication is logged
*For any* successful event publication, a log entry should be created confirming publication
**Validates: Requirements 12.5**

Property 48: Data fetching completes within performance target
*For any* report generation, data fetching should complete in approximately 500 milliseconds (±200ms tolerance)
**Validates: Requirements 13.1**

Property 49: HTML generation completes within performance target
*For any* HTML report generation, rendering should complete in approximately 1.4 seconds (±400ms tolerance)
**Validates: Requirements 13.2**

Property 50: XBRL generation completes within performance target
*For any* XBRL report generation and validation, the process should complete in approximately 800 milliseconds (±300ms tolerance)
**Validates: Requirements 13.3**

Property 51: S3 uploads complete within performance target
*For any* dual S3 upload (HTML + XBRL), both uploads should complete in approximately 800 milliseconds (±300ms tolerance)
**Validates: Requirements 13.4**

Property 52: End-to-end duration metric is emitted
*For any* report generation, a metric report.generation.duration.seconds should be emitted with the total duration
**Validates: Requirements 13.5**

Property 53: Missing files after retries create FAILED record
*For any* file download that fails after 3 retry attempts due to file not found, a CRITICAL error should be logged and database record created with FAILED status
**Validates: Requirements 14.1**

Property 54: Template rendering failures trigger fallback
*For any* Thymeleaf template rendering failure, a fallback simplified template should be attempted, and if successful, status should be PARTIAL
**Validates: Requirements 14.4**

Property 55: XBRL validation failures trigger automatic corrections
*For any* XBRL validation failure, automatic corrections (trim whitespace, round decimals) should be attempted before marking as PARTIAL
**Validates: Requirements 14.5**

Property 56: Performance timers are emitted
*For any* report generation, performance timers should be emitted for overall duration, data fetch, HTML generation, XBRL generation, S3 upload, and database save
**Validates: Requirements 16.1**

Property 57: Operation counters are emitted
*For any* report generation operation, counters should be emitted for success, failure (with failure_reason tags), partial generation, retries, duplicates, and circuit breaker transitions
**Validates: Requirements 16.2**

Property 58: Resource gauges are emitted
*For any* monitoring interval, gauges should be emitted for database connection pool, async executor queue/threads, deferred upload count, and circuit breaker state
**Validates: Requirements 16.3**

Property 59: Structured logs contain required fields
*For any* log entry, it should use JSON format with fields: timestamp, level, logger, thread, message, batch_id, report_id, bank_id, duration_ms, exception, trace_id
**Validates: Requirements 16.4**

Property 60: Critical conditions trigger appropriate alerts
*For any* critical condition (failure rate >10%, S3 consecutive failures, DB pool exhausted, permission denied), a CRITICAL alert should be triggered
**Validates: Requirements 16.5**

Property 61: Transient S3 failures create retry records
*For any* S3 upload failure due to network timeout or service unavailability, an EventProcessingFailure record should be created for retry
**Validates: Requirements 17.1**

Property 62: Permanent S3 failures skip retry
*For any* S3 upload failure due to permission denied (403) or bucket not found (404), a CRITICAL error should be logged and status marked FAILED without retry
**Validates: Requirements 17.2**

Property 63: Retry uses configured options
*For any* EventRetryProcessor retry, the configured maxRetries and backoffIntervalsSeconds from EventRetryOptions should be used
**Validates: Requirements 17.3**

Property 64: Max retries triggers dead letter handling
*For any* event that reaches maxRetries limit, it should be marked as permanently failed and moved to dead letter handling
**Validates: Requirements 17.4**

Property 65: Successful retry updates status and publishes event
*For any* retry that succeeds, the database status should be updated to COMPLETED, presigned URLs generated, and ReportGeneratedEvent published
**Validates: Requirements 17.5**

Property 66: Database failures leave S3 files intact
*For any* database insert failure after S3 upload, the S3 files should remain (not be deleted) for easier recovery
**Validates: Requirements 18.1**

Property 67: Database failures trigger single retry
*For any* database insert failure, one retry should occur after 2 seconds before marking as permanently failed
**Validates: Requirements 18.2**

Property 68: Permanent database failures create fallback records
*For any* database insert that permanently fails, a fallback record should be created in report_metadata_failures table with S3 URIs
**Validates: Requirements 18.3**

Property 69: Orphaned file cleanup deletes old files
*For any* S3 file without corresponding database record that is older than 7 days, the cleanup job should delete it
**Validates: Requirements 18.4**

Property 70: Reconciliation job processes fallback records
*For any* record in report_metadata_failures table, the reconciliation job should attempt to insert it into report_generation_summaries table
**Validates: Requirements 18.5**

Property 71: Circuit breaker opens on failure threshold
*For any* sequence of S3 operations, if 10 consecutive failures occur OR failure rate exceeds 50% over 5 minutes, the circuit breaker should open
**Validates: Requirements 19.1**

Property 72: Open circuit blocks operations and uses fallback
*For any* S3 operation when circuit breaker is OPEN, the operation should be blocked immediately, file saved to local filesystem, and metric report.s3.circuit.breaker.open emitted
**Validates: Requirements 19.2**

Property 73: Circuit transitions to half-open after wait duration
*For any* circuit breaker that remains OPEN for 5 minutes, it should transition to HALF_OPEN state and allow 1 test operation
**Validates: Requirements 19.3**

Property 74: Successful test operation closes circuit
*For any* test operation in HALF_OPEN state that succeeds, the circuit breaker should close and resume normal operations
**Validates: Requirements 19.4**

Property 75: Circuit close emits metric and processes deferred uploads
*For any* circuit breaker that closes, metric report.s3.circuit.breaker.closed should be emitted and deferred uploads should be processed
**Validates: Requirements 19.5**

Property 76: Completed batches skip regeneration
*For any* batch ID with existing COMPLETED status, triggering report generation should skip regeneration and return existing record
**Validates: Requirements 20.2**

Property 77: Failed batches allow regeneration
*For any* batch ID with FAILED or PARTIAL status, triggering report generation should allow regeneration and overwrite previous record
**Validates: Requirements 20.3**

Property 78: Constraint violations are handled gracefully
*For any* database insert that encounters UNIQUE constraint violation on report_id, the exception should be caught, existing record queried, and processing should proceed without error
**Validates: Requirements 20.5**

Property 79: Health indicators return correct status
*For any* health check execution, the status should be UP when all components healthy, WARN for degraded performance, and DOWN for failures
**Validates: Requirements 21.4**



## Error Handling

### Error Categories and Recovery Strategies

**Transient Errors** (Automatic Retry)
- Network timeouts during S3 operations
- Temporary S3 service unavailability
- Database connection timeouts
- Temporary file system issues

Recovery: EventRetryProcessor with exponential backoff (1s, 2s, 4s, 8s, 16s)

**Permanent Errors** (No Retry)
- S3 permission denied (403)
- S3 bucket not found (404)
- Invalid credentials
- Schema validation errors that cannot be auto-corrected

Recovery: Log CRITICAL error, alert operations team, mark status as FAILED

**Data Quality Errors** (Validation Failures)
- Malformed JSON
- Missing required fields
- Invalid checksums
- Schema violations

Recovery: Save invalid data for analysis, mark status as FAILED, alert data quality team

**Partial Failures** (Degraded Success)
- HTML generated but XBRL validation fails
- XBRL generated but HTML template rendering fails
- One file uploaded but other fails

Recovery: Mark status as PARTIAL, save successfully generated artifacts, alert for manual review

### Circuit Breaker States

```
CLOSED (Normal Operation)
    ↓ (10 consecutive failures OR 50% failure rate)
OPEN (Block Operations, Use Fallback)
    ↓ (After 5 minutes)
HALF_OPEN (Test with 1 Operation)
    ↓ (Success)
CLOSED (Resume Normal Operation)
    ↓ (Failure)
OPEN (Back to Blocking)
```

### Compensating Transactions

**Scenario**: Database insert fails after S3 upload
- **Action**: Leave S3 files intact (do not delete)
- **Reason**: Easier recovery and reconciliation
- **Fallback**: Create record in report_metadata_failures table
- **Reconciliation**: Daily job attempts to insert fallback records into main table

**Scenario**: S3 upload fails after HTML generation
- **Action**: Save HTML to local filesystem /tmp/deferred-reports/
- **Reason**: Preserve generated content for retry
- **Retry**: Scheduled job retries upload when S3 recovers

### Error Logging Standards

All errors must be logged with:
- Severity level (ERROR, WARN, INFO)
- Structured JSON format
- Context fields (batch_id, report_id, bank_id)
- Exception stack trace
- Correlation trace_id for distributed tracing

## Testing Strategy

### Unit Testing

**Framework**: JUnit 5 with Mockito
**Target Coverage**: ≥85% line coverage, ≥75% branch coverage

**Components to Test**:
- Domain entities (GeneratedReport aggregate)
- Value objects (ReportId, ReportStatus, PresignedUrl)
- Domain services (HtmlReportGenerator, XbrlReportGenerator, XbrlValidator)
- Application services (ReportCoordinatorService, ReportGenerationService)
- Event listeners (BatchCalculationCompletedEventListener)
- Repository implementations (JpaGeneratedReportRepository)

**Test Patterns**:
- Mock external dependencies (S3, database, event bus)
- Test business logic in isolation
- Verify domain events are raised correctly
- Test error handling paths
- Verify idempotency logic

### Property-Based Testing

**Framework**: jqwik (Java property-based testing library)
**Configuration**: Minimum 100 iterations per property test

**Key Properties to Test**:
- Property 2: Duplicate event detection (generate random events, send duplicates)
- Property 19: Compliance count calculation (generate random exposures with various percentages)
- Property 34: File name pattern validation (generate random bank IDs and dates)
- Property 71: Circuit breaker threshold (generate random failure sequences)
- Property 76: Idempotency for completed batches (generate random batch states)

**Generator Strategies**:
- Smart generators that constrain to valid input space
- Generate edge cases (empty lists, boundary values, null optionals)
- Generate realistic domain objects (valid LEI codes, proper date ranges)

### Integration Testing

**Framework**: Spring Boot Test with Testcontainers
**Containers**: PostgreSQL, LocalStack (S3 emulation)

**Test Scenarios**:
1. **Happy Path**: Both events arrive, report generates successfully, files uploaded, metadata saved
2. **Reverse Event Order**: Quality event arrives before calculation event
3. **Duplicate Events**: Same event arrives multiple times
4. **S3 Failure**: S3 unavailable, fallback to local filesystem
5. **Database Failure**: DB insert fails, compensating transaction creates fallback record
6. **Partial Generation**: HTML succeeds but XBRL validation fails
7. **Circuit Breaker**: Multiple S3 failures trigger circuit open
8. **Retry Success**: Failed event retries and succeeds
9. **Stale Event**: Event older than 24 hours is rejected

**Verification**:
- Database state matches expected
- S3 objects exist with correct metadata
- Domain events published to outbox
- Metrics emitted correctly
- Logs contain expected entries

### Health Check Testing

**Endpoints**:
- `/actuator/health/liveness` - Basic application liveness
- `/actuator/health/readiness` - Ready to accept traffic

**Test Scenarios**:
- All components healthy → UP
- Database slow but responsive → WARN
- S3 slow response times → WARN
- Database timeout → DOWN
- S3 inaccessible → DOWN
- Async executor queue full → DOWN
- >50 pending events older than 5 minutes → DOWN

### End-to-End Testing

**Scope**: Complete pipeline from event publication to file download

**Steps**:
1. Publish BatchCalculationCompletedEvent to event bus
2. Publish BatchQualityCompletedEvent to event bus
3. Wait for report generation to complete
4. Verify database record exists with COMPLETED status
5. Download HTML from presigned URL
6. Download XBRL from presigned URL
7. Manually verify HTML renders correctly in browser
8. Validate XBRL against EBA XSD schema
9. Verify ReportGeneratedEvent published to outbox

## Design Decisions and Rationales

### Decision 0: "Tell, Don't Ask" Principle

**Decision**: Design domain objects with behavior methods that encapsulate business logic rather than exposing state for external manipulation

**Rationale**:
- GeneratedReport aggregate exposes methods like `recordHtmlGeneration()` and `markAsCompleted()` rather than setters
- Business logic (e.g., checking if both reports are complete) is encapsulated within the aggregate
- External code tells the object what to do, not how to do it
- Reduces coupling and makes business rules explicit and testable

**Example**:
```java
// BAD: Asking and manipulating
if (report.getHtmlMetadata() != null && report.getXbrlMetadata() != null) {
    report.setStatus(ReportStatus.COMPLETED);
    report.setCompletedAt(Instant.now());
    eventPublisher.publish(new ReportGeneratedEvent(report));
}

// GOOD: Telling
report.recordHtmlGeneration(s3Uri, fileSize, presignedUrl);
report.recordXbrlGeneration(s3Uri, fileSize, presignedUrl);
// Aggregate automatically marks as completed and raises event
```

**Trade-offs**:
- Requires more thoughtful design of aggregate methods
- May result in more methods on aggregates
- Benefits: Encapsulation, maintainability, testability, business logic clarity

### Decision 1: Dual Event Coordination

**Decision**: Wait for both BatchCalculationCompletedEvent and BatchQualityCompletedEvent before generating reports

**Rationale**: 
- Reports should only be generated when both calculation and quality validation are complete
- Prevents generating reports from incomplete or invalid data
- Coordinator service maintains in-memory tracking of pending events
- Thread-safe with ConcurrentHashMap for concurrent event arrival

**Trade-offs**:
- Adds complexity with event coordination logic
- Requires memory for tracking pending events
- Benefits: Ensures data quality and completeness

### Decision 2: Transactional Outbox Pattern

**Decision**: Use transactional outbox for publishing ReportGeneratedEvent

**Rationale**:
- Ensures reliable event publication with eventual consistency
- Domain events saved atomically with aggregate in same transaction
- OutboxProcessor handles retry and eventual delivery
- Prevents lost events due to publication failures

**Trade-offs**:
- Adds latency (events published asynchronously by scheduled job)
- Requires outbox table and processor infrastructure
- Benefits: Guarantees event delivery, prevents dual-write problem

### Decision 3: Circuit Breaker for S3 Operations

**Decision**: Implement circuit breaker pattern with Resilience4j for S3 operations

**Rationale**:
- Prevents cascading failures when S3 service is degraded
- Fast-fail behavior reduces resource consumption
- Automatic recovery with half-open state testing
- Fallback to local filesystem preserves generated reports

**Trade-offs**:
- Adds complexity with state management
- Requires monitoring and alerting for circuit state
- Benefits: System resilience, graceful degradation

### Decision 4: Leave S3 Files on Database Failure

**Decision**: Do not delete S3 files when database insert fails

**Rationale**:
- Easier recovery and reconciliation
- Avoids data loss of successfully generated reports
- Fallback table (report_metadata_failures) tracks orphaned files
- Daily reconciliation job attempts to insert fallback records

**Trade-offs**:
- Potential for orphaned S3 files
- Requires cleanup job for old orphaned files
- Benefits: Data preservation, simpler recovery

### Decision 5: Thymeleaf for HTML Generation

**Decision**: Use Thymeleaf template engine for HTML report generation

**Rationale**:
- Server-side rendering with strong Java integration
- Natural templating allows preview in browser
- Supports complex expressions and conditionals
- Easy integration with Spring Boot
- Chart.js for interactive visualizations

**Trade-offs**:
- Server-side rendering adds processing time
- Template complexity can grow
- Benefits: Professional reports, maintainable templates

### Decision 6: XBRL Generation with DOM API

**Decision**: Generate XBRL using Java DOM API rather than templating

**Rationale**:
- XBRL requires precise XML structure with namespaces
- DOM API provides programmatic control over XML generation
- Easier to validate against XSD schema
- Type-safe construction of XML elements

**Trade-offs**:
- More verbose code compared to templates
- Requires XML expertise
- Benefits: Correctness, validation, maintainability

### Decision 7: Presigned URLs with 1-Hour Expiration

**Decision**: Generate presigned URLs with 1-hour expiration for report downloads

**Rationale**:
- Provides temporary authenticated access without AWS credentials
- 1-hour window sufficient for immediate download
- Reduces security risk of long-lived URLs
- Users can request new URLs if expired

**Trade-offs**:
- URLs expire quickly, may inconvenience users
- Requires regeneration for later access
- Benefits: Security, access control

### Decision 8: Async Processing with Dedicated Thread Pool

**Decision**: Use @Async with named executor for event processing

**Rationale**:
- Non-blocking event handling improves throughput
- Dedicated thread pool isolates report generation from other operations
- Configurable pool size for resource management
- Prevents blocking main application threads

**Trade-offs**:
- Adds complexity with async error handling
- Requires monitoring of thread pool health
- Benefits: Scalability, responsiveness

### Decision 9: Property-Based Testing with jqwik

**Decision**: Use jqwik for property-based testing of critical properties

**Rationale**:
- Discovers edge cases through random input generation
- Validates universal properties across input space
- Complements example-based unit tests
- Minimum 100 iterations provides good coverage

**Trade-offs**:
- Longer test execution time
- Requires thoughtful property design
- Benefits: Higher confidence, edge case discovery

### Decision 10: Structured JSON Logging

**Decision**: Use JSON structured logging with standard fields

**Rationale**:
- Machine-parseable logs for log aggregation systems
- Consistent field names across all modules
- Supports distributed tracing with trace_id
- Easier querying and analysis

**Trade-offs**:
- Less human-readable in raw form
- Requires log parsing tools
- Benefits: Observability, troubleshooting, analytics

