# Storage Configuration

## Overview

The regtech-ingestion module supports two storage backends for processed files:

1. **Local Filesystem Storage** - For development and testing
2. **AWS S3 Storage** - For production environments

## Configuration

Storage type is controlled by the `storage.type` property in `application.yml`:

```yaml
regtech:
  storage:
    type: local  # Options: 'local' or 's3' (default: s3)
```

### Local Filesystem Storage

**When to use:** Development, testing, local demos

**Configuration:**
```yaml
regtech:
  storage:
    type: local
```

**Behavior:**
- Files are stored in `data/raw/` directory relative to application root
- Directory structure: `data/raw/batch_{timestamp}_{batchId}.json`
- Example: `data/raw/batch_20240331_120530_001.json`
- No AWS credentials required
- Files are immediately accessible on local filesystem

**Implementation:** `LocalFileStorageService`

**S3Reference format (for compatibility):**
```json
{
  "bucket": "local-storage",
  "key": "batch_20240331_120530_001.json",
  "versionId": "1234567890",  // File last modified timestamp
  "uri": "file:///path/to/data/raw/batch_20240331_120530_001.json"
}
```

### AWS S3 Storage

**When to use:** Production, staging, cloud deployments

**Configuration:**
```yaml
regtech:
  storage:
    type: s3  # or omit - s3 is the default
  s3:
    bucket: regtech-data-storage
    region: us-east-1
    prefix: raw/
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}
    endpoint: ${AWS_S3_ENDPOINT:}  # Optional: for LocalStack/MinIO testing
```

**Behavior:**
- Files are uploaded to AWS S3
- Directory structure: `{prefix}/{bankId}/{batchId}/{uuid}-{filename}`
- Example: `raw/BANK001/BATCH123/a1b2c3d4-data.json`
- Requires AWS credentials or IAM role
- Versioning supported (if enabled on bucket)

**Implementation:** `S3FileStorageService` (via `S3StorageServiceImpl` adapter)

## Architecture

### Component Structure

```
regtech-ingestion/infrastructure/filestorage/
├── LocalFileStorageService.java         # Local filesystem implementation
├── S3StorageServiceImpl.java            # S3 adapter (production)
└── S3FileStorageService.java            # Core S3 implementation
```

### Conditional Bean Loading

Both storage implementations are Spring beans with `@ConditionalOnProperty`:

```java
// Local filesystem - active when storage.type=local
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = false)
public class LocalFileStorageService implements S3StorageService

// S3 storage - active when storage.type=s3 or by default
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class S3StorageServiceImpl implements S3StorageService

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorageService implements FileStorageService
```

### Design Pattern: Adapter

`S3StorageServiceImpl` acts as an adapter between:
- **Application layer:** `ProcessBatchCommandHandler.S3StorageService` interface
- **Domain layer:** `FileStorageService` interface
- **Infrastructure layer:** `S3FileStorageService` implementation

This allows the application layer to depend on its own interface while the actual implementation is in infrastructure.

## Usage Examples

### Development Environment

Set in `application.yml`:
```yaml
spring:
  profiles:
    active: development

regtech:
  storage:
    type: local
```

No AWS credentials needed. Files appear in `data/raw/` directory.

### Production Environment

Set in `application.yml`:
```yaml
spring:
  profiles:
    active: production

regtech:
  storage:
    type: s3
  s3:
    bucket: regtech-prod-storage
    region: us-east-1
```

Set environment variables:
```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

Or use IAM roles (recommended for EC2/ECS/Lambda).

### Testing with LocalStack

For integration tests using LocalStack:
```yaml
regtech:
  storage:
    type: s3
  s3:
    bucket: test-bucket
    region: us-east-1
    endpoint: http://localhost:4566  # LocalStack endpoint
    access-key: test
    secret-key: test
