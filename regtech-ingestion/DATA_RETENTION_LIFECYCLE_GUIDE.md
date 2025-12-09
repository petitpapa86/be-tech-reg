# Data Retention and Lifecycle Management Guide

## Overview

The Ingestion Module implements comprehensive data retention and lifecycle policies to meet regulatory compliance requirements, particularly Basel III and BCBS 239 standards for risk data aggregation and reporting.

## Key Components

### 1. DataRetentionPolicy
Defines retention policies with regulatory compliance requirements:
- **Financial Data Policy**: 7-year retention (Basel III compliant)
- **Temporary Processing Data Policy**: 1-year retention for temporary data
- Configurable storage transitions (Standard → Glacier → Deep Archive)
- Legal hold and audit requirements

### 2. DataRetentionService
Core service managing lifecycle policies and compliance reporting:
- **S3 Lifecycle Configuration**: Automatically transitions files between storage classes
- **Compliance Reporting**: Generates detailed compliance reports with violation detection
- **Policy Management**: Manages and validates retention policies

### 3. DataCleanupService
Automated cleanup service for expired data:
- **Automated Deletion**: Removes files that have exceeded retention periods
- **Dry-Run Mode**: Safe testing of cleanup operations
- **Batch Processing**: Processes files in configurable batches to avoid system overload

### 4. ComplianceController
REST API for compliance management:
- **Compliance Reports**: Generate detailed compliance reports
- **Policy Management**: View and update retention policies
- **Manual Cleanup**: Trigger manual cleanup operations
- **Status Monitoring**: Get current compliance status

## S3 Lifecycle Policies

### Default Financial Data Policy
- **Standard Storage**: 0-180 days (6 months)
- **Glacier Storage**: 180-545 days (6-18 months)
- **Deep Archive**: 545+ days (18+ months)
- **Total Retention**: 2555 days (7 years)

### Lifecycle Rules Applied
1. **Glacier Transition**: Files older than 180 days move to Glacier
2. **Deep Archive Transition**: Files older than 18 months move to Deep Archive
3. **Incomplete Multipart Cleanup**: Incomplete uploads cleaned after 7 days
4. **Expiration**: Files deleted after total retention period (if policy allows)

## Compliance Features

### Regulatory Requirements Met
- **Basel III**: 7-year retention for financial risk data
- **BCBS 239**: Risk data aggregation and reporting standards
- **Audit Trail**: Complete audit logging of all operations
- **Data Integrity**: Checksum verification and versioning

### Compliance Reporting
- **Violation Detection**: Identifies non-compliant files and configurations
- **Storage Analysis**: Breakdown by storage class and retention policy
- **Expiry Tracking**: Files approaching expiry and eligible for deletion
- **Compliance Score**: Calculated compliance score with violation penalties

### Audit Requirements
- **Access Logging**: All data access attempts logged
- **Policy Changes**: Retention policy modifications logged
- **Data Deletion**: File deletions logged with justification
- **Audit Log Retention**: Audit logs retained longer than data itself

## Configuration

### Application Properties
```yaml
regtech:
  compliance:
    retention:
      default-policy: FINANCIAL_DATA_DEFAULT
    cleanup:
      enabled: true
      dry-run: true  # Set to false for actual deletion
      batch-size: 100
```

### Scheduled Tasks
- **Lifecycle Policy Update**: Daily at 2 AM
- **Compliance Reporting**: Weekly on Sunday at 3 AM
- **Data Cleanup**: Monthly on 1st day at 4 AM

## API Endpoints

### Compliance Reporting
```
GET /api/v1/ingestion/compliance/report?startDate=2024-01-01&endDate=2024-12-31
GET /api/v1/ingestion/compliance/status
```

### Policy Management
```
GET /api/v1/ingestion/compliance/retention-policies
PUT /api/v1/ingestion/compliance/retention-policies/{policyId}
```

### Lifecycle Management
```
POST /api/v1/ingestion/compliance/configure-lifecycle-policies
POST /api/v1/ingestion/compliance/cleanup
```

## Security and Safety

### Safety Features
- **Dry-Run Mode**: Test cleanup operations without actual deletion
- **Legal Hold Protection**: Files with legal holds cannot be deleted
- **Batch Processing**: Prevents system overload during cleanup
- **Error Handling**: Comprehensive error handling and logging

### Security Measures
- **Access Control**: JWT-based authentication for all endpoints
- **Audit Logging**: All operations logged for compliance
- **Data Encryption**: AES-256 encryption for all stored data
- **Checksum Verification**: Data integrity verification

## Monitoring and Alerting

### Metrics Tracked
- Files under retention management
- Storage class distribution
- Compliance violations
- Cleanup operation results

### Alerts Generated
- High-severity compliance violations
- Failed cleanup operations
- Files approaching expiry
- Policy configuration errors

## Best Practices

### Policy Configuration
1. Always validate policies meet regulatory requirements
2. Test policy changes in non-production environments
3. Document policy changes with business justification
4. Regular review of policy effectiveness

### Cleanup Operations
1. Always test with dry-run mode first
2. Monitor cleanup operations for errors
3. Verify compliance after cleanup operations
4. Maintain audit trail of all deletions

### Compliance Monitoring
1. Regular compliance report generation
2. Proactive monitoring of approaching expiry dates
3. Immediate investigation of high-severity violations
4. Regular policy review and updates

## Troubleshooting

### Common Issues
1. **S3 Lifecycle Policy Conflicts**: Check for existing policies
2. **Permission Errors**: Verify S3 and database permissions
3. **Cleanup Failures**: Check dry-run mode and file permissions
4. **Compliance Violations**: Review retention policies and file ages

### Diagnostic Commands
```bash
# Check compliance status
curl -H "Authorization: Bearer $JWT" /api/v1/ingestion/compliance/status

# Generate compliance report
curl -H "Authorization: Bearer $JWT" /api/v1/ingestion/compliance/report?startDate=2024-01-01&endDate=2024-12-31

# Test cleanup (dry-run)
curl -X POST -H "Authorization: Bearer $JWT" /api/v1/ingestion/compliance/cleanup
```

## Implementation Notes

This implementation addresses the requirements:
- **10.5**: Implements lifecycle policies per regulatory requirements
- **10.7**: Provides detailed processing audit reports
- **9.6**: Automatically archives old files to Glacier storage class

The system is designed to be production-ready with comprehensive error handling, monitoring, and safety features to ensure regulatory compliance while maintaining data integrity and system reliability.