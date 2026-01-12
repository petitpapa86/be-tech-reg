# Metadata Extraction Implementation - Complete Summary

## Overview

Successfully implemented metadata extraction from `BatchDataDTO.bankInfo` to enhance validation accuracy for BCBS 239 compliance. The metadata includes:
- **declaredCount** (totalExposures from bank_info)
- **reportDate** (official report date from bank_info)

## Implementation Architecture

### 1. BatchWithMetadata Record (Application Layer)

**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/BatchWithMetadata.java`

**Purpose**: Wrapper record to transport exposures and associated metadata from download layer to validation layer.

```java
public record BatchWithMetadata(
    List<ExposureRecord> exposures,
    @Nullable Integer declaredCount,        // From bank_info.totalExposures
    @Nullable LocalDate reportDate          // From bank_info.reportDate
) {
    public BatchWithMetadata {
        if (exposures == null) {
            throw new IllegalArgumentException("Exposures list cannot be null");
        }
    }
    
    public static BatchWithMetadata withoutMetadata(List<ExposureRecord> exposures) {
        return new BatchWithMetadata(exposures, null, null);
    }
}
```

**Key Features**:
- Immutable record for thread-safety
- Validation in compact constructor
- Factory method for legacy formats without metadata
- Uses JSpecify `@Nullable` annotations

---

### 2. S3StorageService Interface Enhancement (Application Layer)

**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/S3StorageService.java`

**Added Method**:
```java
Result<BatchWithMetadata> downloadBatchWithMetadata(String s3Uri);
```

**Purpose**: Download batch data with metadata extraction capability.

---

### 3. LocalStorageServiceImpl Implementation (Infrastructure Layer)

**Location**: `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/integration/LocalStorageServiceImpl.java`

#### 3.1 Main Entry Point
```java
@Override
public Result<BatchWithMetadata> downloadBatchWithMetadata(String s3Uri) {
    try {
        BatchWithMetadata batch = downloadBatchFromLocalFile(s3Uri);
        logger.info("Successfully downloaded batch with {} exposures, declaredCount={}, reportDate={} in {} ms",
            batch.exposures().size(), batch.declaredCount(), batch.reportDate(), duration);
        return Result.success(batch);
    } catch (IOException e) {
        return Result.failure("LOCAL_PARSE_ERROR", ErrorType.SYSTEM_ERROR, 
            "Failed to parse JSON: " + e.getMessage(), "json_parsing");
    }
}
```

#### 3.2 Metadata Extraction Logic
```java
private BatchWithMetadata downloadBatchFromLocalFile(String fileUri) throws IOException {
    // ... file reading logic ...
    
    // Support new format with bank_info at top level
    if (rootNode.has("exposures") && rootNode.has("bank_info")) {
        BatchDataDTO batchData = objectMapper.treeToValue(rootNode, BatchDataDTO.class);
        
        // Extract metadata from bank_info
        Integer declaredCount = batchData.bankInfo().totalExposures();
        LocalDate reportDate = batchData.bankInfo().reportDate();
        
        // Log bank information
        logger.info("Processing batch for bank: {} (ABI: {}, LEI: {}), Report date: {}, Total exposures: {}",
            batchData.bankInfo().bankName(),
            batchData.bankInfo().abiCode(),
            batchData.bankInfo().leiCode(),
            reportDate,
            declaredCount);
        
        // Convert ExposureDTOs to ExposureRecords
        List<ExposureRecord> exposures = batchData.exposures().stream()
            .map(ExposureRecord::fromDTO)
            .toList();
        
        return new BatchWithMetadata(exposures, declaredCount, reportDate);
    }
    
    // Backward compatibility: legacy formats return BatchWithMetadata.withoutMetadata()
    if (rootNode.isArray()) {
        List<ExposureRecord> exposures = parseExposuresFromArray(rootNode);
        return BatchWithMetadata.withoutMetadata(exposures);
    }
    
    if (rootNode.has("loan_portfolio")) {
        JsonNode loanPortfolioNode = rootNode.get("loan_portfolio");
        List<ExposureRecord> exposures = parseExposuresFromArray(loanPortfolioNode);
        return BatchWithMetadata.withoutMetadata(exposures);
    }
}
```

