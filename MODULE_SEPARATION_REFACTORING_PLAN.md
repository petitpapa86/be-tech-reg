# Module Separation Refactoring Plan
## Data Quality vs Report Generation Architectural Separation

> **Objective**: Enforce clear separation of concerns where **data-quality** owns all data processing and storage, while **report-generation** focuses solely on reading processed data and building/formatting reports.

---

## ⚠️ **CRITICAL: Shared Code Problem Identified**

Both **data-quality** and **report-generation** modules have **duplicate storage code** that could cause bugs:
- Both have their own S3/local storage implementations
- Both parse JSON from storage locations
- Both handle file URIs and S3 paths
- Changes to one module won't reflect in the other → **BUG RISK**

**Solution**: Extract shared storage logic to **regtech-core** before proceeding with module separation.

---

## Table of Contents
1. [Current State Analysis](#current-state-analysis)
2. [Shared Code Problem](#shared-code-problem)
3. [Target Architecture](#target-architecture)
4. [Code to Relocate](#code-to-relocate)
5. [Step-by-Step Migration Plan](#step-by-step-migration-plan)
6. [Interface Changes](#interface-changes)
7. [Event Flow Updates](#event-flow-updates)
8. [Testing Strategy](#testing-strategy)
9. [Rollback Plan](#rollback-plan)

---

## Shared Code Problem

### ❌ **Problem: Duplicate Storage Code Across Modules**

Both modules currently have their own implementations of storage access, which violates DRY (Don't Repeat Yourself) principle:

#### **report-generation** Storage Implementation:
```
regtech-report-generation/domain/storage/
├── IReportStorageService.java              ❌ Module-specific interface
└── (implementation in infrastructure/)
    ├── S3ReportStorageService.java         ❌ Duplicate S3 logic
    ├── LocalFileStorageService.java        ❌ Duplicate local file logic
    └── Uses CoreS3Service (good!)
```

#### **data-quality** Storage Implementation:
```
regtech-data-quality/infrastructure/reporting/
├── LocalDetailedResultsReader.java         ❌ Duplicate local file logic
└── (likely has S3 logic too)
```

#### **Risk of Bugs**:
1. ✅ **CoreS3Service** exists in `regtech-core` - **GOOD!** Already extracted
2. ❌ File parsing logic (JSON) duplicated across modules
3. ❌ URI handling logic duplicated
4. ❌ Storage path configuration duplicated
5. ❌ Error handling for storage failures duplicated

If bug is fixed in one module's storage code, it won't automatically fix in the other module.

### ✅ **Solution: Extract Common Storage Logic to regtech-core**

**Phase 0** (New): Before module separation, extract shared storage logic to `regtech-core`:

```
regtech-core/infrastructure/storage/
├── IStorageService.java                     ✅ Generic storage interface
├── StorageServiceAdapter.java               ✅ Unified S3/local implementation
├── StorageUri.java                          ✅ URI parsing/handling
├── JsonStorageHelper.java                   ✅ JSON parsing utilities
└── StorageConfiguration.java                ✅ Shared configuration
```

**Benefits**:
- ✅ Single source of truth for storage operations
- ✅ Bug fixes apply to all modules
- ✅ Consistent error handling
- ✅ Easier to test and maintain
- ✅ Can add new storage backends (Azure Blob, GCS) once

---

## Current State Analysis

### ❌ Problems Identified

#### 1. **Duplicate Storage Code** (CRITICAL - Must Fix First)

**Location**: Both `regtech-data-quality` and `regtech-report-generation`

**Duplicate Code**:
- S3 file fetching/uploading
- Local filesystem operations
- JSON parsing from storage
- URI handling (s3://, file://)
- Storage configuration

**Impact**: Changes to storage logic must be made in multiple places, increasing bug risk.

#### 2. **report-generation** Contains Data Processing Logic

**Location**: `regtech-report-generation/application/src/main/java/com/bcbs239/regtech/reportgeneration/application/`

**Files with Processing Responsibilities**:

```
generation/
├── ComprehensiveReportDataAggregator.java    ❌ Fetches, aggregates, validates data from S3/local storage
├── DataAggregationException.java             ❌ Processing-specific exception
└── ComprehensiveReportData.java              ⚠️ Data structure (may be OK here)

ingestionbatch/
├── ProcessDataQualityCompletedUseCase.java   ❌ Processes quality events (coordination, not report building)
└── ProcessRiskCalculationCompletedUseCase.java ❌ Processes calculation events (coordination, not report building)

coordination/
├── BatchEventTracker.java                    ❌ Tracks batch state (coordination logic)
├── ReportCoordinator.java                    ❌ Coordinates data availability (orchestration, not formatting)
├── CalculationEventData.java                 ⚠️ DTO (may be OK here)
└── QualityEventData.java                     ⚠️ DTO (may be OK here)
```

**Key Issue**: `ComprehensiveReportDataAggregator` performs:
- **Data fetching** from S3/local storage (should be in data-quality)
- **Data aggregation** (combining calculation + quality results)
- **Data validation** (`validateDataConsistency()`)
- **Data transformation** (parsing JSON from storage)

**Current Flow** (Incorrect):
```
risk-calculation → RiskCalculationCompletedEvent
                    ↓
data-quality → DataQualityCompletedEvent
                    ↓
report-generation → ProcessRiskCalculationCompletedUseCase
                    → ProcessDataQualityCompletedUseCase
                    → ReportCoordinator.handleCalculationCompleted()
                    → ReportCoordinator.handleQualityCompleted()
                    → ComprehensiveReportDataAggregator.fetchAllData()  ❌ DATA PROCESSING
                    → Fetch from S3/local storage
                    → Aggregate data
                    → Validate consistency
                    → Generate report
```

---

## Target Architecture

### ✅ Correct Separation of Concerns

#### **data-quality** Responsibilities (Processing & Storage)
1. **Validate** incoming data against business rules
2. **Process** validated data (transformations, aggregations)
3. **Store** processed results to S3/local filesystem
4. **Publish** integration event with storage location
5. **Manage** quality reports and scores

#### **report-generation** Responsibilities (Building & Formatting Only)
1. **Listen** for data availability events
2. **Read** pre-processed data from storage locations provided in events
3. **Build** BCBS 239 reports using processed data
4. **Format** reports (PDF, Excel, JSON)
5. **Store** generated reports (not processed data)

### ✅ Target Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                         DATA PROCESSING PHASE                           │
│                      (data-quality module owns this)                    │
└────────────────────────────────────────────────────────────────────────┘
                                    │
risk-calculation → RiskCalculationCompletedEvent
                   (resultFileUri: s3://calculations/batch123.json)
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ data-quality module:                                                     │
│                                                                          │
│ 1. ProcessRiskCalculationCompletedHandler                               │
│    ├─ Fetch calculation results from resultFileUri                     │
│    ├─ Validate against business rules                                  │
│    ├─ Calculate quality metrics                                        │
│    ├─ Aggregate with quality validation results                        │
│    ├─ Store processed data to S3/local: s3://processed/batch123.json  │
│    └─ Publish: ProcessedDataReadyEvent                                 │
│                                                                          │
│ 2. ProcessedDataReadyEvent contains:                                    │
│    {                                                                     │
│      "batchId": "batch123",                                             │
│      "processedDataUri": "s3://processed/batch123.json",               │
│      "qualityScore": 0.95,                                              │
│      "qualityGrade": "EXCELLENT",                                       │
│      "totalExposures": 1500,                                            │
│      "totalAmountEur": 1500000.00,                                      │
│      "processedAt": "2024-01-15T10:00:00Z"                             │
│    }                                                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         REPORT BUILDING PHASE                            │
│                   (report-generation module owns this)                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ report-generation module:                                                │
│                                                                          │
│ 1. ProcessedDataReadyHandler                                            │
│    ├─ Receive ProcessedDataReadyEvent                                  │
│    ├─ Read pre-processed data from processedDataUri                    │
│    ├─ Build BCBS 239 report structure                                  │
│    ├─ Format as PDF/Excel/JSON                                         │
│    └─ Store generated report: s3://reports/batch123-report.pdf        │
│                                                                          │
│ NO DATA PROCESSING - Only report building & formatting                  │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Changes**:
1. **data-quality** receives raw calculation results, processes them, stores processed data
2. **data-quality** publishes `ProcessedDataReadyEvent` with storage URI
3. **report-generation** receives event, reads processed data, builds reports
4. **No data processing** in report-generation module

---

## Code to Relocate

### Phase 1: Move Data Aggregation Logic

#### From: `report-generation/application/generation/`
#### To: `data-quality/application/processing/`

| Source File | Target Location | Purpose |
|-------------|-----------------|---------|
| `ComprehensiveReportDataAggregator.java` | `data-quality/application/processing/ProcessedDataAggregator.java` | Aggregates calculation + quality results |
| `DataAggregationException.java` | `data-quality/application/processing/DataProcessingException.java` | Processing error handling |
| `ComprehensiveReportData.java` | `data-quality/domain/processed/ProcessedBatchData.java` | Domain model for processed data |

**Refactoring Changes**:
```java
// OLD: report-generation/application/generation/ComprehensiveReportDataAggregator.java
@Service
public class ComprehensiveReportDataAggregator {
    private final IReportStorageService reportStorageService;
    
    public ComprehensiveReportData fetchAllData(
        CalculationEventData calculationEvent,
        QualityEventData qualityEvent
    ) {
        // Fetch from S3/local
        // Aggregate
        // Validate
        // Return
    }
}

// NEW: data-quality/application/processing/ProcessedDataAggregator.java
@Service
public class ProcessedDataAggregator {
    private final IDataStorageService dataStorageService;  // Renamed from IReportStorageService
    
    public ProcessedBatchData processAndStore(
        RiskCalculationCompletedInboundEvent calculationEvent,
        ValidationResults qualityResults
    ) {
        // 1. Fetch calculation results from calculationEvent.getResultFileUri()
        // 2. Fetch quality validation results (already in data-quality)
        // 3. Aggregate both datasets
        // 4. Validate consistency
        // 5. Store processed data to S3/local
        // 6. Return ProcessedBatchData with storage URI
    }
}
```

---

### Phase 2: Relocate Event Processing Use Cases

#### From: `report-generation/application/ingestionbatch/`
#### To: `data-quality/application/processing/`

| Source File | Target Location | New Name | Purpose |
|-------------|-----------------|----------|---------|
| `ProcessRiskCalculationCompletedUseCase.java` | `data-quality/application/processing/ProcessCalculationResultsUseCase.java` | New | Process calculation results, aggregate with quality data |

**Delete** (no longer needed):
- `ProcessDataQualityCompletedUseCase.java` - data-quality already has internal handlers

**Refactoring**:
```java
// NEW: data-quality/application/processing/ProcessCalculationResultsUseCase.java
@Component("ProcessCalculationResultsUseCase")
@RequiredArgsConstructor
public class ProcessCalculationResultsUseCase {
    private final ProcessedDataAggregator aggregator;
    private final IIntegrationEventBus eventBus;
    
    @Async("calculationProcessingExecutor")
    public void process(RiskCalculationCompletedInboundEvent event) {
        // 1. Fetch calculation results from event.getResultFileUri()
        // 2. Fetch quality validation results from database/storage
        // 3. Aggregate using ProcessedDataAggregator
        // 4. Store processed data
        // 5. Publish ProcessedDataReadyEvent to report-generation
    }
}
```

---

### Phase 3: Simplify Report Coordinator

#### Update: `report-generation/application/coordination/ReportCoordinator.java`

**Current Responsibilities** (Too Many):
- Tracks batch events (calculation + quality completion)
- Coordinates when both events arrive
- Triggers data aggregation
- Triggers report generation

**New Responsibilities** (Simplified):
- Receives `ProcessedDataReadyEvent`
- Triggers report building (no coordination needed)

**Refactoring**:
```java
// BEFORE: Complex coordination
@Service
public class ReportCoordinator {
    private final BatchEventTracker tracker;  // Delete this
    private final ComprehensiveReportDataAggregator aggregator;  // Delete this
    
    public void handleCalculationCompleted(CalculationEventData data) {
        tracker.recordCalculationCompleted(data);
        checkAndGenerateReport();
    }
    
    public void handleQualityCompleted(QualityEventData data) {
        tracker.recordQualityCompleted(data);
        checkAndGenerateReport();
    }
    
    private void checkAndGenerateReport() {
        if (tracker.isBatchReady()) {
            // Aggregate data
            // Generate report
        }
    }
}

// AFTER: Simple event handling
@Service
public class ReportBuilder {
    private final IDataStorageService dataStorage;
    private final IReportGenerator reportGenerator;
    
    @EventListener
    public void onProcessedDataReady(ProcessedDataReadyEvent event) {
        // 1. Read processed data from event.getProcessedDataUri()
        // 2. Build BCBS 239 report
        // 3. Format as PDF/Excel
        // 4. Store report
        // 5. Publish ReportGeneratedEvent
    }
}
```

**Delete** (no longer needed):
- `BatchEventTracker.java` - coordination moved to data-quality
- `CalculationEventData.java` - replaced by ProcessedDataReadyEvent
- `QualityEventData.java` - replaced by ProcessedDataReadyEvent

---

## Step-by-Step Migration Plan

### Prerequisites
- [ ] Ensure all tests pass before starting
- [ ] Create feature branch: `feature/module-separation-refactoring`
- [ ] Backup database and storage
- [ ] Review all integration event flows
- [ ] **Verify CoreS3Service exists in regtech-core** ✅ (Already present)

---

### **Phase 0: Extract Shared Storage Logic to regtech-core** (Day 1) **⚠️ DO THIS FIRST**

**Objective**: Create unified storage service that both data-quality and report-generation will use.

#### Step 0.1: Create Generic Storage Interface

**File**: `regtech-core/domain/storage/IStorageService.java`

```java
package com.bcbs239.regtech.core.domain.storage;

import java.util.Map;
import java.util.Optional;

/**
 * Generic storage service interface for all modules.
 * Abstracts S3, local filesystem, Azure Blob, etc.
 */
public interface IStorageService {
    
    /**
     * Store data to storage backend
     * @param path Storage path (e.g., "processed-data/batch-123.json")
     * @param content Data content as string
     * @param metadata Optional metadata tags
     * @return Full storage URI (e.g., "s3://bucket/processed-data/batch-123.json")
     */
    String store(String path, String content, Map<String, String> metadata);
    
    /**
     * Load data from storage backend
     * @param storageUri Full storage URI or relative path
     * @return Data content as string
     */
    String load(String storageUri);
    
    /**
     * Check if data exists at storage location
     * @param storageUri Full storage URI or relative path
     * @return true if exists, false otherwise
     */
    boolean exists(String storageUri);
    
    /**
     * Delete data from storage
     * @param storageUri Full storage URI or relative path
     * @return true if deleted, false if not found
     */
    boolean delete(String storageUri);
    
    /**
     * Generate presigned URL for download (S3 only)
     * @param storageUri Full storage URI
     * @param expirationSeconds URL expiration time
     * @return Presigned URL or empty if not supported
     */
    Optional<String> generatePresignedUrl(String storageUri, long expirationSeconds);
}
```

#### Step 0.2: Create Storage URI Value Object

**File**: `regtech-core/domain/storage/StorageUri.java`

```java
package com.bcbs239.regtech.core.domain.storage;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Value object representing a storage location URI.
 * Supports: s3://bucket/key, file:///path/to/file, or relative paths
 */
public record StorageUri(String uri) {
    
    private static final Pattern S3_PATTERN = Pattern.compile("^s3://([^/]+)/(.+)$");
    private static final Pattern FILE_PATTERN = Pattern.compile("^file:///(.+)$");
    
    public StorageUri {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Storage URI cannot be blank");
        }
    }
    
    public boolean isS3Uri() {
        return uri.startsWith("s3://");
    }
    
    public boolean isFileUri() {
        return uri.startsWith("file:///");
    }
    
    public boolean isRelativePath() {
        return !isS3Uri() && !isFileUri();
    }
    
    public String extractBucket() {
        if (!isS3Uri()) {
            throw new IllegalStateException("Not an S3 URI");
        }
        var matcher = S3_PATTERN.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }
    
    public String extractKey() {
        if (!isS3Uri()) {
            throw new IllegalStateException("Not an S3 URI");
        }
        var matcher = S3_PATTERN.matcher(uri);
        return matcher.matches() ? matcher.group(2) : null;
    }
    
    public String extractFilePath() {
        if (!isFileUri()) {
            throw new IllegalStateException("Not a file URI");
        }
        var matcher = FILE_PATTERN.matcher(uri);
        return matcher.matches() ? matcher.group(1) : uri.substring(8); // Skip "file:///"
    }
    
    public static StorageUri of(String uri) {
        return new StorageUri(uri);
    }
}
```

#### Step 0.3: Implement Unified Storage Adapter

**File**: `regtech-core/infrastructure/storage/StorageServiceAdapter.java`

```java
package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Unified storage service supporting S3 and local filesystem.
 * Used by data-quality, report-generation, and other modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceAdapter implements IStorageService {
    
    private final CoreS3Service coreS3Service;
    
    @Value("${regtech.storage.type:local}")
    private String storageType;  // "s3" or "local"
    
    @Value("${regtech.storage.local.base-path:./storage}")
    private String localBasePath;
    
    @Value("${regtech.storage.s3.bucket:}")
    private String s3Bucket;
    
    @Override
    public String store(String path, String content, Map<String, String> metadata) {
        if ("s3".equalsIgnoreCase(storageType)) {
            return storeToS3(path, content, metadata);
        } else {
            return storeLocally(path, content);
        }
    }
    
    @Override
    public String load(String storageUri) {
        StorageUri uri = StorageUri.of(storageUri);
        
        if (uri.isS3Uri()) {
            return loadFromS3(uri);
        } else if (uri.isFileUri()) {
            return loadFromFile(uri);
        } else {
            // Relative path - check configured storage type
            if ("s3".equalsIgnoreCase(storageType)) {
                return loadFromS3(StorageUri.of("s3://" + s3Bucket + "/" + storageUri));
            } else {
                return loadFromFile(StorageUri.of("file:///" + localBasePath + "/" + storageUri));
            }
        }
    }
    
    @Override
    public boolean exists(String storageUri) {
        StorageUri uri = StorageUri.of(storageUri);
        
        if (uri.isS3Uri()) {
            return coreS3Service.objectExists(uri.extractBucket(), uri.extractKey());
        } else if (uri.isFileUri()) {
            return Files.exists(Paths.get(uri.extractFilePath()));
        } else {
            Path path = Paths.get(localBasePath, storageUri);
            return Files.exists(path);
        }
    }
    
    @Override
    public boolean delete(String storageUri) {
        StorageUri uri = StorageUri.of(storageUri);
        
        try {
            if (uri.isS3Uri()) {
                coreS3Service.deleteObject(uri.extractBucket(), uri.extractKey());
                return true;
            } else if (uri.isFileUri()) {
                return Files.deleteIfExists(Paths.get(uri.extractFilePath()));
            } else {
                Path path = Paths.get(localBasePath, storageUri);
                return Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.error("Failed to delete storage: {}", storageUri, e);
            return false;
        }
    }
    
    @Override
    public Optional<String> generatePresignedUrl(String storageUri, long expirationSeconds) {
        StorageUri uri = StorageUri.of(storageUri);
        
        if (!uri.isS3Uri()) {
            return Optional.empty();  // Presigned URLs only for S3
        }
        
        try {
            String presignedUrl = coreS3Service.generatePresignedUrl(
                uri.extractBucket(),
                uri.extractKey(),
                expirationSeconds
            );
            return Optional.of(presignedUrl);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", storageUri, e);
            return Optional.empty();
        }
    }
    
    private String storeToS3(String path, String content, Map<String, String> metadata) {
        String key = path.startsWith("/") ? path.substring(1) : path;
        coreS3Service.uploadString(s3Bucket, key, content, metadata);
        return "s3://" + s3Bucket + "/" + key;
    }
    
    private String storeLocally(String path, String content) {
        try {
            Path filePath = Paths.get(localBasePath, path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            return "file:///" + filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file locally: " + path, e);
        }
    }
    
    private String loadFromS3(StorageUri uri) {
        return coreS3Service.downloadAsString(uri.extractBucket(), uri.extractKey());
    }
    
    private String loadFromFile(StorageUri uri) {
        try {
            return Files.readString(Paths.get(uri.extractFilePath()));
        } catch (IOException e) {
            throw new StorageException("Failed to load file: " + uri.uri(), e);
        }
    }
}
```

#### Step 0.4: Update Module Dependencies

**Update**: `regtech-data-quality/pom.xml` and `regtech-report-generation/pom.xml`

```xml
<dependencies>
    <!-- Add dependency on regtech-core for shared storage -->
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-infrastructure</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

#### Step 0.5: Update Configuration

**Update**: `application.yml` (shared configuration)

```yaml
regtech:
  storage:
    type: ${STORAGE_TYPE:local}  # or 's3'
    local:
      base-path: ${STORAGE_LOCAL_PATH:./storage}
    s3:
      bucket: ${STORAGE_S3_BUCKET:regtech-data}
      region: ${STORAGE_S3_REGION:us-east-1}
```

#### Step 0.6: Replace Module-Specific Storage with Shared Service

**In data-quality**: Replace `LocalDetailedResultsReader` with `IStorageService`  
**In report-generation**: Replace `IReportStorageService` usage with `IStorageService` for data loading (keep report-specific upload methods)

**Testing**:
- [ ] Unit tests for `StorageUri` value object
- [ ] Unit tests for `StorageServiceAdapter` (mocked CoreS3Service)
- [ ] Integration tests with local filesystem
- [ ] Integration tests with S3 (Localstack)

**Time Estimate**: 6-8 hours

---

### Step 1: Create New Data Structures in data-quality (Day 2)

**Tasks**:
1. Create `data-quality/domain/processed/` package
2. Create `ProcessedBatchData.java` domain entity
3. Create `IDataStorageService.java` port interface
4. Create `ProcessedDataReadyEvent.java` integration event

**Files to Create**:

```java
// data-quality/domain/processed/ProcessedBatchData.java
public class ProcessedBatchData extends AggregateRoot<ProcessedBatchDataId> {
    private final ProcessedBatchDataId id;
    private final BatchId batchId;
    private final BankId bankId;
    private final String processedDataUri;  // S3/local storage location
    private final BigDecimal qualityScore;
    private final String qualityGrade;
    private final int totalExposures;
    private final BigDecimal totalAmountEur;
    private final Instant processedAt;
    
    // Factory method
    public static ProcessedBatchData create(...) { ... }
    
    // Domain methods
    public void validate() { ... }
}

// data-quality/domain/storage/IDataStorageService.java (port)
public interface IDataStorageService {
    String storeProcessedData(ProcessedBatchData data);
    ProcessedBatchData loadProcessedData(String storageUri);
}

// core/domain/events/integration/ProcessedDataReadyEvent.java
public class ProcessedDataReadyEvent implements IIntegrationEvent {
    private String batchId;
    private String bankId;
    private String processedDataUri;  // Key: location of processed data
    private BigDecimal qualityScore;
    private String qualityGrade;
    private int totalExposures;
    private BigDecimal totalAmountEur;
    private Instant processedAt;
}
```

**Testing**:
- [ ] Unit tests for `ProcessedBatchData` domain entity
- [ ] Validation logic tests

**Time Estimate**: 4 hours

---

### Step 2: Implement Storage Adapter in data-quality (Day 1-2)

**Tasks**:
1. Create `data-quality/infrastructure/storage/` package
2. Implement `DataStorageServiceAdapter.java` (S3/local storage)
3. Add configuration for storage paths

**Files to Create**:

```java
// data-quality/infrastructure/storage/DataStorageServiceAdapter.java
@Service
public class DataStorageServiceAdapter implements IDataStorageService {
    private final S3Client s3Client;  // For production
    private final ObjectMapper objectMapper;
    private final String storagePath;  // From application.yml
    
    @Override
    public String storeProcessedData(ProcessedBatchData data) {
        String json = objectMapper.writeValueAsString(data);
        
        if (useS3Storage) {
            // Upload to S3: s3://processed-data/batch-{id}.json
            return s3Uri;
        } else {
            // Store locally: /var/regtech/processed-data/batch-{id}.json
            return fileUri;
        }
    }
    
    @Override
    public ProcessedBatchData loadProcessedData(String storageUri) {
        String json = fetchFromStorage(storageUri);
        return objectMapper.readValue(json, ProcessedBatchData.class);
    }
}
```

**Configuration** (`application-dataquality.yml`):
```yaml
data-quality:
  storage:
    type: ${STORAGE_TYPE:local}  # or 's3'
    local-path: ${DATA_QUALITY_STORAGE_PATH:./storage/processed-data}
    s3:
      bucket: ${DATA_QUALITY_S3_BUCKET:regtech-processed-data}
      prefix: processed/
```

**Testing**:
- [ ] Integration tests with local filesystem
- [ ] Integration tests with S3 (using Localstack/testcontainers)

**Time Estimate**: 6 hours

---

### Step 3: Copy and Refactor Aggregation Logic (Day 2-3)

**Tasks**:
1. Copy `ComprehensiveReportDataAggregator.java` to `data-quality/application/processing/ProcessedDataAggregator.java`
2. Refactor to use new domain models
3. Update method signatures

**Refactoring Steps**:

```java
// data-quality/application/processing/ProcessedDataAggregator.java
@Service
@RequiredArgsConstructor
public class ProcessedDataAggregator {
    
    private final IDataStorageService dataStorage;
    private final IQualityReportRepository qualityReportRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    /**
     * Process and aggregate calculation + quality results
     * Store processed data to S3/local storage
     * 
     * @param calculationEvent Event with calculation results location
     * @param qualityReport Quality validation results
     * @return ProcessedBatchData with storage URI
     */
    public ProcessedBatchData processAndStore(
            RiskCalculationCompletedInboundEvent calculationEvent,
            QualityReport qualityReport) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // 1. Fetch calculation results from calculationEvent.getResultFileUri()
            CalculationResults calcResults = fetchCalculationResults(calculationEvent);
            
            // 2. Validate data consistency
            validateConsistency(calcResults, qualityReport);
            
            // 3. Create processed data aggregate
            ProcessedBatchData processedData = ProcessedBatchData.create(
                calculationEvent.getBatchId(),
                calculationEvent.getBankId(),
                calcResults.getBankName(),
                calcResults.getReportingDate(),
                qualityReport.getOverallScore(),
                qualityReport.getQualityGrade(),
                calculationEvent.getTotalExposures(),
                calculationEvent.getTotalAmountEur(),
                Instant.now()
            );
            
            // 4. Store processed data to S3/local
            String storageUri = dataStorage.storeProcessedData(processedData);
            processedData.setStorageUri(storageUri);
            
            log.info("Stored processed data at: {}", storageUri);
            meterRegistry.counter("data.processing.success").increment();
            
            return processedData;
            
        } catch (Exception e) {
            log.error("Failed to process batch data: {}", calculationEvent.getBatchId(), e);
            meterRegistry.counter("data.processing.failure").increment();
            throw new DataProcessingException("Failed to process batch data", e);
        } finally {
            sample.stop(Timer.builder("data.processing.duration").register(meterRegistry));
        }
    }
    
    private CalculationResults fetchCalculationResults(RiskCalculationCompletedInboundEvent event) {
        // Fetch from event.getResultFileUri() using dataStorage
        String json = dataStorage.loadFromUri(event.getResultFileUri());
        return objectMapper.readValue(json, CalculationResults.class);
    }
}
```

**Testing**:
- [ ] Unit tests with mocked storage
- [ ] Integration tests with real storage

**Time Estimate**: 8 hours

---

### Step 4: Create Processing Use Case in data-quality (Day 3-4)

**Tasks**:
1. Create `data-quality/application/processing/ProcessCalculationResultsUseCase.java`
2. Integrate with `ProcessedDataAggregator`
3. Publish `ProcessedDataReadyEvent`

**Implementation**:

```java
// data-quality/application/processing/ProcessCalculationResultsUseCase.java
@Component("ProcessCalculationResultsUseCase")
@RequiredArgsConstructor
public class ProcessCalculationResultsUseCase {
    
    private final ProcessedDataAggregator aggregator;
    private final IQualityReportRepository qualityReportRepository;
    private final IIntegrationEventBus eventBus;
    private final IEventProcessingFailureRepository failureRepository;
    
    @EventListener
    @Async("calculationProcessingExecutor")
    public void onRiskCalculationCompleted(RiskCalculationCompletedInboundEvent event) {
        try {
            log.info("Processing calculation results for batch: {}", event.getBatchId());
            
            // 1. Find quality report (must exist in data-quality database)
            QualityReport qualityReport = qualityReportRepository
                .findByBatchId(new BatchId(event.getBatchId()))
                .orElseThrow(() -> new IllegalStateException(
                    "Quality report not found for batch: " + event.getBatchId()));
            
            // 2. Process and store aggregated data
            ProcessedBatchData processedData = aggregator.processAndStore(event, qualityReport);
            
            // 3. Publish ProcessedDataReadyEvent to report-generation
            ProcessedDataReadyEvent readyEvent = new ProcessedDataReadyEvent(
                processedData.getBatchId().value(),
                processedData.getBankId().value(),
                processedData.getProcessedDataUri(),
                processedData.getQualityScore(),
                processedData.getQualityGrade().name(),
                processedData.getTotalExposures(),
                processedData.getTotalAmountEur(),
                processedData.getProcessedAt()
            );
            
            eventBus.publish(readyEvent);
            
            log.info("Published ProcessedDataReadyEvent for batch: {}", event.getBatchId());
            
        } catch (Exception e) {
            log.error("Failed to process calculation results for batch: {}", event.getBatchId(), e);
            recordFailure(event, e);
        }
    }
    
    private void recordFailure(RiskCalculationCompletedInboundEvent event, Exception error) {
        // Record event processing failure for retry/debugging
        // (existing pattern in your codebase)
    }
}
```

**Configuration**:
```yaml
# data-quality/infrastructure/src/main/resources/application-dataquality.yml
spring:
  task:
    execution:
      pool:
        calculation-processing-executor:
          core-size: 2
          max-size: 5
          queue-capacity: 100
```

**Testing**:
- [ ] Unit tests with mocked dependencies
- [ ] Integration tests with TestContainers
- [ ] Event publishing verification

**Time Estimate**: 6 hours

---

### Step 5: Update report-generation to Consume ProcessedDataReadyEvent (Day 4-5)

**Tasks**:
1. Create `ProcessedDataReadyHandler` in report-generation
2. Simplify `ReportCoordinator` → rename to `ReportBuilder`
3. Remove data aggregation logic

**Implementation**:

```java
// report-generation/application/building/ProcessedDataReadyHandler.java
@Component("ProcessedDataReadyHandler")
@RequiredArgsConstructor
public class ProcessedDataReadyHandler {
    
    private final IDataStorageService dataStorage;  // Read-only access
    private final IReportGenerator reportGenerator;
    private final IGeneratedReportRepository reportRepository;
    
    @EventListener
    @Async("reportBuildingExecutor")
    public void onProcessedDataReady(ProcessedDataReadyEvent event) {
        try {
            log.info("Building report for batch: {}", event.getBatchId());
            
            // 1. Read processed data from storage URI (NO processing, just read)
            ProcessedBatchData processedData = dataStorage.loadProcessedData(
                event.getProcessedDataUri());
            
            // 2. Build BCBS 239 report structure
            ReportStructure reportStructure = buildReportStructure(processedData);
            
            // 3. Generate report in multiple formats
            GeneratedReport report = reportGenerator.generate(
                reportStructure,
                List.of(ReportFormat.PDF, ReportFormat.EXCEL)
            );
            
            // 4. Store generated report
            reportRepository.save(report);
            
            log.info("Report generated for batch: {}", event.getBatchId());
            
        } catch (Exception e) {
            log.error("Failed to build report for batch: {}", event.getBatchId(), e);
            recordFailure(event, e);
        }
    }
    
    private ReportStructure buildReportStructure(ProcessedBatchData data) {
        // Build report structure (NO data processing)
        return ReportStructure.builder()
            .batchId(data.getBatchId())
            .bankName(data.getBankName())
            .reportingDate(data.getReportingDate())
            .qualitySection(buildQualitySection(data))
            .calculationSection(buildCalculationSection(data))
            .summarySection(buildSummarySection(data))
            .build();
    }
}
```

**Delete Files**:
- ❌ `report-generation/application/generation/ComprehensiveReportDataAggregator.java`
- ❌ `report-generation/application/generation/DataAggregationException.java`
- ❌ `report-generation/application/ingestionbatch/ProcessDataQualityCompletedUseCase.java`
- ❌ `report-generation/application/ingestionbatch/ProcessRiskCalculationCompletedUseCase.java`
- ❌ `report-generation/application/coordination/BatchEventTracker.java`
- ❌ `report-generation/application/coordination/ReportCoordinator.java` (rename to ReportBuilder)
- ❌ `report-generation/application/coordination/CalculationEventData.java`
- ❌ `report-generation/application/coordination/QualityEventData.java`

**Testing**:
- [ ] Unit tests for ProcessedDataReadyHandler
- [ ] Integration tests with TestContainers
- [ ] End-to-end tests: calculation → quality → processed data → report generation

**Time Estimate**: 8 hours

---

### Step 6: Update Integration Event Listeners (Day 5-6)

**Tasks**:
1. Update inbox processors in report-generation to listen for `ProcessedDataReadyEvent`
2. Remove listeners for `RiskCalculationCompletedInboundEvent` and `DataQualityCompletedInboundEvent`
3. Update event routing configuration

**Changes**:

```java
// REMOVE from report-generation/presentation/src/main/java/.../integration/
// ❌ RiskCalculationCompletedEventListener.java
// ❌ DataQualityCompletedEventListener.java

// ADD to report-generation/presentation/src/main/java/.../integration/
// ✅ ProcessedDataReadyEventListener.java
@Component
@RequiredArgsConstructor
public class ProcessedDataReadyEventListener {
    
    private final ProcessedDataReadyHandler handler;
    
    @EventListener
    public void onEvent(ProcessedDataReadyEvent event) {
        handler.onProcessedDataReady(event);
    }
}
```

**Configuration Updates**:
```yaml
# application.yml - Update event routing
regtech:
  inbox:
    event-routing:
      ProcessedDataReadyEvent: report-generation
      RiskCalculationCompletedInboundEvent: data-quality  # Changed from report-generation
      DataQualityCompletedInboundEvent: data-quality  # Keep in data-quality
```

**Testing**:
- [ ] Event routing tests
- [ ] Integration tests verifying correct module receives events

**Time Estimate**: 4 hours

---

### Step 7: Update Documentation (Day 6)

**Tasks**:
1. Update `.github/copilot-instructions.md` with new architecture
2. Update `CLEAN_ARCH_GUIDE.md` with concrete examples
3. Create `MODULE_RESPONSIBILITIES.md` documenting boundaries
4. Update API documentation

**Documentation Updates**:

```markdown
# MODULE_RESPONSIBILITIES.md

## Data Quality Module

### Responsibilities
- ✅ Validate data against business rules
- ✅ Process validated data (aggregations, transformations)
- ✅ Store processed data to S3/local storage
- ✅ Publish ProcessedDataReadyEvent with storage URI
- ✅ Manage quality reports and scores

### Key Classes
- `ProcessedDataAggregator` - Aggregates calculation + quality data
- `ProcessCalculationResultsUseCase` - Processes calculation results
- `DataStorageServiceAdapter` - Stores processed data

### Integration Events Published
- `ProcessedDataReadyEvent` → report-generation

### Integration Events Consumed
- `RiskCalculationCompletedInboundEvent` ← risk-calculation
- (Internal quality validation events)

---

## Report Generation Module

### Responsibilities
- ✅ Listen for ProcessedDataReadyEvent
- ✅ Read pre-processed data from storage
- ✅ Build BCBS 239 report structure
- ✅ Format reports (PDF, Excel, JSON)
- ✅ Store generated reports

### Key Classes
- `ProcessedDataReadyHandler` - Handles processed data events
- `ReportBuilder` - Builds report structure
- `ReportGenerator` - Generates formatted reports

### Integration Events Published
- `ReportGeneratedEvent` → (notification systems)

### Integration Events Consumed
- `ProcessedDataReadyEvent` ← data-quality

### What NOT to Do
- ❌ NO data processing/aggregation
- ❌ NO data validation (beyond report formatting)
- ❌ NO direct consumption of RiskCalculationCompletedEvent
- ❌ NO direct consumption of DataQualityCompletedEvent
```

**Time Estimate**: 4 hours

---

### Step 8: Integration Testing & Validation (Day 7)

**Tasks**:
1. End-to-end integration tests
2. Performance testing
3. Error handling validation
4. Rollback testing

**Test Scenarios**:

```java
@SpringBootTest
@Testcontainers
class ModuleSeparationIntegrationTest {
    
    @Test
    void shouldProcessDataInDataQualityAndGenerateReportInReportGeneration() {
        // 1. Trigger risk calculation completed event
        RiskCalculationCompletedInboundEvent calcEvent = createCalculationEvent();
        eventBus.publish(calcEvent);
        
        // 2. Wait for data-quality to process and publish ProcessedDataReadyEvent
        await().atMost(10, SECONDS).until(() -> 
            processedDataReadyEventReceived(calcEvent.getBatchId()));
        
        // 3. Verify processed data stored in data-quality storage
        ProcessedBatchData processedData = dataStorage.loadProcessedData(
            getStorageUri(calcEvent.getBatchId()));
        assertThat(processedData).isNotNull();
        assertThat(processedData.getQualityScore()).isGreaterThan(BigDecimal.ZERO);
        
        // 4. Wait for report-generation to build report
        await().atMost(15, SECONDS).until(() -> 
            reportGenerated(calcEvent.getBatchId()));
        
        // 5. Verify report generated
        GeneratedReport report = reportRepository.findByBatchId(calcEvent.getBatchId());
        assertThat(report).isNotNull();
        assertThat(report.getReportFormat()).contains(ReportFormat.PDF);
    }
    
    @Test
    void shouldNotAllowReportGenerationToProcessData() {
        // Verify report-generation has NO data processing classes
        assertThat(getClassesInPackage("com.bcbs239.regtech.reportgeneration.application"))
            .noneMatch(cls -> cls.getSimpleName().contains("Aggregator"))
            .noneMatch(cls -> cls.getSimpleName().contains("Processor"))
            .noneMatch(cls -> cls.getSimpleName().contains("Calculator"));
    }
}
```

**Time Estimate**: 8 hours

---

## Interface Changes

### New Interfaces in data-quality

```java
// data-quality/domain/storage/IDataStorageService.java
public interface IDataStorageService {
    String storeProcessedData(ProcessedBatchData data);
    ProcessedBatchData loadProcessedData(String storageUri);
    String loadFromUri(String uri);  // Generic fetch
}

// data-quality/domain/processing/IProcessedDataAggregator.java
public interface IProcessedDataAggregator {
    ProcessedBatchData processAndStore(
        RiskCalculationCompletedInboundEvent calculationEvent,
        QualityReport qualityReport
    );
}
```

### Updated Interfaces in report-generation

```java
// report-generation/domain/storage/IReportStorageService.java
// Change: Read-only access to processed data
public interface IReportStorageService {
    ProcessedBatchData loadProcessedData(String storageUri);  // Read-only
    void storeGeneratedReport(GeneratedReport report);  // Keep for report storage
}
```

---

## Event Flow Updates

### Old Event Flow (Before Refactoring)
```
risk-calculation
    └─> RiskCalculationCompletedEvent
         └─> report-generation (ProcessRiskCalculationCompletedUseCase)
              └─> ReportCoordinator.handleCalculationCompleted()
                   └─> BatchEventTracker.recordCalculationCompleted()

data-quality
    └─> DataQualityCompletedEvent
         └─> report-generation (ProcessDataQualityCompletedUseCase)
              └─> ReportCoordinator.handleQualityCompleted()
                   └─> BatchEventTracker.recordQualityCompleted()

ReportCoordinator
    └─> checkIfBothEventsReceived()
         └─> ComprehensiveReportDataAggregator.fetchAllData()  ❌ DATA PROCESSING
              └─> Generate report
```

### New Event Flow (After Refactoring)
```
risk-calculation
    └─> RiskCalculationCompletedEvent
         └─> data-quality (ProcessCalculationResultsUseCase)  ✅ MOVED HERE
              └─> ProcessedDataAggregator.processAndStore()
                   ├─> Fetch calculation results
                   ├─> Fetch quality results
                   ├─> Aggregate data
                   ├─> Store processed data
                   └─> Publish ProcessedDataReadyEvent
                        └─> report-generation (ProcessedDataReadyHandler)
                             └─> Load processed data (read-only)
                             └─> Build report structure
                             └─> Generate report formats
                             └─> Store generated report
```

**Key Differences**:
1. ✅ Data processing moved to data-quality
2. ✅ report-generation receives ONE event (ProcessedDataReadyEvent)
3. ✅ No coordination logic in report-generation
4. ✅ Clear separation: processing vs building

---

## Testing Strategy

### Unit Tests
- [ ] `ProcessedBatchData` domain entity tests
- [ ] `ProcessedDataAggregator` tests (mocked storage)
- [ ] `ProcessCalculationResultsUseCase` tests (mocked dependencies)
- [ ] `ProcessedDataReadyHandler` tests (mocked storage)
- [ ] `ReportBuilder` tests

### Integration Tests
- [ ] Storage adapter tests (S3 + local filesystem)
- [ ] Event publishing tests (ProcessedDataReadyEvent)
- [ ] Event consumption tests (report-generation listens correctly)
- [ ] Database persistence tests

### End-to-End Tests
- [ ] Complete flow: calculation → quality → processed data → report
- [ ] Error handling: missing quality report
- [ ] Error handling: storage failure
- [ ] Error handling: report generation failure
- [ ] Performance tests: large batches (1000+ exposures)

### Architecture Compliance Tests
- [ ] Verify no data processing classes in report-generation
- [ ] Verify report-generation only consumes ProcessedDataReadyEvent
- [ ] Verify clean architecture layers (domain → application → infrastructure)

---

## Rollback Plan

### If Issues Detected During Deployment

#### Immediate Rollback (< 5 minutes)
1. **Revert Git Commits**:
   ```bash
   git revert HEAD~7..HEAD
   git push origin main
   ```

2. **Redeploy Previous Version**:
   ```bash
   ./mvnw clean install -DskipTests
   ./mvnw spring-boot:run
   ```

#### Gradual Rollback (Feature Flag)
If feature flags were used:
```yaml
# application.yml
data-quality:
  use-new-processing-flow: false  # Revert to old flow
```

### Database Rollback
No database migrations in this refactoring (only code changes).

### Event Queue Cleanup
If events stuck in queues:
```sql
-- Clear inbox messages for new event type
DELETE FROM inbox_messages WHERE event_type = 'ProcessedDataReadyEvent';
```

---

## Implementation Checklist

### Pre-Migration
- [ ] Code freeze on affected modules
- [ ] Backup production database
- [ ] Notify team of refactoring timeline
- [ ] Create feature branch

### Phase 1: Data Quality Updates (Days 1-4)
- [ ] Create `ProcessedBatchData` domain entity
- [ ] Create `IDataStorageService` port
- [ ] Implement `DataStorageServiceAdapter`
- [ ] Create `ProcessedDataAggregator`
- [ ] Create `ProcessCalculationResultsUseCase`
- [ ] Create `ProcessedDataReadyEvent`
- [ ] Unit tests pass
- [ ] Integration tests pass

### Phase 2: Report Generation Updates (Days 4-6)
- [ ] Create `ProcessedDataReadyHandler`
- [ ] Update `ReportCoordinator` → `ReportBuilder`
- [ ] Delete old aggregation classes
- [ ] Delete old event processing use cases
- [ ] Delete coordination logic
- [ ] Update event listeners
- [ ] Unit tests pass
- [ ] Integration tests pass

### Phase 3: Testing & Documentation (Days 6-7)
- [ ] End-to-end integration tests
- [ ] Performance testing
- [ ] Architecture compliance tests
- [ ] Update documentation
- [ ] Code review
- [ ] Merge to main branch

### Post-Migration
- [ ] Verify NO duplicate storage code across modules
- [ ] Verify both modules use `IStorageService` from regtech-core
- [ ] Deploy to staging environment
- [ ] Smoke tests in staging
- [ ] Deploy to production
- [ ] Monitor logs and metrics
- [ ] Verify no errors in event processing
- [ ] Team training on new architecture

---

## Success Criteria

### Functional Requirements
- ✅ All existing functionality works (reports generate correctly)
- ✅ No data loss during migration
- ✅ Event processing continues without interruption
- ✅ Reports contain same data as before refactoring

### Architectural Requirements
- ✅ **NO duplicate storage code** - shared logic in regtech-core
- ✅ Both modules use `IStorageService` from regtech-core
- ✅ report-generation contains NO data processing logic
- ✅ report-generation only consumes `ProcessedDataReadyEvent`
- ✅ data-quality owns all data aggregation and storage
- ✅ Clear separation of concerns enforced

### Performance Requirements
- ✅ Report generation time remains < 15 seconds (same as before)
- ✅ Data processing time < 10 seconds
- ✅ Event processing latency < 5 seconds

### Quality Requirements
- ✅ Test coverage > 80% on new code
- ✅ No SonarQube critical/blocker issues
- ✅ Code review approved by 2+ team members
- ✅ Documentation updated and reviewed

---

## Timeline

| Phase | Duration | Completion Date |
|-------|----------|-----------------|
| **Phase 0: Extract Shared Storage** | **1 day** | **Day 1** |
| Phase 1: Data Quality Updates | 3 days | Day 4 |
| Phase 2: Report Generation Updates | 2 days | Day 6 |
| Phase 3: Testing & Documentation | 1 day | Day 7 |
| **Total** | **7 working days** | **Day 7** |

**Recommended Sprint**: 2-week sprint with buffer for testing and reviews

**⚠️ CRITICAL**: Phase 0 (shared storage extraction) MUST complete before Phase 1

---

## Risk Assessment

### High Risk
- ❌ **Event processing failures**: If events not routed correctly, reports won't generate
  - **Mitigation**: Comprehensive integration tests, canary deployment

### Medium Risk
- ⚠️ **Performance degradation**: Extra network hop (data-quality → storage → report-generation)
  - **Mitigation**: Performance testing, caching, optimized storage access

### Low Risk
- ✅ **Code complexity**: Cleaner separation actually reduces complexity
- ✅ **Database changes**: No database migrations required

---

## Questions & Answers

**Q1: Why not keep coordination logic in report-generation?**
- A: Coordination is a form of data processing. data-quality should decide when data is ready, not report-generation.

**Q2: What if quality validation completes before risk calculation?**
- A: data-quality waits for risk calculation event before processing. Order doesn't matter.

**Q3: Will this impact performance?**
- A: Minimal impact. We're already using S3/local storage for calculation results. One more storage write is negligible.

**Q4: Can we do this incrementally (feature flag)?**
- A: Yes. Add a feature flag `data-quality.use-new-processing-flow` to toggle between old and new flows.

**Q5: What happens to existing reports in progress?**
- A: They will complete using old flow. New batches use new flow after deployment.

---

## Conclusion

This refactoring enforces the **Single Responsibility Principle** at the module level AND **DRY principle** for shared code:

- **regtech-core** = Shared storage logic (single source of truth)
- **data-quality** = Data processing, aggregation, validation, storage
- **report-generation** = Report building, formatting, presentation

After this refactoring:
- ✅ **NO duplicate storage code** - eliminated bug risk from code duplication
- ✅ Clear module boundaries
- ✅ Easier to test and maintain
- ✅ Scalable architecture (can split modules into microservices later)
- ✅ Follows Clean Architecture principles
- ✅ Better observability (clear separation of processing vs formatting metrics)
- ✅ Consistent storage behavior across all modules

**Next Steps**: 
1. Review this plan with team
2. Schedule 2-week sprint
3. **Start with Phase 0** (extract shared storage - CRITICAL)
4. Then proceed to Phase 1 (data-quality updates)

---

**Document Version**: 1.0  
**Last Updated**: 2024-12-10  
**Author**: AI Coding Agent  
**Status**: Ready for Review ✅
