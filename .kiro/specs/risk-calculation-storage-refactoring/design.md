# Risk Calculation Storage Refactoring - Design Document

## Overview

This design document outlines the refactoring of the Risk Calculation Module's data storage architecture to adopt a file-first strategy. The current implementation stores detailed exposure and mitigation data in PostgreSQL tables while also serializing complete results to JSON files, creating unnecessary data duplication. This refactoring eliminates the redundancy by establishing JSON files as the single source of truth for detailed calculation results, while maintaining minimal database metadata for operational queries.

### Key Design Decisions

1. **JSON Files as Single Source of Truth**: Complete calculation results (exposures, mitigations, portfolio analysis) will be stored exclusively in JSON files, eliminating database duplication.

2. **Minimal Database Metadata**: Only essential batch metadata (status, URI, timestamps, counts) will be stored in the database for fast operational queries.

3. **Optional Summary Persistence**: Portfolio analysis summaries can optionally be stored in the database for dashboard performance, but detailed breakdowns remain in JSON files.

4. **Immutable Audit Records**: JSON files serve as immutable, versioned audit records that can be retrieved independently of database state.

5. **Backward Compatibility**: The refactoring maintains API compatibility while deprecating database persistence methods for exposures and mitigations.

### Rationale

- **Reduced Storage Costs**: Eliminates duplicate storage of large exposure datasets
- **Simplified Schema**: Removes complex tables and relationships for exposures/mitigations
- **Improved Maintainability**: Single source of truth reduces synchronization issues
- **Better Audit Trail**: Immutable JSON files provide reliable historical records
- **Scalability**: File storage scales better than database storage for large datasets

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Risk Calculation Module                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  Calculation     │         │  File Storage    │          │
│  │  Command Handler │────────▶│  Service         │          │
│  └──────────────────┘         └──────────────────┘          │
│           │                            │                     │
│           │                            ▼                     │
│           │                   ┌──────────────────┐          │
│           │                   │  JSON Serializer │          │
│           │                   └──────────────────┘          │
│           │                            │                     │
│           │                            ▼                     │
│           │                   ┌──────────────────┐          │
│           │                   │  S3 / Local FS   │          │
│           │                   │  (JSON Files)    │          │
│           │                   └──────────────────┘          │
│           │                                                  │
│           ▼                                                  │
│  ┌──────────────────┐                                       │
│  │  Batch Metadata  │                                       │
│  │  Repository      │                                       │
│  └──────────────────┘                                       │
│           │                                                  │
│           ▼                                                  │
│  ┌──────────────────┐                                       │
│  │  PostgreSQL      │                                       │
│  │  (Metadata Only) │                                       │
│  └──────────────────┘                                       │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Storage Flow

1. **Calculation Completion**:
   - Risk calculation completes with all exposures and mitigations
   - Results are serialized to JSON format
   - JSON file is stored via File Storage Service
   - Storage URI is returned

2. **Metadata Persistence**:
   - Batch metadata (including storage URI) is saved to database
   - Optional portfolio analysis summary is saved to database
   - Batch status is updated to COMPLETED

3. **Result Retrieval**:
   - Fast queries use database metadata (status, counts, summaries)
   - Detailed queries download and parse JSON files using storage URI
   - Downstream modules receive storage URI in events

### Database Schema Changes

**Existing Tables to Deprecate**:
- `exposures` table (detailed exposure records)
- `mitigations` table (detailed mitigation records)

**Tables to Retain**:
- `batches` table (with added `calculation_results_uri` column)
- `portfolio_analysis` table (optional summary metrics)

**New Schema for `batches` table**:
```sql
ALTER TABLE batches 
ADD COLUMN calculation_results_uri VARCHAR(500);

-- Index for URI lookups
CREATE INDEX idx_batches_results_uri 
ON batches(calculation_results_uri);
```

## Components and Interfaces

### 1. File Storage Service (Existing)

