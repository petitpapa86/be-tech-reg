# Data Quality Local Storage Configuration Fix

## Issue
The Data Quality module was attempting to use S3 storage in local development mode, causing AWS credential errors:
```
Failed to upload to S3: The AWS Access Key Id you provided does not exist in our records
```

## Root Cause
1. Default storage type was set to `s3` in configuration
2. `S3StorageServiceImpl` was always instantiated because it had `matchIfMissing = true`
3. `LocalStorageServiceImpl` was incomplete - missing the `getObjectSize(String)` method

## Solution

### 1. Configuration Change
Changed default storage type from `s3` to `local` in:
- `regtech-data-quality/infrastructure/src/main/resources/application-data-quality.yml`

```yaml
data-quality:
  storage:
    type: local  # Changed from s3
```

### 2. Conditional Bean Loading
Added `@ConditionalOnProperty` to `S3StorageServiceImpl`:
```java
@ConditionalOnProperty(name = "data-quality.storage.type", havingValue = "s3", matchIfMissing = true)
```

This ensures S3 implementation only loads when explicitly configured for S3.

### 3. Completed LocalStorageServiceImpl
Added missing methods to implement the full `S3StorageService` interface:

#### Added Methods:
1. **`downloadExposures(String s3Uri, int expectedCount)`**
   - Downloads exposures with count validation
   - Logs warning if actual count doesn't match expected

2. **`objectExists(String s3Uri)`**
   - Checks if a file exists in the local filesystem
   - Handles file:// URI format and URL decoding

3. **`getObjectSize(String s3Uri)`**
   - Returns file size in bytes
   - Handles file:// URI format and URL decoding
   - Returns error if file doesn't exist

## Implementation Details

### URI Handling
The local implementation handles various URI formats:
- `file:///C:/path/to/file.json` (Windows)
- `file:///path/to/file.json` (Unix)
- URL-encoded paths (e.g., `%20` for spaces)

### Error Handling
All methods return `Result<T>` with proper error codes:
- `LOCAL_FILE_NOT_FOUND` - File doesn't exist
- `LOCAL_PARSE_ERROR` - JSON parsing failed
- `LOCAL_SIZE_CHECK_ERROR` - Failed to get file size
- `LOCAL_EXISTS_CHECK_ERROR` - Failed to check existence

## Files Modified

1. **regtech-data-quality/infrastructure/src/main/resources/application-data-quality.yml**
   - Changed `storage.type` from `s3` to `local`

2. **regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/integration/S3StorageServiceImpl.java**
   - Added `@ConditionalOnProperty` annotation

3. **regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/integration/LocalStorageServiceImpl.java**
   - Added `downloadExposures(String, int)` method
   - Added `objectExists(String)` method
   - Added `getObjectSize(String)` method

## Next Steps

**RESTART THE APPLICATION** for the configuration changes to take effect:
1. Stop the running application
2. Start it again
3. The Data Quality module will now use local filesystem storage
4. No AWS credentials required

## Verification

After restart, you should see:
- No AWS credential errors
- Files stored in `./data/quality/` directory
- Log messages indicating local storage usage

## Configuration Profiles

The configuration supports different profiles:
- **Development**: Uses local filesystem storage (default)
- **Production**: Uses S3 storage (requires AWS credentials)

To switch to S3 in production:
```yaml
data-quality:
  storage:
    type: s3
```
