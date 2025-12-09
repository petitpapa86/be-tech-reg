# Configuration Inventory Document

## Overview

This document provides a comprehensive audit of the current configuration structure across all RegTech modules. It identifies configuration files, their locations, duplicate settings, and areas requiring reorganization.

**Audit Date:** November 22, 2025  
**Requirements:** 1.1, 1.2, 8.5

---

## 1. Configuration Files Inventory

### 1.1 Root Configuration

**Location:** `regtech-app/src/main/resources/application.yml`

**Contents:**
- Spring application settings (name, profiles, datasource, JPA, Flyway)
- IAM module configuration (security, JWT, OAuth2, authorization)
- Storage configuration (type selector: local/s3)
- Ingestion module configuration (S3, file, processing, performance, parser)
- RegTech shared configuration (bank-registry, outbox, inbox, event retry)
- Billing module configuration (Stripe, tiers, dunning, invoices, scheduling, notifications)
- Risk calculation module configuration (storage, processing, currency, geographic, concentration)
- Data quality rules engine configuration
- Management/Actuator endpoints
- Logging configuration
- Profile-specific overrides (development, production, gcp)

**Size:** ~500 lines  
**Profile Sections:** 3 (base, gcp, development/production for risk-calculation)

### 1.2 Module-Specific Configuration Files

#### Ingestion Module
**Location:** `regtech-ingestion/infrastructure/src/main/resources/application-ingestion.yml`

**Contents:**
- File upload settings (max-size, supported-types)
- Async processing configuration (thread-pool-size, queue-capacity)
- Performance settings (max-concurrent-files, chunk-size)
- Parser settings (default-max-records)

**Size:** ~20 lines  
**Profile Sections:** None  
**Status:** ✅ Exists, but missing S3 configuration (present in root)

#### Risk Calculation Module
**Location:** `regtech-risk-calculation/infrastructure/src/main/resources/application-risk-calculation.yml`

**Contents:**
- Storage configuration (S3 and local)
- Processing configuration (async, thread pools, timeouts)
- Currency conversion settings
- Performance settings
- Geographic classification (home country, EU countries)
- Concentration thresholds
- Profile-specific overrides (development, production)

**Size:** ~120 lines  
**Profile Sections:** 3 (base, development, production)  
**Status:** ✅ Exists and well-structured

#### Report Generation Module
**Location:** `regtech-report-generation/infrastructure/src/main/resources/application-report-generation.yml`

**Contents:**
- S3 storage configuration (bucket, prefixes, encryption)
- File path patterns for data retrieval
- Local fallback configuration
- Async processing configuration
- Event coordination settings
- Performance targets
- Retry configuration
- AWS configuration
- Resilience4j circuit breaker and retry policies
- Thymeleaf configuration
- Profile-specific overrides (development, production)

**Size:** ~180 lines  
**Profile Sections:** 3 (base, development, production)  
**Status:** ✅ Exists and comprehensive

#### Data Quality Module
**Location:** N/A

**Status:** ❌ Does not exist - configuration is in root application.yml

#### Billing Module
**Location:** N/A

**Status:** ❌ Does not exist - configuration is in root application.yml

#### IAM Module
**Location:** N/A

**Status:** ❌ Does not exist - configuration is in root application.yml

---

## 2. Configuration Properties Classes

### 2.1 Existing Properties Classes

| Module | Class | Prefix | Location | Status |
|--------|-------|--------|----------|--------|
| Ingestion | `IngestionProperties` | `ingestion` | `regtech-ingestion/infrastructure/.../configuration/` | ✅ Exists |
| Risk Calculation | `RiskCalculationProperties` | `risk-calculation` | `regtech-risk-calculation/infrastructure/.../config/` | ✅ Exists |
| Billing | `BillingConfigurationProperties` | `billing` | `regtech-billing/infrastructure/.../configuration/` | ✅ Exists |
| IAM | `IamConfigurationProperties` | `iam` | `regtech-iam/infrastructure/.../configuration/` | ✅ Exists |
| Core S3 | `S3Properties` | `ingestion.s3` | `regtech-core/infrastructure/.../s3/` | ⚠️ Hardcoded to ingestion prefix |

### 2.2 Properties Class Structure

