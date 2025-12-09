# S3 Configuration Fix

## Problem
Application failed to start with the following error:
```
Parameter 0 of constructor in com.bcbs239.regtech.dataquality.infrastructure.integration.S3StorageServiceImpl 
required a bean of type 'com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service' that could not be found.
```

## Root Cause
The `CoreS3Service` bean was not being created because the Spring component scan in `RegtechApplication` was not including the packages where S3 configuration is defined:
- `com.bcbs239.regtech.core.infrastructure.s3` (contains `S3Config` with `@Bean` definitions for `S3Client` and `S3Presigner`)
- `com.bcbs239.regtech.core.infrastructure.filestorage` (contains `CoreS3Service`)

## Solution
1. Added the missing packages to the `@ComponentScan` annotation in `RegtechApplication.java`:
```java
"com.bcbs239.regtech.core.infrastructure.s3",
"com.bcbs239.regtech.core.infrastructure.filestorage",
```

2. Added `@EnableConfigurationProperties(S3Properties.class)` to `S3Config` to ensure `S3Properties` bean is created when `S3Config` is loaded.

## Affected Modules
The following modules depend on `CoreS3Service` and will benefit from this fix:
1. **regtech-data-quality** - `S3StorageServiceImpl` for storing validation results
2. **regtech-risk-calculation** - `S3FileStorageService` and `S3CalculationResultsStorageService` for storing calculation results
3. **regtech-ingestion** - `S3FileStorageService` for file uploads
4. **regtech-report-generation** - `S3ReportStorageService` for storing generated reports

## Configuration
The S3 configuration is controlled by properties in `application.yml`:
- `ingestion.s3.enabled` - Enable/disable S3 (defaults to `true`)
- `ingestion.s3.bucket` - S3 bucket name
- `ingestion.s3.region` - AWS region
- `ingestion.s3.access-key` - AWS access key (optional, uses default credentials if not set)
- `ingestion.s3.secret-key` - AWS secret key (optional)
- `ingestion.s3.endpoint` - Custom S3 endpoint (optional, for LocalStack or MinIO)
- `ingestion.s3.kms-key-id` - KMS key for encryption (optional)

## Testing
After this fix, the application should start successfully and all S3-dependent services should be able to inject `CoreS3Service`.
