# S3 Storage Service API Fix

## Issue
The `S3CalculationResultsStorageService` had compilation errors due to API mismatches with the `CoreS3Service`.

## Errors Fixed
1. **Missing methods**: `uploadFile()`, `downloadFile()`, `checkServiceHealth()` - these methods don't exist in CoreS3Service
2. **Wrong method name**: `getBucketName()` should be `getBucket()`
3. **Type mismatches**: Result<Optional<ErrorDetail>> instead of Result<ErrorDetail>

## Changes Made

### 1. Updated Imports
- Added `S3Properties` import
- Added `PutObjectResponse` import from AWS SDK
- Added `HashMap` import
- Removed `ByteArrayInputStream` (no longer needed)

### 2. Updated Constructor
- Added `S3Properties` dependency injection to access bucket configuration

### 3. Fixed `storeCalculationResults()` Method
**Before:**
```java
Result<String> uploadResult = coreS3Service.uploadFile(s3Key, inputStream, jsonBytes.length, "application/json");
```

**After:**
```java
String bucket = s3Properties.getBucket();
PutObjectResponse response = coreS3Service.putString(
    bucket, 
    s3Key, 
    jsonContent, 
    "application/json", 
    new HashMap<>(), 
    null
);
String s3Uri = String.format("s3://%s/%s", bucket, s3Key);
```

### 4. Fixed `retrieveCalculationResults()` Method
**Before:**
```java
Result<InputStream> downloadResult = coreS3Service.downloadFile(s3Key);
try (InputStream inputStream = downloadResult.getValue()) {
    // ...
}
```

**After:**
```java
String bucket = s3Properties.getBucket();
try (InputStream inputStream = coreS3Service.getObjectStream(bucket, s3Key)) {
    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    return Result.success(jsonContent);
}
```

### 5. Fixed `checkServiceHealth()` Method
**Before:**
```java
Result<Boolean> healthResult = coreS3Service.checkServiceHealth();
```

**After:**
```java
String bucket = s3Properties.getBucket();
String testKey = S3_PREFIX + "health-check-test";
try {
    coreS3Service.headObject(bucket, testKey);
} catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
    // Expected - key doesn't exist but S3 is accessible
}
```

## CoreS3Service API Reference

The correct CoreS3Service methods are:
- `putBytes(bucket, key, content, contentType, metadata, kmsKeyId)` - Upload bytes
- `putString(bucket, key, content, contentType, metadata, kmsKeyId)` - Upload string
- `getObjectStream(bucket, key)` - Download as InputStream
- `headObject(bucket, key)` - Check if object exists
- `deleteObject(bucket, key)` - Delete object
- `generatePresignedUrl(bucket, key, expiration, urlConsumer)` - Generate presigned URL

## Build Status
✅ **FIXED** - All compilation errors resolved. Build successful.

```
[INFO] regtech-risk-calculation-infrastructure ............ SUCCESS [  7.803 s]
[INFO] BUILD SUCCESS
```

## Related Files
- `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/filestorage/S3CalculationResultsStorageService.java`
- `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/filestorage/CoreS3Service.java`
- `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/s3/S3Properties.java`
