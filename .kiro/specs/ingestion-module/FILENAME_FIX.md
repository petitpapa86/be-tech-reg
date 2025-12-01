# Ingestion Module - Raw Data Filename Fix

## Problem Statement

The `LocalFileStorageService` currently generates filenames with a duplicate "batch_" prefix, resulting in filenames like:
```
batch_20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json
```

This occurs because:
1. The `batchId` already contains the "batch_" prefix: `batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023`
2. The filename generation adds another "batch_" prefix and timestamp: `batch_{timestamp}_{batchId}.json`

## Proposed Solution

**Option 1: Use batchId as-is (Recommended)**
- Filename: `batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json`
- Rationale: The batchId already contains all necessary information (timestamp and UUID)
- Simplest change with minimal risk

**Option 2: Add timestamp without "batch_" prefix**
- Filename: `20251127_164816_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json`
- Rationale: Preserves storage timestamp separate from batch creation timestamp
- More complex, but provides additional metadata

**Option 3: Add bank prefix**
- Filename: `{bankId}_batch_20251127_164759_6e34fc57-2579-4dc6-a8e4-1417a1cfd023.json`
- Rationale: Makes it easier to identify files by bank
- Useful for manual file browsing

## Recommendation

**Use Option 1** - Just use the batchId as the filename (with .json extension). This is the simplest solution that:
- Eliminates the duplicate prefix
- Maintains all necessary information (timestamp and UUID)
- Requires minimal code changes
- Has no impact on downstream systems (they use S3Reference, not filename directly)

## Implementation

### File to Modify
`regtech-ingestion/infrastructure/src/main/java/com/bcbs239/regtech/ingestion/infrastructure/filestorage/LocalFileStorageService.java`

### Current Code (Line 89)
```java
String fileName = String.format("batch_%s_%s.json", timestamp, batchId);
```

### Proposed Code
```java
String fileName = String.format("%s.json", batchId);
```

### Impact Analysis
- **Breaking Changes**: None - downstream systems use S3Reference object, not filename
- **Database**: No changes needed - S3Reference stores the key
- **Tests**: May need to update test assertions that check filename format
- **Documentation**: Update any documentation that references filename format

## Testing Checklist
- [ ] Verify new files are created with correct format
- [ ] Verify existing file references still work
- [ ] Verify S3Reference is correctly populated
- [ ] Update integration tests
- [ ] Verify no duplicate "batch_" prefix in new files