**Key Features**:
- Supports BatchDataDTO format with bank_info
- Backward compatible with legacy formats (array, loan_portfolio)
- Extracts metadata: declaredCount and reportDate
- Comprehensive logging for audit trail

---

### 4. S3StorageServiceImpl Implementation (Infrastructure Layer)

**Location**: `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/integration/S3StorageServiceImpl.java`

```java
@Override
public Result<BatchWithMetadata> downloadBatchWithMetadata(String s3Uri) {
    try {
        BatchWithMetadata batch;
        
        // Check if it's a local file URI
        if (s3Uri != null && s3Uri.startsWith("file://")) {
            batch = downloadBatchFromLocalFile(s3Uri);
        } else {
            // For S3, fall back to downloading exposures without metadata
            // (streaming mode doesn't support metadata extraction yet)
            Result<List<ExposureRecord>> exposuresResult = downloadExposures(s3Uri);
            if (exposuresResult.isFailure()) {
                return Result.failure(exposuresResult.getError().orElseThrow());
            }
            batch = BatchWithMetadata.withoutMetadata(exposuresResult.getValueOrThrow());
        }
        
        return Result.success(batch);
    } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
        return Result.failure("S3_DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR, 
            "Failed to download from S3: " + e.getMessage(), "s3_download");
    }
}
```

**Key Features**:
- Delegates to local file logic for file:// URIs
- Falls back to no-metadata mode for S3 streaming
- Future enhancement: Add metadata support for S3 streaming

---

### 5. TimelinessValidator Enhancement (Application Layer)

**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/timeliness/TimelinessValidator.java`

#### 5.1 Updated Method Signature
```java
public TimelinessResult calculateTimeliness(
    List<ExposureRecord> exposures,
    LocalDate uploadDate,
    LocalDate reportDate,  // NEW PARAMETER - from metadata
    String bankId
)
```

#### 5.2 Metadata Priority Logic
```java
// Use provided reportDate from metadata, or calculate from exposures as fallback
LocalDate reportingDate = reportDate;
if (reportingDate == null) {
    // Fallback: calculate from exposures
    reportingDate = exposures.stream()
        .map(ExposureRecord::reportingDate)
        .filter(date -> date != null)
        .max(LocalDate::compareTo)
        .orElse(null);
}

if (reportingDate == null) {
    return TimelinessResult.noReportingDate();
}

long delayDays = ChronoUnit.DAYS.between(reportingDate, uploadDate);
```

**Key Features**:
- Accepts reportDate parameter from metadata
- Falls back to calculating from exposures if metadata not available
- Maintains backward compatibility with legacy data

---

### 6. ParallelExposureValidationCoordinator Update (Application Layer)

**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/ParallelExposureValidationCoordinator.java`

```java
TimelinessValidator.TimelinessResult timelinessResult = null;
if (uploadDate != null && bankId != null && !exposures.isEmpty()) {
    timelinessResult = timelinessValidator.calculateTimeliness(
        exposures, uploadDate, null, bankId  // null = calculate from exposures
    );
}
```

**Key Features**:
- Passes null for reportDate (will be overridden with metadata in command handler)
- Provides fallback calculation based on exposure dates

---

### 7. ValidateBatchQualityCommandHandler Integration (Application Layer)

**Location**: `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/ValidateBatchQualityCommandHandler.java`