**Ingestion Module:**
```java
@ConfigurationProperties(prefix = "ingestion")
- FileProperties (maxSize, supportedTypes)
- ProcessingProperties (asyncEnabled, threadPoolSize, queueCapacity)
- PerformanceProperties (maxConcurrentFiles, chunkSize)
- ParserProperties (defaultMaxRecords)
```

**Risk Calculation Module:**
```java
@ConfigurationProperties(prefix = "risk-calculation")
- StorageProperties (type, s3, local)
- ProcessingProperties (asyncEnabled, threadPoolSize, queueCapacity, timeoutSeconds)
- CurrencyProperties (baseCurrency, cacheEnabled, cacheTtl, provider)
- PerformanceProperties (maxConcurrentCalculations, streamingParserEnabled, memoryThresholdMb)
- GeographicProperties (homeCountry, euCountries)
- ConcentrationProperties (highThreshold, moderateThreshold, precision)
```

**Billing Module:**
```java
@ConfigurationProperties(prefix = "billing")
- StripeProperties (apiKey, webhookSecret)
- TiersProperties (starter)
- DunningProperties (reminderIntervals, finalActionDelay)
- InvoicesProperties (dueDays, currency)
- BillingCycleProperties (timezone, billingDay)
- OutboxProperties (enabled, processingInterval, retryInterval, maxRetries, cleanupInterval, cleanupRetentionDays)
- SchedulingProperties (monthlyBilling, dunningProcess)
- NotificationsProperties (email, sms, push)
```

**IAM Module:**
```java
@ConfigurationProperties(prefix = "iam")
- SecurityProperties (jwt, password, oauth2)
- AuthorizationProperties (cache, multiTenant, permissions)
```

---

## 3. S3 Storage Configuration Analysis

### 3.1 Modules Using S3

| Module | Bucket | Prefix | Configuration Location | Status |
|--------|--------|--------|------------------------|--------|
| Ingestion | `regtech-data-storage` | `raw/` | Root application.yml | ⚠️ Should be in module file |
| Data Quality | Not configured | Not configured | N/A | ❌ Missing |
| Risk Calculation | `regtech-risk-calculations` | `calculations/` | application-risk-calculation.yml | ✅ Correct |
| Report Generation | `risk-analysis` | `reports/html/`, `reports/xbrl/` | application-report-generation.yml | ✅ Correct |

### 3.2 S3 Configuration Structure

**Current Structure (Inconsistent):**

1. **Ingestion (in root application.yml):**
```yaml
ingestion:
  s3:
    bucket: regtech-data-storage
    region: us-east-1
    prefix: raw/
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}
    endpoint: ${AWS_S3_ENDPOINT:}
```

2. **Risk Calculation (in module file):**
```yaml
risk-calculation:
  storage:
    s3:
      bucket: regtech-risk-calculations
      region: us-east-1
      prefix: calculations/
      access-key: ${AWS_ACCESS_KEY_ID:}
      secret-key: ${AWS_SECRET_ACCESS_KEY:}
      endpoint: ${AWS_S3_ENDPOINT:}
      encryption: AES256
```

3. **Report Generation (in module file):**
```yaml
report-generation:
  s3:
    bucket: risk-analysis
    html-prefix: reports/html/
    xbrl-prefix: reports/xbrl/
    encryption: AES256
    presigned-url-expiration-hours: 1

aws:
  region: eu-west-1
  s3:
    endpoint: ${AWS_S3_ENDPOINT:}
  access-key-id: ${AWS_ACCESS_KEY_ID:}
  secret-access-key: ${AWS_SECRET_ACCESS_KEY:}
```

**Issues:**
- Inconsistent structure across modules
- Report generation uses separate `aws` section
- Core S3Properties class hardcoded to `ingestion.s3` prefix
- Data quality module has no S3 configuration

---

## 4. Thread Pool and Async Configuration

### 4.1 Modules Using Async Processing

| Module | Configuration Location | Thread Pool Size | Queue Capacity | Status |
|--------|------------------------|------------------|----------------|--------|
| Ingestion | Root application.yml | 10 | 100 | ⚠️ Should be in module file |
| Data Quality | N/A | N/A | N/A | ❌ Not configured |
| Risk Calculation | application-risk-calculation.yml | 5 (dev: 2, prod: 10) | 50 | ✅ Correct |
| Report Generation | application-report-generation.yml | 2-5 (dev: 2-3, prod: 5-10) | 100-200 | ✅ Correct |
| Billing (dunning) | Root application.yml | 5 | N/A | ⚠️ Should be in module file |

