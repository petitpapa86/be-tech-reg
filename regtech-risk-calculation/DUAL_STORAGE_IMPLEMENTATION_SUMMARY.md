# Dual Storage Strategy Implementation Summary

## Overview
Successfully implemented the dual storage strategy for risk calculation results, separating detailed data (S3/filesystem) from aggregated summaries (database).

## Components Created

### 1. Domain Layer
**Already Existed:**
- `BatchSummary` - Domain aggregate for database storage
- `BatchSummaryStatus` - Status enumeration
- `BatchSummaryRepository` - Repository interface
- `ICalculationResultsStorageService` - Service interface for file storage

### 2. Application Layer
**Created:**
- `CalculationResultsJsonSerializer` - Serializes calculation results to/from JSON
  - Handles complete result serialization with summary and detailed exposures
  - Provides deserialization for retrieval scenarios
  - Uses Jackson ObjectMapper for JSON processing

**Updated:**
- `CalculateRiskMetricsCommandHandler` - Now implements dual storage:
  1. Serializes results to JSON
  2. Stores detailed results in S3/filesystem
  3. Creates BatchSummary aggregate
  4. Stores summary in database with file references

### 3. Infrastructure Layer
**Created:**
- `BatchSummaryMapper` - Maps between domain and entity objects
  - Converts BatchSummary ↔ BatchSummaryEntity
  - Handles breakdown reconstruction (geographic/sector)
  - Maps all concentration indices and file references

- `S3CalculationResultsStorageService` - S3 implementation
  - Stores calculation results in S3 bucket
  - File naming: `calculated/calc_{batchId}_{timestamp}.json`
  - Uses CoreS3Service for S3 operations
  - Conditional on `risk-calculation.storage.type=s3`

- `LocalCalculationResultsStorageService` - Local filesystem implementation
  - Stores results in local directory
  - Default implementation when S3 is not configured
  - Conditional on `risk-calculation.storage.type=local`

**Already Existed:**
- `JpaBatchSummaryRepository` - JPA repository implementation
- `SpringDataBatchSummaryRepository` - Spring Data repository
- `BatchSummaryEntity` - JPA entity

### 4. Database Schema
**Updated:**
- `V1__Create_batch_summaries_table.sql` - Enhanced with:
  - Geographic breakdown columns (Italy, EU, Non-EU amounts and percentages)
  - Sector breakdown columns (Retail, Sovereign, Corporate, Banking amounts and percentages)
  - Concentration indices (Herfindahl for geographic and sector)
  - Dual file references:
    - `input_file_uri` - Raw ingestion data (what we read)
    - `output_file_uri` - Calculated results (what we write)

## Storage Flow

```
1. Read raw data from input file (ingestion)
   ↓
2. Perform risk calculations
   ↓
3. Serialize complete results to JSON
   ↓
4. Store detailed JSON in S3/filesystem
   ↓
5. Store aggregated summary in database
   ↓
6. Database row contains references to both files
```

## Configuration

### Application Properties
```yaml
risk-calculation:
  storage:
    type: "local"  # Options: local, s3
```

### Storage Implementations
- **Local**: Active when `type=local` (default)
- **S3**: Active when `type=s3`
- Both implement `ICalculationResultsStorageService`
- Spring Boot auto-configuration selects the appropriate implementation

## Data Structure

### Database (Aggregated Summary)
- Single row per batch
- All breakdown percentages and amounts
- HHI concentration indices
- References to input and output files
- Status and timestamps

### S3/Filesystem (Detailed Results)
- Complete JSON with:
  - Batch metadata
  - Bank information
  - Summary section (totals, breakdowns, HHI)
  - Detailed exposures array (all calculated exposures)
  - Individual exposure details (amounts, classification, mitigation)

## Key Benefits

1. **Scalability**: Large exposure datasets don't bloat the database
2. **Performance**: Fast queries on aggregated data
3. **Traceability**: Full audit trail with file references
4. **Flexibility**: Easy to retrieve detailed data when needed
5. **Cost-Effective**: Database stores only essential summary data

## Integration Points

### Command Handler Flow
```java
// 1. Serialize results
Result<String> jsonResult = jsonSerializer.serializeToJson(result);

// 2. Store in S3/filesystem
Result<FileStorageUri> storageResult = 
    calculationResultsStorageService.storeCalculationResults(
        jsonResult.getValue(), batchId, bankId);

// 3. Create summary
BatchSummary batchSummary = createBatchSummary(
    result, inputFileUri, storageResult.getValue());

// 4. Store in database
batchSummaryRepository.save(batchSummary);
```

### File References
- **Input File**: `file:///data/raw/batch_20240331_001.json`
- **Output File**: `s3://risk-analysis-production/calculated/calc_batch_20240331_001.json`

## Testing Considerations

1. **Unit Tests**: Test mapper conversions, serialization
2. **Integration Tests**: Test full storage flow
3. **Mock Repositories**: Use `@ConditionalOnMissingBean` for test mocks
4. **File Storage**: Test both local and S3 implementations

## Next Steps

1. Add integration tests for dual storage flow
2. Implement retrieval endpoints for detailed results
3. Add monitoring for storage operations
4. Consider adding caching for frequently accessed summaries
5. Implement cleanup policies for old calculation results