#### 7.1 Dependency Injection
```java
private final TimelinessValidator timelinessValidator;

public ValidateBatchQualityCommandHandler(
    S3StorageService s3StorageService,
    RulesService rulesService,
    QualityReportRepository qualityReportRepository,
    IIntegrationEventBus integrationEventBus,
    ILogger logger,
    TimelinessValidator timelinessValidator  // NEW PARAMETER
) {
    // ...
    this.timelinessValidator = timelinessValidator;
}
```

#### 7.2 Download with Metadata
```java
Result<BatchWithMetadata> downloadResult = s3StorageService.downloadBatchWithMetadata(command.s3Uri());
if (downloadResult.isFailure()) {
    return Result.failure(downloadResult.getError().orElseThrow());
}

BatchWithMetadata batchData = downloadResult.getValueOrThrow();
List<ExposureRecord> exposures = batchData.exposures();

logger.info("Downloaded batch for batchId={}: exposures={}, declaredCount={}, reportDate={}",
    command.batchId().value(), exposures.size(), batchData.declaredCount(), batchData.reportDate());
```

#### 7.3 Use Metadata for declaredCount
```java
ValidationBatchResult batchResult = coordinator.validateAll(
    exposures, rulesService,
    batchData.declaredCount() != null ? batchData.declaredCount() : command.expectedExposureCount(),
    null,  // crmReferences - requires external CRM system
    command.uploadDate() != null ? command.uploadDate() : java.time.LocalDate.now(),
    command.bankId().value()
);
```

#### 7.4 Override Timeliness with Metadata
```java
// Override timeliness calculation with metadata reportDate if available
if (batchData.reportDate() != null && command.uploadDate() != null) {
    TimelinessValidator.TimelinessResult metadataTimeliness = 
        timelinessValidator.calculateTimeliness(
            exposures,
            command.uploadDate() != null ? command.uploadDate() : java.time.LocalDate.now(),
            batchData.reportDate(),  // Use metadata report date
            command.bankId().value()
        );
    
    // Replace timeliness result with metadata-based calculation
    batchResult = ValidationBatchResult.complete(
        batchResult.results(),
        batchResult.exposureResults(),
        batchResult.consistencyResult(),
        metadataTimeliness  // Override
    );
}
```

**Key Features**:
- Downloads batch with metadata
- Uses metadata declaredCount for "Declared Count Match" consistency check
- Two-phase timeliness calculation:
  1. Coordinator calculates with fallback (max exposure date)
  2. Command handler overrides with metadata reportDate if available
- Maintains backward compatibility (fallback to command parameters if metadata missing)

---