**Interface**: `IFileStorageService`

```java
public interface IFileStorageService {
    Result<String> storeFile(String fileName, String content);
    Result<String> retrieveFile(String storageUri);
    Result<Void> deleteFile(String storageUri);
}
```

**Implementations**:
- `LocalFileStorageService`: Development/testing (filesystem)
- `S3FileStorageService`: Production (AWS S3)

**Design Decision**: Reuse existing file storage abstraction to maintain consistency with other modules.

### 2. JSON Serializer (Enhanced)

**Component**: `CalculationResultsJsonSerializer`

**Responsibilities**:
- Serialize complete calculation results to JSON
- Include format version for backward compatibility
- Deserialize JSON back to structured objects
- Handle serialization errors gracefully

**Enhanced JSON Format**:
```json
{
  "format_version": "1.0",
  "batch_id": "batch_20241207_123456",
  "calculated_at": "2024-12-07T12:34:56Z",
  "bank_info": {
    "bank_name": "Example Bank",
    "abi_code": "12345",
    "lei_code": "LEI123456789"
  },
  "summary": {
    "total_exposures": 1500,
    "total_amount_eur": 1500000000.00,
    "geographic_breakdown": { ... },
    "sector_breakdown": { ... },
    "concentration_indices": {
      "herfindahl_geographic": 0.15,
      "herfindahl_sector": 0.22
    }
  },
  "calculated_exposures": [
    {
      "exposure_id": "EXP001",
      "gross_exposure_eur": 1000000.00,
      "net_exposure_eur": 800000.00,
      "total_mitigation_eur": 200000.00,
      "percentage_of_total": 0.067,
      "counterparty": { ... },
      "classification": { ... },
      "mitigations": [ ... ]
    }
  ]
}
```

**Design Decision**: Include `format_version` field to enable future schema evolution while maintaining backward compatibility.

### 3. Batch Repository (Enhanced)

**Interface**: `BatchRepository`

**New Methods**:
```java
public interface BatchRepository {
    // Existing methods...
    void createBatch(String batchId, BankInfo bankInfo, LocalDate reportDate, 
                     int totalExposures, Instant ingestedAt);
    boolean exists(String batchId);
    void updateStatus(String batchId, String status);
    void markAsProcessed(String batchId, Instant processedAt);
    
    // New methods for file-first architecture
    void updateCalculationResultsUri(String batchId, String uri);
    Optional<String> getCalculationResultsUri(String batchId);
    void markAsCompleted(String batchId, String resultsUri, Instant processedAt);
}
```

**Design Decision**: Add URI management methods to support file-first architecture while maintaining existing batch lifecycle methods.

### 4. Exposure Repository (Deprecated)

**Status**: Methods for database persistence will be deprecated

**Migration Strategy**:
- Mark `save()` and `saveAll()` methods as `@Deprecated`
- Add new method: `List<ExposureRecording> loadFromJson(String jsonContent)`
- Existing `findByBatchId()` will delegate to JSON file retrieval

**Design Decision**: Maintain interface for backward compatibility but redirect to JSON-based retrieval.

### 5. Mitigation Repository (Deprecated)

**Status**: Methods for database persistence will be deprecated

**Migration Strategy**:
- Mark `save()` and `saveAll()` methods as `@Deprecated`
- Add new method: `List<RawMitigationData> loadFromJson(String jsonContent)`
- Existing `findByBatchId()` will delegate to JSON file retrieval

**Design Decision**: Similar to ExposureRepository, maintain interface but redirect to JSON-based retrieval.

### 6. Portfolio Analysis Repository (Retained)

**Status**: Retained for optional summary persistence

**Purpose**: Store summary metrics for fast dashboard queries without parsing JSON files

**Fields Stored**:
- `batch_id`
- `total_portfolio_eur`
- `geographic_hhi`
- `geographic_concentration_level`
- `sector_hhi`
- `sector_concentration_level`
- `analyzed_at`

