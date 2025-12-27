# Module Routes Security Audit

## Overview
This document audits all RouterFunction definitions across modules to identify:
- Routes that should be public (no authentication required)
- Routes that require authentication only
- Routes that require specific permissions
- Current security pattern compliance

## Audit Results

### 1. Ingestion Module

#### 1.1 Health Endpoint
**File:** `regtech-ingestion/presentation/src/main/java/com/bcbs239/regtech/ingestion/presentation/health/IngestionHealthController.java`
- **Route:** `GET /api/v1/ingestion/health`
- **Current Security:** Uses `RouterAttributes.withAttributes` with `null` permissions ✅
- **Should Be:** Public (no authentication required)
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** Add to public paths configuration

#### 1.2 Upload and Process File
**File:** `regtech-ingestion/presentation/src/main/java/com/bcbs239/regtech/ingestion/presentation/batch/upload/UploadAndProcessFileController.java`
- **Route:** `POST /api/v1/ingestion/upload-and-process`
- **Current Security:** Uses `RouterAttributes.withAttributes` with permissions `["ingestion:upload-process"]` ✅
- **Should Be:** Requires authentication + specific permission
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** None

#### 1.3 Batch Status Query
**File:** `regtech-ingestion/presentation/src/main/java/com/bcbs239/regtech/ingestion/presentation/batch/status/BatchStatusController.java`
- **Route:** `GET /api/v1/ingestion/batch/{batchId}/status`
- **Route:** `POST /api/v1/ingestion/batch/{batchId}/status`
- **Current Security:** Uses `.withAttribute()` method (old pattern) ❌
- **Should Be:** Requires authentication + specific permission `["ingestion:status:view"]`
- **Status:** ❌ NON-COMPLIANT - Not using RouterAttributes.withAttributes()
- **Action Required:** Update to use RouterAttributes.withAttributes() pattern

#### 1.4 Retention Policies (Disabled)
**File:** `regtech-ingestion/presentation/src/main/java/com/bcbs239/regtech/ingestion/presentation/compliance/policies/RetentionPoliciesController.java`
- **Route:** `GET /api/v1/ingestion/compliance/retention-policies`
- **Route:** `PUT /api/v1/ingestion/compliance/retention-policies/{policyId}`
- **Current Security:** Uses `.withAttribute()` method (old pattern) ❌
- **Should Be:** Requires authentication + specific permission `["ingestion:compliance:manage"]`
- **Status:** ❌ NON-COMPLIANT - Not using RouterAttributes.withAttributes()
- **Note:** Currently disabled via @ConditionalOnProperty
- **Action Required:** Update to use RouterAttributes.withAttributes() pattern

#### 1.5 Compliance Reports (Disabled)
**File:** `regtech-ingestion/presentation/src/main/java/com/bcbs239/regtech/ingestion/presentation/compliance/reports/ComplianceReportsController.java`
- **Route:** `GET /api/v1/ingestion/compliance/report`
- **Route:** `GET /api/v1/ingestion/compliance/status`
- **Current Security:** Uses `.withAttribute()` method (old pattern) ❌
- **Should Be:** Requires authentication + specific permission
- **Status:** ❌ NON-COMPLIANT - Not using RouterAttributes.withAttributes()
- **Note:** Currently disabled via @ConditionalOnProperty
- **Action Required:** Update to use RouterAttributes.withAttributes() pattern

### 2. Data Quality Module

#### 2.1 Health Endpoints
**File:** `regtech-data-quality/presentation/src/main/java/com/bcbs239/regtech/dataquality/presentation/monitoring/QualityHealthRoutes.java`
- **Routes:**
  - `GET /api/v1/data-quality/health`
  - `GET /api/v1/data-quality/health/database`
  - `GET /api/v1/data-quality/health/s3`
  - `GET /api/v1/data-quality/health/validation-engine`
- **Current Security:** Uses `RouterAttributes.withAttributes` with `null` permissions ✅
- **Should Be:** Public (no authentication required)
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** Verify all health paths are in public paths configuration

#### 2.2 Metrics Endpoint
**File:** `regtech-data-quality/presentation/src/main/java/com/bcbs239/regtech/dataquality/presentation/monitoring/QualityHealthRoutes.java`
- **Route:** `GET /api/v1/data-quality/metrics`
- **Current Security:** Uses `RouterAttributes.withAttributes` with permissions `["data-quality:metrics:view"]` ✅
- **Should Be:** Requires authentication + specific permission
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** None

#### 2.3 Quality Report Endpoints
**File:** `regtech-data-quality/presentation/src/main/java/com/bcbs239/regtech/dataquality/presentation/reports/QualityReportRoutes.java`
- **Routes:**
  - `GET /api/v1/data-quality/reports?bankId=...` - Permission: `["data-quality:reports:view"]`
  - `GET /api/v1/data-quality/trends` - Permission: `["data-quality:trends:view"]`