## Metadata Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Data Source Layer                           │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              BatchDataDTO (JSON File)                       │ │
│  │  {                                                          │ │
│  │    "bank_info": {                                           │ │
│  │      "totalExposures": 1500,  ← declaredCount             │ │
│  │      "reportDate": "2024-12-31"  ← reportDate             │ │
│  │    },                                                       │ │
│  │    "exposures": [ ... ]                                    │ │
│  │  }                                                          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  LocalStorageServiceImpl.downloadBatchFromLocalFile()      │ │
│  │    → Parse BatchDataDTO                                    │ │
│  │    → Extract metadata:                                     │ │
│  │      - declaredCount = bankInfo.totalExposures()          │ │
│  │      - reportDate = bankInfo.reportDate()                 │ │
│  │    → Convert exposures to ExposureRecord list             │ │
│  │    → Return BatchWithMetadata                             │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │         BatchWithMetadata (Transport Object)                │ │
│  │  {                                                          │ │
│  │    exposures: List<ExposureRecord>,                        │ │
│  │    declaredCount: 1500,  ← Extracted                      │ │
│  │    reportDate: LocalDate.of(2024, 12, 31)  ← Extracted   │ │
│  │  }                                                          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│               Validation Orchestration Layer                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │      ValidateBatchQualityCommandHandler                    │ │
│  │                                                             │ │
│  │  1. coordinator.validateAll(                               │ │
│  │       exposures,                                           │ │
│  │       declaredCount: batchData.declaredCount() ← Metadata │ │
│  │     )                                                      │ │
│  │     → Consistency check "Declared Count Match" uses this  │ │
│  │                                                             │ │
│  │  2. timelinessValidator.calculateTimeliness(               │ │
│  │       exposures,                                           │ │
│  │       uploadDate,                                          │ │
│  │       reportDate: batchData.reportDate() ← Metadata       │ │
│  │     )                                                      │ │
│  │     → Timeliness calculation uses official report date    │ │
│  │                                                             │ │
│  │  3. Override batchResult.timelinessResult                  │ │
│  │     → Replace with metadata-based calculation              │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Validation Results Layer                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │             ValidationBatchResult (JSON Output)             │ │
│  │  {                                                          │ │
│  │    "dimensione_coerenza": {                                │ │
│  │      "declared_count_match": {                             │ │
│  │        "score": 1.0,                                       │ │
│  │        "passed": true,                                     │ │
│  │        "details": {                                        │ │
│  │          "declared_count": 1500,  ← From metadata         │ │
│  │          "actual_count": 1500                             │ │
│  │        }                                                   │ │
│  │      }                                                     │ │
│  │    },                                                      │ │
│  │    "dimensione_tempestivita": {                           │ │
│  │      "reporting_date": "2024-12-31",  ← From metadata    │ │
│  │      "upload_date": "2025-01-10",                         │ │
│  │      "delay_days": 10,                                    │ │
│  │      "threshold_days": 7,                                 │ │
│  │      "score": 0.3,                                        │ │
│  │      "passed": false                                      │ │
│  │    }                                                       │ │
│  │  }                                                          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Validation Enhancements

### 1. Consistency Dimension (DIMENSIONE: COERENZA)

**Check**: Declared Count Match

**Before Metadata**:
```json
{
  "declared_count_match": {
    "score": 0.0,
    "passed": false,
    "details": {
      "declared_count": null,  // ❌ No metadata
      "actual_count": 1500,
      "match": false,
      "message": "No declared count provided"
    }
  }
}
```

**After Metadata**:
```json
{
  "declared_count_match": {
    "score": 1.0,
    "passed": true,
    "details": {
      "declared_count": 1500,  // ✅ From bank_info.totalExposures
      "actual_count": 1500,
      "match": true,
      "message": "Declared count matches actual count"
    }
  }
}
```

---

### 2. Timeliness Dimension (DIMENSIONE: TEMPESTIVITÀ)

**Before Metadata (Calculated from Exposures)**:
```json
{
  "dimensione_tempestivita": {
    "reporting_date": "2024-12-28",  // ❌ max(exposures.reportingDate) - may be inaccurate
    "upload_date": "2025-01-10",
    "delay_days": 13,
    "threshold_days": 7,
    "score": 0.0,
    "passed": false
  }
}
```

**After Metadata (Official Report Date)**:
```json
{
  "dimensione_tempestivita": {
    "reporting_date": "2024-12-31",  // ✅ From bank_info.reportDate - official date
    "upload_date": "2025-01-10",
    "delay_days": 10,
    "threshold_days": 7,
    "score": 0.3,
    "passed": false
  }
}
```

**Key Improvement**: Using official reportDate from bank_info provides accurate timeliness calculation instead of approximation from exposure dates.

---

## Backward Compatibility

### Legacy Format Support

**Array Format** (no metadata):
```json
[
  { "exposure_id": "EXP001", ... },
  { "exposure_id": "EXP002", ... }
]
```
→ Returns `BatchWithMetadata.withoutMetadata(exposures)`

**Loan Portfolio Format** (no metadata):
```json
{
  "loan_portfolio": [
    { "exposure_id": "EXP001", ... }
  ]
}
```
→ Returns `BatchWithMetadata.withoutMetadata(exposures)`