**Design Decision**: Keep summary persistence optional to balance query performance with storage simplicity.

### 7. Calculation Results Storage Service (New)

**Interface**: `ICalculationResultsStorageService`

```java
public interface ICalculationResultsStorageService {
    Result<String> storeCalculationResults(RiskCalculationResult result);
    Result<RiskCalculationResult> retrieveCalculationResults(String batchId);
    Result<JsonNode> retrieveCalculationResultsRaw(String batchId);
}
```

**Implementation**: `CalculationResultsStorageServiceImpl`

**Responsibilities**:
- Orchestrate serialization and file storage
- Generate appropriate file names (e.g., `risk_calc_{batchId}_{timestamp}.json`)
- Handle retrieval by batch ID (lookup URI from database, then download file)
- Provide both structured and raw JSON retrieval

**Design Decision**: Create dedicated service to encapsulate the complete storage workflow and hide complexity from command handlers.

## Data Models

### Batch Metadata (Enhanced)

```java
public class BatchMetadata {
    private String batchId;
    private BankInfo bankInfo;
    private LocalDate reportDate;
    private int totalExposures;
    private BatchStatus status;
    private String calculationResultsUri;  // NEW
    private Instant ingestedAt;
    private Instant processedAt;
}
```

### Calculation Results JSON Structure

**Root Level**:
- `format_version`: String (e.g., "1.0")
- `batch_id`: String
- `calculated_at`: ISO 8601 timestamp
- `bank_info`: Object (bank_name, abi_code, lei_code)
- `summary`: Object (aggregated metrics)
- `calculated_exposures`: Array (detailed exposure records)

**Summary Section**:
- `total_exposures`: Integer
- `total_amount_eur`: Decimal
- `geographic_breakdown`: Object (country → amount/percentage)
- `sector_breakdown`: Object (sector → amount/percentage)
- `concentration_indices`: Object (HHI values)

**Exposure Record**:
- `exposure_id`: String
- `gross_exposure_eur`: Decimal
- `net_exposure_eur`: Decimal
- `total_mitigation_eur`: Decimal
- `percentage_of_total`: Decimal
- `counterparty`: Object (LEI, name, country)
- `classification`: Object (geographic, sector)
- `mitigations`: Array (type, eur_value)

### Portfolio Analysis Summary (Database)

```java
public class PortfolioAnalysisSummary {
    private String batchId;
    private BigDecimal totalPortfolioEur;
    private double geographicHHI;
    private ConcentrationLevel geographicConcentrationLevel;
    private double sectorHHI;
    private ConcentrationLevel sectorConcentrationLevel;
    private Instant analyzedAt;
}
```

**Design Decision**: Store only summary metrics in database, not detailed breakdowns, to minimize database storage while enabling fast dashboard queries.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: JSON Serialization Completeness
*For any* calculation result, serializing to JSON and then checking the structure should confirm that all required sections (batch metadata, exposures, mitigations, portfolio analysis) are present.
**Validates: Requirements 1.2, 10.2, 10.3, 10.4**

### Property 2: Storage URI Validity
*For any* JSON file stored via File_Storage_Service, the returned URI should be well-formed and retrievable (either valid S3 URI format or valid filesystem path).
**Validates: Requirements 1.4**

### Property 3: Database Exposure Exclusion
*For any* batch processing operation, after completion, the exposures table should not contain new records for that batch_id.
**Validates: Requirements 2.3**

### Property 4: Database Mitigation Exclusion
*For any* batch processing operation, after completion, the mitigations table should not contain new records for that batch_id.
**Validates: Requirements 2.4**

### Property 5: Batch Completion State
*For any* successfully processed batch, the database record should have status=COMPLETED and a non-null calculation_results_uri.
**Validates: Requirements 2.5**

### Property 6: Event URI Inclusion
*For any* BatchCalculationCompletedEvent published, the event payload should contain a non-empty calculation_results_uri field.
**Validates: Requirements 4.1**