### 4.2 Async Configuration Structure

**Current Structure:**

1. **Ingestion (in root application.yml):**
```yaml
ingestion:
  processing:
    async-enabled: true
    thread-pool-size: 10
    queue-capacity: 100
```

2. **Risk Calculation (in module file):**
```yaml
risk-calculation:
  processing:
    async-enabled: true
    thread-pool-size: 5
    queue-capacity: 50
    timeout-seconds: 300
```

3. **Report Generation (in module file):**
```yaml
report-generation:
  async:
    core-pool-size: 2
    max-pool-size: 5
    queue-capacity: 100
    thread-name-prefix: report-gen-
    await-termination-seconds: 60
```

4. **Billing (in root application.yml):**
```yaml
billing:
  scheduling:
    dunningProcess:
      threadPoolSize: 5
```

**Issues:**
- Inconsistent property names (thread-pool-size vs core-pool-size)
- Report generation has more detailed configuration (thread-name-prefix, await-termination)
- No @EnableAsync configuration classes found
- No TaskExecutor beans found

---

## 5. Security Configuration

### 5.1 Public Paths Configuration

**Current Location:** Hardcoded in `SecurityFilter.java`

**Hardcoded Public Paths:**
```java
private final Set<String> publicPaths = Set.of(
    "/api/public/**",
    "/api/health",
    "/api/auth/login",
    "/api/auth/register",
    "/api/auth/forgot-password",
    "/api/v1/users/register",
    "/api/v1/ingestion/**",
    "/actuator/health",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/h2-console/**",
    "/api/v1/ingestion/upload-and-process",
    "/api/v1/data-quality/reports/**"
);
```

**Issues:**
- Public paths are hardcoded in Java code
- No configuration-driven approach
- Missing health endpoints from other modules (risk-calculation, report-generation)
- Difficult to modify without code changes

### 5.2 Security Configuration in application.yml

**Current Structure:**
```yaml
iam:
  security:
    jwt:
      secret: mySecretKey123456789012345678901234567890123456789012345678901234567890
      expiration: 86400
    password:
      min-length: 12
      require-uppercase: true
      require-lowercase: true
      require-digits: true
      require-special-chars: true
    oauth2:
      google:
        client-id: ${GOOGLE_CLIENT_ID:}
        client-secret: ${GOOGLE_CLIENT_SECRET:}
      facebook:
        client-id: ${FACEBOOK_CLIENT_ID:}
        client-secret: ${FACEBOOK_CLIENT_SECRET:}
  authorization:
    cache:
      enabled: true
      ttl: 300
    multi-tenant:
      enabled: true
      default-organization: "default-org"
    permissions:
      strict-mode: true
      audit-enabled: true
```

**Status:** ✅ Well-structured, but missing public-paths configuration

---

## 6. Duplicate Configuration Analysis

### 6.1 Configuration Present in Both Root and Module Files

| Configuration | Root application.yml | Module File | Recommendation |
|---------------|---------------------|-------------|----------------|
| Ingestion file settings | ✅ | ✅ | Keep in module file only |
| Ingestion processing | ✅ | ✅ | Keep in module file only |
| Ingestion performance | ✅ | ✅ | Keep in module file only |
| Ingestion parser | ✅ | ✅ | Keep in module file only |
| Ingestion S3 | ✅ | ❌ | Move to module file |
| Risk calculation (all) | ✅ | ✅ | Remove from root |

### 6.2 Configuration Only in Root (Should be in Module Files)

| Configuration | Module | Current Location | Target Location |
|---------------|--------|------------------|-----------------|
| Ingestion S3 | Ingestion | Root application.yml | application-ingestion.yml |
| Data quality rules engine | Data Quality | Root application.yml | application-data-quality.yml |
| Billing (all) | Billing | Root application.yml | application-billing.yml |
| IAM security | IAM | Root application.yml | Keep in root (shared concern) |

---

## 7. Profile-Specific Configuration

### 7.1 Profiles in Use