**New Format** (with metadata):
```json
{
  "bank_info": {
    "bank_name": "Banca Intesa",
    "abi_code": "03069",
    "lei_code": "2W8N8UU78PMDQKZENC08",
    "report_date": "2024-12-31",
    "total_exposures": 1500
  },
  "exposures": [
    { "exposure_id": "EXP001", ... }
  ]
}
```
→ Returns `BatchWithMetadata(exposures, 1500, LocalDate.of(2024, 12, 31))`

---

## Fallback Logic

### declaredCount Fallback Chain
1. **First Priority**: `batchData.declaredCount()` (from bank_info.totalExposures)
2. **Second Priority**: `command.expectedExposureCount()` (from command parameter)
3. **Third Priority**: `null` (skip declared count check)

```java
Integer declaredCount = batchData.declaredCount() != null 
    ? batchData.declaredCount() 
    : command.expectedExposureCount();
```

### reportDate Fallback Chain
1. **First Priority**: `batchData.reportDate()` (from bank_info.reportDate)
2. **Second Priority**: Calculate from exposures: `max(exposures.reportingDate)`
3. **Third Priority**: `null` (skip timeliness check)

```java
LocalDate reportingDate = reportDate;
if (reportingDate == null) {
    reportingDate = exposures.stream()
        .map(ExposureRecord::reportingDate)
        .filter(date -> date != null)
        .max(LocalDate::compareTo)
        .orElse(null);
}
```

---

## Testing Recommendations

### Unit Tests

#### Test BatchWithMetadata
```java
@Test
void shouldCreateBatchWithMetadata() {
    List<ExposureRecord> exposures = List.of(mockExposure());
    Integer declaredCount = 1500;
    LocalDate reportDate = LocalDate.of(2024, 12, 31);
    
    BatchWithMetadata batch = new BatchWithMetadata(exposures, declaredCount, reportDate);
    
    assertThat(batch.exposures()).hasSize(1);
    assertThat(batch.declaredCount()).isEqualTo(1500);
    assertThat(batch.reportDate()).isEqualTo(LocalDate.of(2024, 12, 31));
}

@Test
void shouldCreateBatchWithoutMetadata() {
    List<ExposureRecord> exposures = List.of(mockExposure());
    
    BatchWithMetadata batch = BatchWithMetadata.withoutMetadata(exposures);
    
    assertThat(batch.exposures()).hasSize(1);
    assertThat(batch.declaredCount()).isNull();
    assertThat(batch.reportDate()).isNull();
}
```

#### Test TimelinessValidator
```java
@Test
void shouldCalculateTimelinessWithMetadataReportDate() {
    List<ExposureRecord> exposures = createExposures();
    LocalDate uploadDate = LocalDate.of(2025, 1, 10);
    LocalDate metadataReportDate = LocalDate.of(2024, 12, 31);
    
    TimelinessResult result = timelinessValidator.calculateTimeliness(
        exposures, uploadDate, metadataReportDate, "bank123"
    );
    
    assertThat(result.reportingDate()).isEqualTo(metadataReportDate);
    assertThat(result.delayDays()).isEqualTo(10); // 2024-12-31 to 2025-01-10
}

@Test
void shouldFallbackToCalculatedReportDateWhenMetadataNull() {
    List<ExposureRecord> exposures = createExposuresWithDates();
    LocalDate uploadDate = LocalDate.of(2025, 1, 10);
    LocalDate calculatedMaxDate = LocalDate.of(2024, 12, 28);
    
    TimelinessResult result = timelinessValidator.calculateTimeliness(
        exposures, uploadDate, null, "bank123"  // null metadata
    );
    
    assertThat(result.reportingDate()).isEqualTo(calculatedMaxDate);
    assertThat(result.delayDays()).isEqualTo(13); // 2024-12-28 to 2025-01-10
}
```

### Integration Tests

