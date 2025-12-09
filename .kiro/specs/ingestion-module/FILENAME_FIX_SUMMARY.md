# Ingestion Module - Raw Data Filename Fix Summary

## Issue
The `LocalFileStorageService` was generating filenames with a duplicate "batch_" prefix:
```
batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json
                      ↑ duplicate prefix
```

## Root Cause
The filename generation code was adding a "batch_" prefix and timestamp to a `batchId` that already contained the "batch_" prefix:

```java
// OLD CODE (Line 89)
String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
String fileName = String.format("batch_%s_%s.json", timestamp, batchId);
```

Since `batchId` format is: `batch_YYYYMMDD_HHMMSS_UUID`, this resulted in duplicate prefixes.

## Solution
Changed the filename generation to use the `batchId` directly as the filename:

```java
// NEW CODE
String fileName = String.format("%s.json", batchId);
```

## Changes Made

### 1. Updated LocalFileStorageService.java
**File**: `regtech-ingestion/infrastructure/src/main/java/com/bcbs239/regtech/ingestion/infrastructure/filestorage/LocalFileStorageService.java`

**Changes**:
- Removed duplicate "batch_" prefix from filename generation
- Removed unused `TIMESTAMP_FORMATTER` constant
- Removed unused imports (`LocalDateTime`, `DateTimeFormatter`)
- Updated class documentation with correct filename examples

### 2. New Filename Format
**Before**: `batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json`

**After**: `batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json`

The filename now directly uses the batchId, which already contains:
- "batch_" prefix
- Creation timestamp (YYYYMMDD_HHMMSS)
- Unique UUID

## Impact Analysis

### ✅ No Breaking Changes
- Downstream systems use the `S3Reference` object, not the filename directly
- The S3Reference stores the key correctly regardless of filename format
- Database records reference the S3Reference, not the filename

### ✅ Tests Pass
- All existing tests continue to pass
- No test assertions were checking the specific filename format

### ✅ S3 Storage Unaffected
- The S3FileStorageService uses a different key generation strategy
- S3 keys use hierarchical structure: `{prefix}{bankId}/{batchId}/{uuid}-{fileName}`
- No duplicate prefix issue in S3 storage

## Verification

### Test Results
```bash
mvn test -pl regtech-ingestion/infrastructure
```
Result: ✅ All tests pass

### Code Quality
- No compilation errors
- No diagnostic warnings
- Unused code removed (timestamp formatter)

## Files Modified
1. `regtech-ingestion/infrastructure/src/main/java/com/bcbs239/regtech/ingestion/infrastructure/filestorage/LocalFileStorageService.java`

## Documentation Created
1. `.kiro/specs/ingestion-module/FILENAME_FIX.md` - Problem analysis and solution options
2. `.kiro/specs/ingestion-module/FILENAME_FIX_SUMMARY.md` - This summary document

## Next Steps
- ✅ Fix applied and tested
- ✅ Documentation updated
- ✅ Tests passing
- Future uploads will use the corrected filename format
- Existing files with duplicate prefixes will remain (no migration needed as they're still accessible via S3Reference)