| Profile | Purpose | Configured In |
|---------|---------|---------------|
| `development` | Local development | Root application.yml, application-risk-calculation.yml, application-report-generation.yml |
| `production` | Production deployment | application-risk-calculation.yml, application-report-generation.yml |
| `gcp` | Google Cloud Platform | Root application.yml (logging only) |

### 7.2 Profile Override Patterns

**Risk Calculation Module:**
- Development: local storage, reduced thread pools, mock currency provider
- Production: S3 storage, increased thread pools, real currency provider

**Report Generation Module:**
- Development: dev S3 bucket, smaller thread pools, LocalStack endpoint, lenient circuit breaker
- Production: production S3 bucket, larger thread pools, stricter circuit breaker

**Missing Profile Overrides:**
- Ingestion module has no profile-specific overrides
- Data quality module has no profile-specific overrides
- Billing module has no profile-specific overrides

---

## 8. Environment Variables

### 8.1 Environment Variables in Use

| Variable | Used By | Purpose | Default |
|----------|---------|---------|---------|
| `AWS_ACCESS_KEY_ID` | Ingestion, Risk Calculation, Report Generation | S3 access key | Empty |
| `AWS_SECRET_ACCESS_KEY` | Ingestion, Risk Calculation, Report Generation | S3 secret key | Empty |
| `AWS_S3_ENDPOINT` | Ingestion, Risk Calculation, Report Generation | S3 endpoint (LocalStack) | Empty |
| `GOOGLE_CLIENT_ID` | IAM | Google OAuth2 client ID | Empty |
| `GOOGLE_CLIENT_SECRET` | IAM | Google OAuth2 client secret | Empty |
| `FACEBOOK_CLIENT_ID` | IAM | Facebook OAuth2 client ID | Empty |
| `FACEBOOK_CLIENT_SECRET` | IAM | Facebook OAuth2 client secret | Empty |
| `STRIPE_API_KEY` | Billing | Stripe API key | Empty |
| `STRIPE_WEBHOOK_SECRET` | Billing | Stripe webhook secret | Empty |
| `BANK_REGISTRY_URL` | RegTech shared | Bank registry service URL | http://localhost:8081 |

### 8.2 Hardcoded Secrets (Security Issues)

**⚠️ CRITICAL:**
```yaml
iam:
  security:
    jwt:
      secret: mySecretKey123456789012345678901234567890123456789012345678901234567890
```

**Recommendation:** Move to environment variable `${JWT_SECRET}`

---

## 9. Shared Configuration

### 9.1 Configuration That Should Remain in Root

| Configuration | Reason |
|---------------|--------|
| Spring datasource | Shared database connection |
| Spring JPA | Shared persistence settings |
| Spring Flyway | Shared migration management |
| Management/Actuator | Shared monitoring endpoints |
| Logging | Shared logging configuration |
| IAM security | Cross-cutting security concern |
| RegTech outbox/inbox | Shared event processing |
| RegTech bank-registry | Shared external service |

### 9.2 Configuration That Should Move to Modules

| Configuration | Current Location | Target Module |
|---------------|------------------|---------------|
| Ingestion S3 | Root | Ingestion |
| Ingestion file/processing/performance/parser | Root | Ingestion (already exists, remove from root) |
| Data quality rules engine | Root | Data Quality |
| Billing (all) | Root | Billing |
| Risk calculation (all) | Root | Risk Calculation (already exists, remove from root) |

---

## 10. Missing Configuration

### 10.1 Modules Without Configuration Files

1. **Data Quality Module**
   - Missing: application-data-quality.yml
   - Needs: S3 storage, async processing, rules engine settings

2. **Billing Module**
   - Missing: application-billing.yml
   - Needs: All billing configuration currently in root

3. **IAM Module**
   - Missing: application-iam.yml
   - Note: Security configuration should remain in root as it's a cross-cutting concern
   - Could have: Module-specific IAM settings if needed

### 10.2 Missing Async Configuration Classes

- No @EnableAsync configuration classes found
- No TaskExecutor beans found
- Async processing likely not properly configured despite configuration properties

### 10.3 Missing Public Paths Configuration

- Public paths are hardcoded in SecurityFilter
- Should be configurable via application.yml under `iam.security.public-paths`

---