```

## File Naming Convention

### Local Storage
Format: `batch_{timestamp}_{batchId}.json`
- `timestamp`: yyyyMMdd_HHmmss format
- `batchId`: Batch identifier from processing

### S3 Storage
Format: `{prefix}/{bankId}/{batchId}/{uuid}-{originalFilename}`
- `prefix`: Configured path prefix (e.g., "raw/")
- `bankId`: Bank identifier
- `batchId`: Batch identifier
- `uuid`: 8-character unique identifier
- `originalFilename`: Original uploaded filename

## Directory Structure Example

### Local Filesystem
```
data/
└── raw/
    ├── batch_20240331_001.json
    ├── batch_20240331_002.json
    ├── batch_20240331_003.json
    └── batch_20240331_004.json
```

### S3 Bucket
```
regtech-data-storage/
└── raw/
    ├── BANK001/
    │   ├── BATCH001/
    │   │   └── a1b2c3d4-loans.json
    │   └── BATCH002/
    │       └── e5f6g7h8-exposures.json
    └── BANK002/
        └── BATCH003/
            └── i9j0k1l2-data.json
```

## Error Handling

Both implementations return `Result<S3Reference>` with appropriate error details:

### Local Storage Errors
- `NULL_FILE_STREAM`: Input stream is null
- `NULL_FILE_METADATA`: Metadata is missing
- `INVALID_BATCH_ID`: Batch ID validation failed
- `INVALID_BANK_ID`: Bank ID validation failed
- `INVALID_EXPOSURE_COUNT`: Negative exposure count
- `LOCAL_STORAGE_ERROR`: Filesystem I/O error
- `STORAGE_ERROR`: Unexpected error

### S3 Storage Errors
- `S3_STORAGE_ERROR`: S3 API errors (permissions, bucket not found, etc.)
- `FILE_READ_ERROR`: Unable to read input stream
- `STORAGE_ERROR`: Unexpected AWS SDK errors

## Performance Considerations

### Local Storage
- **Pros:** 
  - Fast local disk I/O
  - No network latency
  - No AWS costs
  - Easy debugging
- **Cons:**
  - Not suitable for distributed systems
  - No automatic replication
  - Limited by local disk space

### S3 Storage
- **Pros:**
  - Highly durable (99.999999999%)
  - Automatically replicated
  - Scalable storage
  - Versioning support
  - Lifecycle policies
- **Cons:**
  - Network latency
  - AWS costs
  - Requires credentials management

## Migration Path

To migrate from local to S3:

1. Change configuration:
   ```yaml
   storage:
     type: s3  # Changed from 'local'
   ```

2. Configure S3 bucket and credentials

3. Restart application

4. (Optional) Migrate existing files from `data/raw/` to S3 bucket

## Best Practices

1. **Development:** Use `storage.type: local` for faster iteration
2. **CI/CD:** Use LocalStack with `storage.type: s3` for integration tests
3. **Staging/Production:** Use `storage.type: s3` with real AWS S3
4. **Credentials:** Use IAM roles instead of access keys when possible
5. **Monitoring:** Monitor S3 bucket size and costs in production
6. **Backup:** Configure S3 versioning and lifecycle policies

## Troubleshooting

### Local Storage: Permission Denied
- Ensure application has write permissions to `data/raw/` directory
- Check disk space availability

### S3 Storage: Access Denied
- Verify AWS credentials are correct
- Check IAM policy grants PutObject permission
- Verify bucket name and region are correct

### S3 Storage: Bucket Not Found
- Create S3 bucket manually or via IaC
- Verify bucket name matches configuration
- Check bucket is in the correct region

## Configuration Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `storage.type` | String | `s3` | Storage backend: `local` or `s3` |
| `s3.bucket` | String | - | S3 bucket name (required for S3) |
| `s3.region` | String | - | AWS region (required for S3) |
| `s3.prefix` | String | `raw/` | S3 key prefix for organization |
| `s3.access-key` | String | - | AWS access key (or use IAM role) |
| `s3.secret-key` | String | - | AWS secret key (or use IAM role) |
| `s3.endpoint` | String | - | Custom endpoint (for LocalStack/MinIO) |