#### Test End-to-End Flow
```java
@Test
void shouldDownloadBatchWithMetadataAndValidate() {
    // Given: File with BatchDataDTO format
    String fileUri = "file:///data/batch_with_metadata.json";
    ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
        batchId, fileUri, bankId, null, uploadDate
    );
    
    // When: Execute validation
    Result<QualityReportId> result = commandHandler.handle(command);
    
    // Then: Verify metadata used
    assertThat(result.isSuccess()).isTrue();
    QualityReport report = reportRepository.findById(result.getValueOrThrow()).orElseThrow();
    assertThat(report.getDeclaredCount()).isEqualTo(1500);  // From metadata
    assertThat(report.getReportDate()).isEqualTo(LocalDate.of(2024, 12, 31));  // From metadata
}

@Test
void shouldFallbackForLegacyFormat() {
    // Given: File with legacy array format (no metadata)
    String fileUri = "file:///data/legacy_exposures.json";
    ValidateBatchQualityCommand command = new ValidateBatchQualityCommand(
        batchId, fileUri, bankId, 1500, uploadDate  // Command provides expected count
    );
    
    // When: Execute validation
    Result<QualityReportId> result = commandHandler.handle(command);
    
    // Then: Verify fallback to command parameters
    assertThat(result.isSuccess()).isTrue();
    QualityReport report = reportRepository.findById(result.getValueOrThrow()).orElseThrow();
    assertThat(report.getDeclaredCount()).isEqualTo(1500);  // From command
    // reportDate calculated from exposures
}
```

---

## Performance Considerations

### Memory Usage
- **BatchWithMetadata** is lightweight (only 2 additional fields: Integer + LocalDate)
- Exposures list is shared (no duplication)
- Metadata extraction happens once during file parsing

### Processing Time
- Minimal overhead: metadata extracted during existing JSON parsing
- No additional I/O operations required
- Fallback logic adds negligible computation time

### Scalability
- Metadata extraction scales linearly with file size
- No impact on parallel processing capability
- Backward compatibility ensures smooth transition

---

## Future Enhancements

### 1. S3 Streaming Metadata Support
Currently, S3 streaming mode doesn't extract metadata. Future enhancement:
```java
private BatchWithMetadata downloadAndParseStreamingWithMetadata(ResponseInputStream<?> s3Object) {
    // Parse bank_info during streaming
    // Extract metadata while parsing exposures
    // Return BatchWithMetadata with both
}
```

### 2. Additional Metadata Fields
Extend `BatchWithMetadata` to include:
- Bank name, ABI code, LEI code
- Regulatory period
- Currency information
- Data classification

### 3. Metadata Validation
Add validation for metadata consistency:
- Verify declaredCount matches actual exposure count
- Validate reportDate is not in future
- Check bank identifiers are valid

---

## Benefits Summary

✅ **Accuracy**: Using official reportDate from bank_info instead of approximation  
✅ **Completeness**: Declared count check now functional with metadata  
✅ **Backward Compatibility**: Legacy formats still supported with fallback logic  
✅ **Maintainability**: Clean separation of concerns with BatchWithMetadata wrapper  
✅ **Testability**: Easy to test with and without metadata scenarios  
✅ **Logging**: Comprehensive audit trail for metadata extraction  
✅ **Type Safety**: Immutable record with compile-time validation  

---

## Compilation Status

✅ **Module Compilation**: SUCCESS  
✅ **All Dependencies**: Resolved  
✅ **No Compilation Errors**: Clean build  

**Build Time**: 1 minute 12 seconds  
**Modules Compiled**: 11 (including all dependencies)  

---

## Next Steps

1. **Integration Testing**: Test with real BatchDataDTO files containing bank_info metadata
2. **Performance Benchmarking**: Measure overhead of metadata extraction
3. **Documentation Updates**: Update API documentation with metadata parameters
4. **Migration Guide**: Create guide for transitioning from legacy formats to metadata format

---

**Document Version**: 1.0  
**Last Updated**: January 12, 2026  
**Status**: ✅ Implementation Complete, Compiled Successfully
