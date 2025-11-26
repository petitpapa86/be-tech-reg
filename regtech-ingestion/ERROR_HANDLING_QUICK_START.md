# Ingestion Error Handling - Quick Start Guide

## Overview
The ingestion module now has automatic retry for failed batch processing operations.

## Key Features

✅ **Automatic Retry**: Failed batches are automatically retried up to 5 times  
✅ **Event-Based**: Uses domain events for decoupled error handling  
✅ **Local Storage**: Development mode uses local filesystem (no AWS needed)  
✅ **Bank Validation**: Strict validation - bank must exist before processing  

## Quick Setup

### 1. Development Mode (Local Storage)

**application.yml:**
```yaml
spring:
  profiles:
    active: development

ingestion:
  storage:
    type: local
    local:
      base-path: ./data/ingestion
```

**Result:**
- Files stored in `./data/ingestion/`
- No AWS credentials needed
- Perfect for local development

### 2. Production Mode (S3 Storage)

**application.yml:**
```yaml
spring:
  profiles:
    active: production

ingestion:
  storage:
    type: s3
    s3:
      bucket: regtech-ingestion
      region: us-east-1
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
```

**Result:**
- Files stored in S3
- Encrypted with AES256
- Production-ready

## How It Works

### When Processing Succeeds
```
1. Upload file → 2. Process → 3. Store → 4. Complete ✓
```

### When Processing Fails
```
1. Upload file → 2. Process → 3. Error ✗
   ↓
4. Save failure to database
   ↓
5. Retry after 10s, 30s, 1min, 5min, 10min
   ↓
6. Success ✓ OR Permanent failure after 5 attempts
```

## Common Scenarios

### Scenario 1: Bank Not Found

**Problem:**
```
Bank with ID 'BANK123' not found
```

**What Happens:**
1. Batch marked as failed
2. Failure saved for retry
3. System retries 5 times (in case bank is added)
4. If still not found after 5 attempts → permanent failure

**Solution:**
- Add the bank to the system
- Failure will auto-retry and succeed

### Scenario 2: Temporary Database Error

**Problem:**
```
Database connection timeout
```

**What Happens:**
1. Failure saved for retry
2. System retries with exponential backoff
3. Usually succeeds on retry

**Solution:**
- No action needed - automatic retry handles it

### Scenario 3: Invalid File Format

**Problem:**
```
JSON parsing error - invalid format
```

**What Happens:**
1. Batch marked as failed
2. No retry (validation errors are permanent)

**Solution:**
- Fix the file format
- Re-upload the file

## Configuration Options

### Retry Settings

```yaml
ingestion:
  retry:
    max-retries: 5  # Number of retry attempts (default: 5)
```

### Storage Settings

```yaml
ingestion:
  storage:
    type: local  # 'local' or 's3'
    
    local:
      base-path: ./data/ingestion
      create-directories: true
    
    s3:
      bucket: regtech-ingestion
      region: us-east-1
      prefix: raw/
      encryption: AES256
```

## Monitoring

### Check Failed Events

**SQL Query:**
```sql
-- Pending retries
SELECT * FROM event_processing_failures 
WHERE status = 'PENDING' 
ORDER BY next_retry_at;

-- Permanently failed
SELECT * FROM event_processing_failures 
WHERE status = 'FAILED';

-- Recently succeeded
SELECT * FROM event_processing_failures 
WHERE status = 'SUCCEEDED' 
AND updated_at > NOW() - INTERVAL '1 hour';
```

### Log Messages

**Look for:**
```
Batch processing failure saved for retry
Retrying batch processing
Batch processing retry succeeded
event retry permanently failed
```

## Troubleshooting

### Issue: Retries Not Happening

**Check:**
1. Is `EventRetryProcessor` enabled?
2. Check `event_processing_failures` table
3. Verify `next_retry_at` timestamp

**Solution:**
```yaml
# Ensure retry processor is enabled
event-retry:
  enabled: true
  interval: PT1M  # Check every minute
```

### Issue: Files Not Stored Locally

**Check:**
1. Storage type configuration
2. Directory permissions
3. Base path exists

**Solution:**
```bash
# Create directory
mkdir -p ./data/ingestion

# Check permissions
ls -la ./data/

# Verify configuration
grep "storage.type" application-ingestion.yml
```

### Issue: Bank Always Not Found

**Check:**
1. Bank exists in database
2. Bank is active
3. Bank ID matches exactly

**Solution:**
```sql
-- Check bank exists
SELECT * FROM banks WHERE bank_id = 'BANK123';

-- Check bank is active
SELECT * FROM banks WHERE bank_id = 'BANK123' AND status = 'ACTIVE';
```

## Testing

### Test Retry Mechanism

```java
@Test
void testBatchProcessingRetry() {
    // 1. Upload file with invalid bank ID
    var command = new UploadAndProcessFileCommand(...);
    var result = handler.handle(command);
    
    // 2. Verify failure saved
    var failures = failureRepository.findByBatchId(batchId);
    assertThat(failures).hasSize(1);
    
    // 3. Add bank to system
    bankRepository.save(new Bank(...));
    
    // 4. Trigger retry
    retryProcessor.processFailedEvents();
    
    // 5. Verify success
    var batch = batchRepository.findById(batchId);
    assertThat(batch.getStatus()).isEqualTo(BatchStatus.COMPLETED);
}
```

### Test Local Storage

```java
@Test
@ActiveProfiles("development")
void testLocalStorage() {
    // Upload file
    var result = handler.handle(command);
    
    // Verify file exists locally
    Path filePath = Paths.get("./data/ingestion/batch_*.json");
    assertThat(Files.exists(filePath)).isTrue();
}
```

## Best Practices

1. **Use Development Profile Locally**
   - Faster development
   - No AWS costs
   - Easier debugging

2. **Monitor Failed Events**
   - Set up alerts for permanent failures
   - Review retry patterns
   - Identify systemic issues

3. **Handle Permanent Failures**
   - Investigate root cause
   - Fix underlying issue
   - Consider manual reprocessing

4. **Clean Up Old Failures**
   - Archive succeeded events
   - Remove old permanent failures
   - Keep database lean

## Support

For issues or questions:
1. Check logs in `regtech-app/logs/`
2. Query `event_processing_failures` table
3. Review `BATCH_PROCESSING_ERROR_HANDLING.md` for details
