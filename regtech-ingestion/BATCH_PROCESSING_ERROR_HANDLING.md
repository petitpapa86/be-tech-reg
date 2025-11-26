# Batch Processing Error Handling Implementation

## Overview
This document describes the enhanced error handling and retry mechanism for batch processing in the ingestion module.

## Architecture

### 1. Event-Based Failure Handling

When batch processing fails in `UploadAndProcessFileCommandHandler`, the system now:

1. **Publishes a failure event** (`BatchProcessingFailedEvent`) containing:
   - Batch ID
   - Bank ID
   - File name
   - Error message
   - Error type
   - Temporary file key (for retry)

2. **Saves failure to database** via `BatchProcessingFailureHandler`:
   - Creates an `EventProcessingFailure` record
   - Stores event payload and metadata
   - Configures retry attempts (default: 5)

3. **Automatic retry** via `EventRetryProcessor`:
   - Scheduled processor checks for failed events
   - Deserializes event and invokes `BatchProcessingRetryHandler`
   - Uses exponential backoff (10s, 30s, 1min, 5min, 10min)

### 2. Bank Not Found Error Handling

In `ProcessBatchCommandHandler`, when a bank is not found:

1. **Validation is strict**: No mock bank creation in production
2. **Batch is marked as failed** with clear error message
3. **Error is returned** with proper error type (`VALIDATION_ERROR`)
4. **Failure event is published** for retry (in case bank is added later)

### 3. Local File Storage for Development

The system supports two storage modes:

#### Development Mode (Local Filesystem)
- Storage type: `local`
- Base path: `./data/ingestion`
- Files stored in: `data/raw/batch_YYYYMMDD_HHMMSS_<batchId>.json`
- No AWS credentials required

#### Production Mode (AWS S3)
- Storage type: `s3`
- Bucket: `regtech-ingestion`
- Prefix: `raw/`
- Encryption: AES256

## Configuration

### application-ingestion.yml

```yaml
ingestion:
  # Storage configuration
  storage:
    type: s3  # Options: 's3' or 'local'
    
    s3:
      bucket: regtech-ingestion
      region: us-east-1
      prefix: raw/
      encryption: AES256
    
    local:
      base-path: ./data/ingestion
      create-directories: true
  
  # Retry settings
  retry:
    max-retries: 5

---
# Development profile
spring:
  config:
    activate:
      on-profile: development

ingestion:
  storage:
    type: local  # Use local filesystem in development
```

## Components

### New Components

1. **BatchProcessingFailedEvent**
   - Domain event for batch processing failures
   - Contains all context needed for retry

2. **BatchProcessingFailureHandler**
   - Listens to `BatchProcessingFailedEvent`
   - Saves failure to `EventProcessingFailure` table
   - Configures retry attempts
   - EventRetryProcessor will automatically deserialize and retry the event

### Modified Components

1. **UploadAndProcessFileCommandHandler**
   - Publishes `BatchProcessingFailedEvent` on async processing failure
   - Includes error details and temp file key

2. **ProcessBatchCommandHandler**
   - Strict bank validation (no mock creation)
   - Clear error messages for bank not found
   - Proper error logging

3. **LocalFileStorageService**
   - Fixed conditional property to match configuration
   - Uses `ingestion.storage.type` instead of `storage.type`

4. **S3FileStorageService**
   - Fixed conditional property to match configuration

5. **EventProcessingFailureEntity**
   - Increased `event_type` column length from 255 to 500 characters
   - Accommodates longer fully-qualified class names

## Flow Diagrams

### Success Flow
```
Upload Request
  → Store temp file
  → Create batch record
  → Start async processing
    → Parse file
    → Validate content
    → Fetch bank info ✓
    → Store in S3/Local
    → Complete batch
    → Publish BatchIngestedEvent
```

### Failure Flow with Retry
```
Upload Request
  → Store temp file
  → Create batch record
  → Start async processing
    → Parse file
    → Validate content
    → Fetch bank info ✗ (Bank not found)
    → Mark batch as failed
    → Publish BatchProcessingFailedEvent
      → Save to EventProcessingFailure table
      → EventRetryProcessor picks up (after backoff)
        → Deserialize event
        → Invoke BatchProcessingRetryHandler
          → Retry processing
          → If still fails: increment retry count
          → If max retries reached: mark as permanently failed
```

## Retry Strategy

### Exponential Backoff
- Attempt 1: 10 seconds
- Attempt 2: 30 seconds
- Attempt 3: 1 minute
- Attempt 4: 5 minutes
- Attempt 5: 10 minutes

### Max Retries
- Default: 5 attempts
- Configurable via `ingestion.retry.max-retries`

### Permanent Failure
After max retries:
- Status changed to `FAILED`
- `EventProcessingPermanentlyFailed` event published
- Manual intervention required

## Error Types

### Recoverable Errors (Retry)
- `BANK_NOT_FOUND`: Bank might be added later
- `DATABASE_ERROR`: Temporary database issues
- `STORAGE_ERROR`: Temporary storage issues
- `SYSTEM_ERROR`: Transient system errors

### Non-Recoverable Errors (No Retry)
- `VALIDATION_ERROR`: Invalid file format (after parsing)
- `INVALID_FILE_EXTENSION`: Wrong file type
- `FILE_TOO_LARGE`: Exceeds size limits

## Monitoring

### Log Messages

**Success:**
```
Batch processing retry succeeded; details={batchId=...}
```

**Failure:**
```
Batch processing retry failed; details={batchId=..., error=...}
```

**Permanent Failure:**
```
event retry permanently failed; details={failureId=..., retryCount=5}
```

### Database Queries

Check failed events:
```sql
SELECT * FROM event_processing_failures 
WHERE status = 'PENDING' 
AND next_retry_at <= NOW();
```

Check permanently failed events:
```sql
SELECT * FROM event_processing_failures 
WHERE status = 'FAILED';
```

## Testing

### Development Testing

1. Start application with development profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=development
```

2. Upload a file with invalid bank ID
3. Check `data/raw/` directory for stored files
4. Monitor logs for retry attempts
5. Verify `event_processing_failures` table

### Production Testing

1. Configure S3 credentials
2. Set profile to production
3. Test with valid and invalid scenarios
4. Monitor CloudWatch logs
5. Check S3 bucket for stored files

## Database Schema Changes

### Migration V2: Increase event_type Column Length

**File:** `regtech-app/src/main/resources/db/migration/V2__Increase_event_type_length.sql`

```sql
ALTER TABLE event_processing_failures 
ALTER COLUMN event_type TYPE VARCHAR(500);
```

**Reason:** The original 255 character limit was too short for fully-qualified class names like `com.bcbs239.regtech.ingestion.domain.batch.events.BatchProcessingFailedEvent`.

**Error Fixed:** `ERRORE: il valore è troppo lungo per il tipo character varying(255)`

## Future Enhancements

1. **Dead Letter Queue**: Move permanently failed events to DLQ
2. **Manual Retry**: Admin UI to manually retry failed batches
3. **Alerting**: Send notifications on permanent failures
4. **Metrics**: Track retry success/failure rates
5. **Cleanup**: Automatic cleanup of old failed events