### Property 7: File Retrieval Round-Trip
*For any* calculation result stored as JSON, retrieving the file by its URI and deserializing should produce an equivalent calculation result.
**Validates: Requirements 4.3**

### Property 8: JSON-Based Exposure Retrieval
*For any* batch with stored JSON results, calling the exposure retrieval method should return parsed exposure data without querying the exposures table.
**Validates: Requirements 5.5**

### Property 9: JSON File Immutability
*For any* stored JSON file, attempting to store another file with the same batch_id should either fail or create a new versioned file, never overwriting the original.
**Validates: Requirements 8.1, 8.4**

### Property 10: Historical Data Retrieval
*For any* batch_id with stored results, retrieving calculation results by that batch_id should return the complete JSON file.
**Validates: Requirements 8.3**

### Property 11: Status Query Database-Only
*For any* batch status query, the operation should complete using only database queries without triggering file I/O operations.
**Validates: Requirements 9.1**

### Property 12: Summary Query Database-Only
*For any* portfolio summary query, when summaries are enabled, the operation should complete using only database queries without file access.
**Validates: Requirements 9.2**

### Property 13: Detailed Query File-Based
*For any* detailed exposure query, the operation should trigger JSON file download and parsing.
**Validates: Requirements 9.3**

### Property 14: Format Version Presence
*For any* serialized calculation result JSON, the root object should contain a "format_version" field with a valid version string.
**Validates: Requirements 10.1**

### Property 15: Error Event Publishing
*For any* file storage error during batch processing, a BatchCalculationFailedEvent should be published with error details.
**Validates: Requirements 7.5**

## Error Handling

### Serialization Errors

**Exception**: `CalculationResultsSerializationException`

**Scenarios**:
- Invalid calculation result structure
- Circular references in object graph
- Unsupported data types

**Handling**:
- Log detailed error with batch_id and failure reason
- Publish BatchCalculationFailedEvent
- Mark batch status as FAILED in database
- Do not proceed with file storage

### File Storage Errors

**Exception**: `FileStorageException`

**Scenarios**:
- S3 connection failures
- Insufficient permissions
- Storage quota exceeded
- Network timeouts

**Handling**:
- Log error with batch_id and storage URI
- Retry with exponential backoff (3 attempts)
- If all retries fail, publish BatchCalculationFailedEvent
- Mark batch status as FAILED in database

### File Retrieval Errors

**Exception**: `FileNotFoundException`

**Scenarios**:
- File deleted or moved
- Invalid URI in database
- Storage service unavailable

**Handling**:
- Log error with batch_id and requested URI
- Return error response to API caller
- Do not retry (file absence is permanent)
- Consider marking batch for reprocessing

### Deserialization Errors

**Exception**: `CalculationResultsDeserializationException`

**Scenarios**:
- Corrupted JSON file
- Format version mismatch
- Missing required fields
- Invalid data types

**Handling**:
- Log error with batch_id and file URI
- Attempt to parse with older format versions
- If parsing fails, return error to caller
- Consider file corruption and potential reprocessing

### Database Errors

**Exception**: `DataAccessException` (Spring)

**Scenarios**:
- Connection failures
- Constraint violations
- Transaction timeouts

**Handling**:
- Log error with batch_id and operation details
- Retry transient errors (connection issues)
- For constraint violations, investigate data integrity
- Publish BatchCalculationFailedEvent for permanent failures

### Consistency Errors

**Scenario**: File stored but database update fails

**Handling**:
- File remains in storage (orphaned)
- Batch status remains IN_PROGRESS
- Background job should detect orphaned files
- Retry database update or mark batch for manual review

**Scenario**: Database updated but event publishing fails

**Handling**:
- Batch marked as COMPLETED in database
- Downstream modules not notified
- Background job should detect completed batches without published events
- Republish events for missed notifications