## 11. Configuration Organization Issues

### 11.1 Structural Issues

1. **Inconsistent S3 Configuration:**
   - Different property structures across modules
   - Core S3Properties hardcoded to ingestion prefix
   - Report generation uses separate `aws` section

2. **Inconsistent Async Configuration:**
   - Different property names (thread-pool-size vs core-pool-size)
   - Different levels of detail across modules
   - No actual async configuration classes

3. **Duplicate Configuration:**
   - Ingestion configuration in both root and module file
   - Risk calculation configuration in both root and module file

4. **Missing Module Files:**
   - Data quality has no module configuration file
   - Billing has no module configuration file

5. **Hardcoded Values:**
   - Public paths hardcoded in SecurityFilter
   - JWT secret hardcoded in application.yml

### 11.2 Documentation Issues

1. **Inline Comments:**
   - Some configuration has good comments (report-generation)
   - Most configuration lacks explanatory comments
   - No requirement references in comments

2. **Configuration Reference:**
   - No centralized configuration reference document
   - Difficult to understand all available properties

---

## 12. Recommendations Summary

### 12.1 High Priority

1. ✅ Create application-data-quality.yml
2. ✅ Create application-billing.yml
3. ✅ Move ingestion S3 configuration to module file
4. ✅ Remove duplicate configuration from root application.yml
5. ✅ Move public paths to configuration (iam.security.public-paths)
6. ✅ Move JWT secret to environment variable

### 12.2 Medium Priority

1. ✅ Standardize S3 configuration structure across modules
2. ✅ Standardize async configuration property names
3. ✅ Create async configuration classes (@EnableAsync, TaskExecutor beans)
4. ✅ Add profile-specific overrides for ingestion, data-quality, billing
5. ✅ Add inline documentation comments to all configuration

### 12.3 Low Priority

1. ✅ Create configuration reference document
2. ✅ Create configuration migration guide
3. ✅ Add configuration validation tests
4. ✅ Update deployment documentation

---

## 13. Migration Strategy

### Phase 1: Create Module Configuration Files
- Create application-data-quality.yml
- Create application-billing.yml
- Move ingestion S3 configuration to application-ingestion.yml

### Phase 2: Update Configuration Properties Classes
- Standardize S3 configuration structure
- Standardize async configuration property names
- Add validation annotations

### Phase 3: Create Async Configuration Classes
- Create @EnableAsync configuration classes for each module
- Create TaskExecutor beans with proper naming

### Phase 4: Update SecurityFilter
- Add public-paths to IamConfigurationProperties
- Inject configuration into SecurityFilter
- Remove hardcoded paths

### Phase 5: Remove Duplicates
- Remove ingestion configuration from root
- Remove risk calculation configuration from root
- Keep only shared infrastructure in root

### Phase 6: Add Profile Overrides
- Add development/production profiles to ingestion
- Add development/production profiles to data-quality
- Add development/production profiles to billing

### Phase 7: Documentation and Testing
- Add inline comments to all configuration
- Create configuration reference document
- Create configuration validation tests
- Update deployment documentation

---

## Appendix A: Configuration File Sizes

| File | Lines | Sections | Profiles |
|------|-------|----------|----------|
| Root application.yml | ~500 | 15+ | 3 |
| application-ingestion.yml | ~20 | 4 | 0 |
| application-risk-calculation.yml | ~120 | 7 | 3 |
| application-report-generation.yml | ~180 | 10 | 3 |

**Total Configuration:** ~820 lines across 4 files

---

## Appendix B: Module Configuration Matrix

| Module | Has Config File | Has Properties Class | Uses S3 | Uses Async | Profile Overrides |
|--------|----------------|---------------------|---------|------------|-------------------|
| Ingestion | ✅ (partial) | ✅ | ✅ | ✅ | ❌ |
| Data Quality | ❌ | ❌ | ❌ | ❌ | ❌ |
| Risk Calculation | ✅ | ✅ | ✅ | ✅ | ✅ |
| Report Generation | ✅ | ❌ | ✅ | ✅ | ✅ |
| Billing | ❌ | ✅ | ❌ | ✅ | ❌ |
| IAM | ❌ | ✅ | ❌ | ❌ | ❌ |

---

**End of Configuration Inventory Document**
