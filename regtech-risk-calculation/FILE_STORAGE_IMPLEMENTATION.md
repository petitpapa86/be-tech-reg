# File Storage Implementation for Risk Calculation Module

## Overview

Implemented a complete file storage solution for the risk calculation module that supports both S3 (production) and local filesystem (development) storage, following the same pattern as the ingestion module.

## Components Implemented

### 1. Domain Service Interface

**File**: `domain/src/main/java/.../domain/services/IFileStorageService.java`

Defines the contract for file storage operations:
- `downloadFileContent(FileStorageUri)` - Downloads file content from storage
- `storeCalculationResults(BatchId, String)` - Stores calculation results
- `checkServiceHealth()` - Verifies storage service availability

### 2. S3 File Storage Service

**File**: `infrastructure/src/main/java/.../infrastructure/filestorage/S3FileStorageService.java`

Production implementation using AWS S3:
- Uses `CoreS3Service` for S3 operations
- Supports both `s3://` and `https://` URI formats
- Includes comprehensive error handling and logging
- Activated when `risk-calculation.storage.type=s3`

**Features**:
- Automatic S3 URI parsing
- Metadata tagging (batch-id, timestamp, content-type)
- Server-side encryption support
- Structured logging with metrics

### 3. Local File Storage Service

**File**: `infrastructure/src/main/java/.../infrastructure/filestorage/LocalFileStorageService.java`

Development implementation using local filesystem:
- Stores files in configurable directory (default: `./data/risk-calculations`)
- Automatic directory creation
- File URI format: `file://absolute/path/to/file.json`
- Activated when `risk-calculation.storage.type=local`

**Features**:
- Timestamp-based file naming
- Health check with write verification
- Comprehensive error handling

### 4. Updated FileProcessingService

**File**: `application/src/main/java/.../application/shared/FileProcessingService.java`

Updated to use the new file storage services:
- Removed HTTP client dependency (now handled by storage services)
- Uses injected `IFileStorageService` for all file operations
- Simplified implementation focusing on business logic
- Maintains streaming JSON parsing for memory efficiency

## Configuration

### Application Configuration

```yaml
risk-calculation:
  storage:
    type: s3  # Options: 's3' or 'local'
    
    # S3 configuration
    s3:
      bucket: regtech-risk-calculations
      region: us-east-1
      prefix: calculations/
      encryption: AES256
    
    # Local filesystem configuration
    local:
      base-path: ./data/risk-calculations
      create-directories: true
```

### Profile-Based Configuration

**Development Profile**:
```yaml
spring:
  config:
    activate:
      on-profile: development

risk-calculation:
  storage:
    type: local  # Use local filesystem in development
```

**Production Profile**:
```yaml
spring:
  config:
    activate:
      on-profile: production

risk-calculation:
  storage:
    type: s3  # Use S3 in production
```

## Usage Example

### Downloading and Parsing Exposures

```java
@Service
public class RiskCalculationService {
    
    private final FileProcessingService fileProcessingService;
    
    public void processExposures(FileStorageUri sourceUri, BankId bankId) {
        // Download and parse exposures
        Result<List<CalculatedExposure>> result = 
            fileProcessingService.downloadAndParseExposures(sourceUri, bankId);
        
        if (result.isSuccess()) {
            List<CalculatedExposure> exposures = result.getValue().get();
            // Process exposures...
        }
    }
}
```

### Storing Calculation Results

```java
// Store results
Result<FileStorageUri> storeResult = fileProcessingService.storeCalculationResults(
    batchId,
    exposures,
    aggregationResult
);

if (storeResult.isSuccess()) {
    FileStorageUri uri = storeResult.getValue().get();
    log.info("Results stored at: {}", uri.uri());
}
```

## File Format

### Input File Format (Exposures)

The service expects JSON files with the following structure:

```json
{
  "bank_info": {
    "bank_name": "Community First Bank",
    "abi_code": "08081",
    "lei_code": "815600D7623147C25D86",
    "report_date": "2024-09-12",
    "total_loans": 5
  },
  "loan_portfolio": [
    {
      "loan_id": "LOAN001",
      "exposure_id": "EXP_LOAN001_2024",
      "gross_exposure_amount": 250000.00,
      "net_exposure_amount": 240000.00,
      "currency": "EUR",
      "sector": "CORPORATE",
      "borrower_country": "IT"
    }
  ]
}
```

### Output File Format (Calculation Results)

```json
{
  "batchId": "BATCH001",
  "calculatedAt": "2024-11-27T09:21:57.745Z",
  "summary": {
    "total_exposures": 5,
    "total_amount_eur": 1625000.00,
    "geographic_breakdown": {...},
    "sector_breakdown": {...},
    "concentration_indices": {...}
  },
  "exposures": [...]
}
```

## Storage Locations

### Local Filesystem

- **Base Path**: `./data/risk-calculations/`
- **File Pattern**: `{batchId}_results_{timestamp}.json`
- **Example**: `BATCH001_results_20241127_092157.json`

### S3

- **Bucket**: `regtech-risk-calculations`
- **Key Pattern**: `calculations/{batchId}/results_{timestamp}.json`
- **Example**: `s3://regtech-risk-calculations/calculations/BATCH001/results_20241127_092157.json`

## Error Handling

All operations return `Result<T>` with comprehensive error details:

- `LOCAL_FILE_NOT_FOUND` - File doesn't exist in local storage
- `LOCAL_DOWNLOAD_ERROR` - IO error reading local file
- `LOCAL_STORAGE_ERROR` - Error writing to local filesystem
- `S3_FILE_NOT_FOUND` - File doesn't exist in S3
- `S3_DOWNLOAD_ERROR` - S3 error downloading file
- `S3_UPLOAD_ERROR` - S3 error uploading file
- `FILE_PARSING_ERROR` - JSON parsing error
- `FILE_PROCESSING_ERROR` - Unexpected processing error

## Testing

### Unit Tests

- `LocalFileStorageServiceTest` - Tests local filesystem operations
- Uses JUnit 5 `@TempDir` for isolated testing
- Covers store, retrieve, and health check operations

### Integration Testing

To test with actual files:

1. Place test file in `data/raw/batchId_example.json`
2. Configure local storage: `risk-calculation.storage.type=local`
3. Run the application with development profile
4. Results will be stored in `./data/risk-calculations/`

## Migration Notes

### From Previous Implementation

The previous implementation had:
- HTTP client for direct file downloads
- TODO comments for storage implementation
- No separation between S3 and local storage

The new implementation:
- ✅ Abstracts storage behind domain interface
- ✅ Supports both S3 and local filesystem
- ✅ Profile-based configuration
- ✅ Comprehensive error handling
- ✅ Structured logging with metrics
- ✅ Health checks for both storage types

## Next Steps

1. **Implement Exposure Parsing**: Complete the `parseExposure()` method in `FileProcessingService` to properly parse exposure data from JSON
2. **Add Retry Logic**: Consider adding retry logic to S3 operations for transient failures
3. **Add Metrics**: Integrate with monitoring system to track storage operations
4. **Add Caching**: Consider caching frequently accessed files
5. **Add Presigned URLs**: For S3, implement presigned URL generation for direct client access

## Related Documentation

- [Risk Calculation Module Design](.kiro/specs/risk-calculation-module/design.md)
- [Configuration Organization](.kiro/specs/configuration-organization/CONFIGURATION_REFERENCE.md)
- [Ingestion Module File Storage](../regtech-ingestion/infrastructure/src/main/java/com/bcbs239/regtech/ingestion/infrastructure/filestorage/)