- **Current Security:** Uses `RouterAttributes.withAttributes` with specific permissions ✅
- **Should Be:** Requires authentication + specific permissions
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** None

### 3. Risk Calculation Module

#### 3.1 Health Endpoints
**File:** `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/monitoring/RiskCalculationHealthRoutes.java`
- **Routes:**
  - `GET /api/v1/risk-calculation/health`
  - `GET /api/v1/risk-calculation/health/database`
  - `GET /api/v1/risk-calculation/health/file-storage`
  - `GET /api/v1/risk-calculation/health/currency-conversion`
- **Current Security:** Uses `RouterAttributes.withAttributes` with `null` permissions ✅
- **Should Be:** Public (no authentication required)
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** Verify all health paths are in public paths configuration

#### 3.2 Metrics Endpoint
**File:** `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/monitoring/RiskCalculationHealthRoutes.java`
- **Route:** `GET /api/v1/risk-calculation/metrics`
- **Current Security:** Uses `RouterAttributes.withAttributes` with permissions `["risk-calculation:metrics:view"]` ✅
- **Should Be:** Requires authentication + specific permission
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** None

#### 3.3 Batch Summary Status Endpoints
**File:** `regtech-risk-calculation/presentation/src/main/java/com/bcbs239/regtech/riskcalculation/presentation/status/BatchSummaryStatusRoutes.java`
- **Routes:**
  - `GET /api/v1/risk-calculation/batches/{batchId}` - Permission: `["risk-calculation:batches:view"]`
  - `GET /api/v1/risk-calculation/banks/{bankId}/batches` - Permission: `["risk-calculation:batches:view"]`
  - `GET /api/v1/risk-calculation/batches/{batchId}/exists` - Permission: `["risk-calculation:batches:view"]`
- **Current Security:** Uses `RouterAttributes.withAttributes` with specific permissions ✅
- **Should Be:** Requires authentication + specific permissions
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** None

### 4. Report Generation Module

#### 4.1 Health Endpoints
**File:** `regtech-report-generation/presentation/src/main/java/com/bcbs239/regtech/reportgeneration/presentation/health/ReportGenerationHealthRoutes.java`
- **Routes:**
  - `GET /api/v1/report-generation/health`
  - `GET /api/v1/report-generation/health/database`
  - `GET /api/v1/report-generation/health/s3`
  - `GET /api/v1/report-generation/health/event-tracker`
  - `GET /api/v1/report-generation/health/async-executor`
- **Current Security:** Uses `RouterAttributes.withAttributes` with `null` permissions ✅
- **Should Be:** Public (no authentication required)
- **Status:** ✅ COMPLIANT - Already using RouterAttributes pattern
- **Action Required:** Verify all health paths are in public paths configuration

## Summary

### Compliance Status
- **Total Routes Audited:** 28
- **Compliant Routes:** 25 (89%)
- **Non-Compliant Routes:** 3 (11%)

### Non-Compliant Routes (Require Updates)
1. **Ingestion Module:**
   - `BatchStatusController` - 2 routes using old `.withAttribute()` pattern
   - `RetentionPoliciesController` - 2 routes using old `.withAttribute()` pattern (disabled)
   - `ComplianceReportsController` - 2 routes using old `.withAttribute()` pattern (disabled)

### Public Paths to Add to Configuration
The following health endpoint paths should be added to `iam.security.public-paths` in `application.yml`:

```yaml
iam:
  security:
    public-paths:
      # ... existing paths ...
      
      # Ingestion module health
      - /api/v1/ingestion/health
      
      # Data Quality module health
      - /api/v1/data-quality/health
      - /api/v1/data-quality/health/**
      
      # Risk Calculation module health
      - /api/v1/risk-calculation/health
      - /api/v1/risk-calculation/health/**
      
      # Report Generation module health
      - /api/v1/report-generation/health
      - /api/v1/report-generation/health/**
```

### Permissions Catalog
All routes requiring permissions are properly documented with their permission strings:

**Ingestion:**
- `ingestion:upload-process` - Upload and process files
- `ingestion:status:view` - View batch status
- `ingestion:compliance:manage` - Manage retention policies

**Data Quality:**
- `data-quality:metrics:view` - View performance metrics
- `data-quality:reports:view` - View quality reports
- `data-quality:trends:view` - View quality trends

**Risk Calculation:**
- `risk-calculation:metrics:view` - View performance metrics
- `risk-calculation:batches:view` - View batch summaries

## Recommendations

1. **Update Non-Compliant Routes:** Convert the 3 non-compliant routes in the ingestion module to use `RouterAttributes.withAttributes()` pattern
2. **Verify Public Paths:** Ensure all health endpoints are added to the public paths configuration
3. **Documentation:** Document all permission strings in a central permissions reference
4. **Testing:** Add integration tests to verify security configuration is correctly applied to all routes