## Testing Strategy

### Unit Testing

**Serialization Tests**:
- Test JSON serialization with various calculation result structures
- Verify all required fields are present in serialized JSON
- Test format_version field inclusion
- Test serialization of edge cases (empty exposures, null mitigations)

**Repository Tests**:
- Test batch metadata persistence with calculation_results_uri
- Verify exposures are NOT persisted to database
- Verify mitigations are NOT persisted to database
- Test portfolio analysis summary persistence (when enabled)
- Test URI retrieval by batch_id

**File Storage Service Tests**:
- Mock file storage operations
- Test URI generation and validation
- Test error handling for storage failures
- Test retrieval by URI

**Deserialization Tests**:
- Test JSON deserialization to domain objects
- Test handling of different format versions
- Test error handling for malformed JSON
- Test backward compatibility with older formats

### Property-Based Testing

The model will use **jqwik** (already configured in the project) for property-based testing.

**Property Test 1: Serialization Round-Trip**
- Generate random RiskCalculationResult objects
- Serialize to JSON, then deserialize
- Verify the deserialized object equals the original
- **Validates: Property 7**

**Property Test 2: URI Format Validity**
- Generate random file content
- Store via File_Storage_Service
- Verify returned URI matches expected format (S3 or filesystem)
- **Validates: Property 2**

**Property Test 3: Database Exclusion**
- Generate random batch processing scenarios
- Execute storage workflow
- Query exposures and mitigations tables
- Verify no records exist for the batch_id
- **Validates: Properties 3, 4**

**Property Test 4: Batch Completion State**
- Generate random successful batch completions
- Verify database record has COMPLETED status and non-null URI
- **Validates: Property 5**

**Property Test 5: Event Structure**
- Generate random batch completion events
- Verify calculation_results_uri field is present and non-empty
- **Validates: Property 6**

**Property Test 6: File Immutability**
- Generate random batch_id and file content
- Store file
- Attempt to store again with same batch_id
- Verify original file is unchanged or operation fails
- **Validates: Property 9**

**Property Test 7: Query Isolation**
- Generate random batch status queries
- Monitor file I/O operations
- Verify no file access occurs during status queries
- **Validates: Property 11**

**Property Test 8: Format Version Presence**
- Generate random calculation results
- Serialize to JSON
- Parse JSON and verify format_version field exists
- **Validates: Property 14**

### Integration Testing

**End-to-End Storage Flow**:
- Execute complete calculation workflow
- Verify JSON file is created in storage
- Verify database metadata is updated with URI
- Verify event is published with URI
- Verify file can be retrieved and parsed

**Repository Integration**:
- Test with real database (H2 for tests)
- Verify batch metadata persistence
- Verify portfolio analysis summary persistence
- Verify old tables remain empty

**File Storage Integration**:
- Test with local filesystem storage
- Test file creation, retrieval, and error scenarios
- Verify URI generation and resolution

**Error Scenario Testing**:
- Simulate storage failures and verify error handling
- Simulate database failures and verify rollback
- Simulate deserialization errors and verify error responses

### Migration Testing

**Backward Compatibility**:
- Test reading old batches that have database-stored exposures
- Verify system can handle mixed state (some batches in DB, some in files)
- Test gradual migration scenarios

**Schema Migration**:
- Test adding calculation_results_uri column to existing batches table
- Test system behavior with deprecated tables present
- Test system behavior after dropping deprecated tables

### Performance Testing

**File Size Testing**:
- Test with large calculation results (10,000+ exposures)
- Measure serialization time
- Measure file upload time
- Measure file download and parsing time

**Concurrent Access**:
- Test multiple simultaneous batch processing operations
- Verify file storage handles concurrent writes
- Verify database handles concurrent metadata updates

**Query Performance**:
- Benchmark status queries (database-only)
- Benchmark summary queries (database-only)
- Benchmark detailed queries (file-based)
- Compare with old database-query approach

