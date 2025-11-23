# Configuration Migration Guide

## Overview

This guide documents the migration from the old configuration structure (all configuration in root `application.yml`) to the new modular structure (module-specific configuration files). This migration improves maintainability, reduces duplication, and provides clearer boundaries between shared infrastructure and module-specific settings.

## Migration Summary

### What Changed

**Before:**
- All configuration in `regtech-app/src/main/resources/application.yml`
- Module-specific settings mixed with shared infrastructure
- Significant duplication across modules
- Unclear ownership of configuration properties

**After:**
- Shared infrastructure in `regtech-app/src/main/resources/application.yml`
- Module-specific configuration in `{module}/infrastructure/src/main/resources/application-{module}.yml`
- Clear separation of concerns
- Reduced duplication through consistent patterns

### Benefits

1. **Improved Maintainability**: Module developers can modify their configuration without affecting other modules
2. **Reduced Duplication**: Common patterns (S3, async, storage) are standardized
3. **Clear Ownership**: Each module owns its configuration file
4. **Better Organization**: Related configuration is co-located with the module
5. **Easier Testing**: Module configuration can be tested independently

## Configuration File Mapping

### Root Configuration (application.yml)

**Remains in root `application.yml`:**
- Spring application settings
- Database configuration (datasource, JPA, Flyway)
- Logging configuration
- Metrics and monitoring (Spring Actuator)
- Security configuration (JWT, OAuth2, public paths)
- Event processing (inbox/outbox)
- Cross-cutting concerns

**Location:** `regtech-app/src/main/resources/application.yml`

### Module-Specific Configuration

| Module | Old Location | New Location |
|--------|-------------|--------------|
| Ingestion | Root `application.yml` | `regtech-ingestion/infrastructure/src/main/resources/application-ingestion.yml` |
| Data Quality | Root `application.yml` | `regtech-data-quality/infrastructure/src/main/resources/application-data-quality.yml` |
| Risk Calculation | Root `application.yml` | `regtech-risk-calculation/infrastructure/src/main/resources/application-risk-calculation.yml` |
| Report Generation | Root `application.yml` | `regtech-report-generation/infrastructure/src/main/resources/application-report-generation.yml` |
| Billing | Root `application.yml` | `regtech-billing/infrastructure/src/main/resources/application-billing.yml` |
| IAM | Root `application.yml` | `regtech-iam/infrastructure/src/main/resources/application-iam.yml` |


---

## Property Path Mapping

### Ingestion Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.ingestion.*` | `ingestion.*` | Prefix simplified |
| `ingestion.file.*` | `ingestion.file.*` | No change |
| `ingestion.storage.*` | `ingestion.storage.*` | No change |
| `ingestion.async.*` | `ingestion.async.*` | No change |
| `ingestion.processing.*` | `ingestion.processing.*` | No change |
| `ingestion.performance.*` | `ingestion.performance.*` | No change |
| `ingestion.parser.*` | `ingestion.parser.*` | No change |

### Data Quality Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.data-quality.*` | `data-quality.*` | Prefix simplified |
| `data-quality.storage.*` | `data-quality.storage.*` | No change |
| `data-quality.async.*` | `data-quality.async.*` | No change |
| `data-quality.rules-engine.*` | `data-quality.rules-engine.*` | No change |
| `data-quality.rules-migration.*` | `data-quality.rules-migration.*` | No change |

### Risk Calculation Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.risk-calculation.*` | `risk-calculation.*` | Prefix simplified |
| `risk-calculation.storage.*` | `risk-calculation.storage.*` | No change |
| `risk-calculation.async.*` | `risk-calculation.async.*` | No change |
| `risk-calculation.processing.*` | `risk-calculation.processing.*` | No change |
| `risk-calculation.currency.*` | `risk-calculation.currency.*` | No change |
| `risk-calculation.performance.*` | `risk-calculation.performance.*` | No change |
| `risk-calculation.geographic.*` | `risk-calculation.geographic.*` | No change |
| `risk-calculation.concentration.*` | `risk-calculation.concentration.*` | No change |

### Report Generation Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.report-generation.*` | `report-generation.*` | Prefix simplified |
| `report-generation.s3.*` | `report-generation.s3.*` | No change |
| `report-generation.file-paths.*` | `report-generation.file-paths.*` | No change |
| `report-generation.fallback.*` | `report-generation.fallback.*` | No change |
| `report-generation.async.*` | `report-generation.async.*` | No change |
| `report-generation.coordination.*` | `report-generation.coordination.*` | No change |
| `report-generation.performance.*` | `report-generation.performance.*` | No change |
| `report-generation.retry.*` | `report-generation.retry.*` | No change |

### Billing Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.billing.*` | `billing.*` | Prefix simplified |
| `billing.stripe.*` | `billing.stripe.*` | No change |
| `billing.tiers.*` | `billing.tiers.*` | No change |
| `billing.dunning.*` | `billing.dunning.*` | No change |
| `billing.invoices.*` | `billing.invoices.*` | No change |
| `billing.billing-cycle.*` | `billing.billing-cycle.*` | No change |
| `billing.outbox.*` | `billing.outbox.*` | No change |
| `billing.scheduling.*` | `billing.scheduling.*` | No change |
| `billing.notifications.*` | `billing.notifications.*` | No change |

### IAM Module

| Old Path | New Path | Notes |
|----------|----------|-------|
| `app.iam.*` | `iam.*` | Prefix simplified |
| `iam.security.*` | `iam.security.*` | Remains in root (shared) |
| `iam.authorization.*` | `iam.authorization.*` | Remains in root (shared) |
| `iam.user-management.*` | `iam.user-management.*` | Moved to module file |
| `iam.session.*` | `iam.session.*` | Moved to module file |

**Note:** Security configuration (`iam.security.*`) remains in root `application.yml` as it is a shared concern across all modules.


---

## Migration Steps

### Phase 1: Preparation (Completed)

✅ **Step 1.1: Audit Current Configuration**
- Reviewed all existing configuration files
- Documented module usage of S3, thread pools, and async processing
- Identified duplicate configuration
- Created configuration inventory

✅ **Step 1.2: Create Module Configuration Files**
- Created `application-{module}.yml` for each module
- Placed files in `{module}/infrastructure/src/main/resources/`
- Copied module-specific configuration from root

### Phase 2: Configuration Properties Update (Completed)

✅ **Step 2.1: Update Configuration Properties Classes**
- Updated `@ConfigurationProperties` prefix annotations
- Added validation annotations (`@NotNull`, `@Min`, `@Max`)
- Created nested classes for logical grouping
- Added JavaDoc documentation

✅ **Step 2.2: Create Async Configuration Classes**
- Created `{Module}AsyncConfiguration` classes
- Configured module-specific thread pools
- Added `@EnableAsync` annotations
- Bound to module configuration properties

### Phase 3: Security Configuration Update (Completed)

✅ **Step 3.1: Consolidate Security Configuration**
- Moved all security configuration to `iam.security` section
- Created centralized public paths list
- Added comments documenting path purposes
- Included health endpoints from all modules

✅ **Step 3.2: Update SecurityFilter**
- Modified to inject `IAMProperties`
- Replaced hardcoded paths with configuration
- Added validation and logging

✅ **Step 3.3: Update Module Routes**
- Ensured all routes use `RouterAttributes.withAttributes`
- Added health endpoints to public paths
- Documented permission requirements

### Phase 4: Testing and Validation (Completed)

✅ **Step 4.1: Create Configuration Tests**
- Created `ConfigurationPropertiesTest`
- Created `ProfileOverrideTest`
- Created `EnvironmentVariableTest`
- Created `NumericRangeValidationTest`

✅ **Step 4.2: Create Integration Tests**
- Created `ApplicationContextLoadingTest`
- Created `S3ConfigurationIntegrationTest`
- Created `SecurityConfigurationIntegrationTest`
- Created `ThreadPoolConfigurationIntegrationTest`

✅ **Step 4.3: Run Test Suite**
- All unit tests passing
- All integration tests passing
- Configuration validation tests passing

### Phase 5: Cleanup (Completed)

✅ **Step 5.1: Remove Duplicates**
- Removed module-specific configuration from root `application.yml`
- Kept only shared infrastructure configuration
- Verified backward compatibility

✅ **Step 5.2: Verify Application Startup**
- Tested startup with development profile
- Tested startup with production profile
- Verified all modules load configuration correctly

### Phase 6: Documentation (In Progress)

✅ **Step 6.1: Create Configuration Reference**
- Document all properties by module
- Document valid values and defaults
- Provide examples for common scenarios

🔄 **Step 6.2: Create Migration Guide** (This Document)
- Document migration from old to new structure
- Provide property path mappings
- Document troubleshooting steps

⏳ **Step 6.3: Update Deployment Documentation**
- Document environment-specific configuration
- Document required environment variables
- Document profile selection guidance


---

## Backward Compatibility

### Approach

The migration was designed to maintain backward compatibility during the transition:

1. **Dual Configuration Support**: Both old and new paths were supported temporarily
2. **Gradual Migration**: Modules were migrated one at a time
3. **Extensive Testing**: Each migration step was validated with tests
4. **Rollback Plan**: Original configuration was preserved until verification

### Breaking Changes

The following changes may require updates to external configuration:

1. **Configuration Properties Prefix Changes**
   - Old: `app.{module}.*`
   - New: `{module}.*`
   - **Impact**: Environment variables or external configuration using old prefix must be updated

2. **Security Configuration Consolidation**
   - Old: Security settings scattered across modules
   - New: Centralized in `iam.security.*`
   - **Impact**: Custom security configuration must be moved to new location

3. **Public Paths Configuration**
   - Old: Hardcoded in `SecurityFilter`
   - New: Configured in `iam.security.public-paths`
   - **Impact**: Custom public paths must be added to configuration

### Migration for Existing Deployments

If you have an existing deployment with custom configuration:

**Option 1: Update Configuration Files**
1. Update your configuration files to use new property paths
2. Move module-specific configuration to module files
3. Update environment variables to use new prefixes

**Option 2: Use Environment Variable Overrides**
1. Keep existing configuration files temporarily
2. Override specific properties using environment variables
3. Gradually migrate to new structure

**Example Environment Variable Migration:**
```bash
# Old
export APP_INGESTION_FILE_MAX_SIZE=524288000

# New
export INGESTION_FILE_MAX_SIZE=524288000
```


---

## Troubleshooting Common Migration Issues

### Issue 1: Configuration Not Loading After Migration

**Symptoms:**
- Module configuration properties are null
- Application uses default values instead of configured values
- `@ConfigurationProperties` beans not created

**Diagnosis:**
```bash
# Check if configuration file exists
ls -la {module}/infrastructure/src/main/resources/application-{module}.yml

# Check application logs for configuration loading
grep "application-{module}.yml" logs/application.log

# Verify active profile
grep "The following profiles are active" logs/application.log
```

**Solutions:**

1. **Verify File Location**
   ```
   Correct: regtech-ingestion/infrastructure/src/main/resources/application-ingestion.yml
   Wrong: regtech-ingestion/src/main/resources/application-ingestion.yml
   ```

2. **Verify File Naming**
   ```
   Correct: application-ingestion.yml
   Wrong: application_ingestion.yml
   Wrong: ingestion.yml
   ```

3. **Verify Configuration Properties Class**
   ```java
   @Configuration
   @ConfigurationProperties(prefix = "ingestion")
   @Validated
   public class IngestionProperties {
       // Properties
   }
   ```

4. **Enable Configuration Properties**
   ```java
   @SpringBootApplication
   @EnableConfigurationProperties({
       IngestionProperties.class,
       DataQualityProperties.class,
       // ... other properties classes
   })
   public class RegtechApplication {
       // ...
   }
   ```

### Issue 2: Profile-Specific Configuration Not Applied

**Symptoms:**
- Development profile uses production settings
- Production profile uses development settings
- Profile overrides not taking effect

**Diagnosis:**
```bash
# Check active profile
echo $SPRING_PROFILES_ACTIVE

# Check application logs
grep "The following profiles are active" logs/application.log

# Verify profile section in configuration
cat {module}/infrastructure/src/main/resources/application-{module}.yml | grep -A 5 "on-profile"
```

**Solutions:**

1. **Set Active Profile**
   ```bash
   # Environment variable
   export SPRING_PROFILES_ACTIVE=development
   
   # Command line
   java -jar app.jar --spring.profiles.active=development
   
   # JVM property
   java -Dspring.profiles.active=development -jar app.jar
   ```

2. **Verify Profile Section Syntax**
   ```yaml
   ---
   spring:
     config:
       activate:
         on-profile: development
   
   ingestion:
     storage:
       type: local
   ```

3. **Check Property Indentation**
   - YAML is indentation-sensitive
   - Use 2 spaces for indentation
   - Don't mix spaces and tabs

### Issue 3: Environment Variables Not Resolving

**Symptoms:**
- Properties show `${VAR_NAME}` in logs
- Application fails to start with "Could not resolve placeholder"
- Sensitive values exposed in logs

**Diagnosis:**
```bash
# Check if environment variable is set
echo $AWS_ACCESS_KEY_ID
echo $DB_PASSWORD

# Check application logs for placeholder errors
grep "Could not resolve placeholder" logs/application.log
```

**Solutions:**

1. **Set Missing Environment Variables**
   ```bash
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   export DB_PASSWORD=your-password
   ```

2. **Add Default Values for Development**
   ```yaml
   # Development-friendly with defaults
   datasource:
     username: ${DB_USERNAME:postgres}
     password: ${DB_PASSWORD:postgres}
   
   # Production-only (no defaults)
   datasource:
     username: ${DB_USERNAME}
     password: ${DB_PASSWORD}
   ```

3. **Use .env File for Local Development**
   ```bash
   # Create .env file (don't commit!)
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   AWS_ACCESS_KEY_ID=test
   AWS_SECRET_ACCESS_KEY=test
   
   # Load before starting application
   export $(cat .env | xargs)
   ```

### Issue 4: Thread Pool Not Working

**Symptoms:**
- Async methods execute synchronously
- Thread pool not created
- Wrong thread pool used

**Diagnosis:**
```bash
# Check thread pool configuration
grep "async" {module}/infrastructure/src/main/resources/application-{module}.yml

# Check application logs for thread pool creation
grep "TaskExecutor" logs/application.log

# Check thread names in logs
grep "ingestion-async-" logs/application.log
```

**Solutions:**

1. **Verify @EnableAsync Annotation**
   ```java
   @Configuration
   @EnableAsync
   public class IngestionAsyncConfiguration {
       // ...
   }
   ```

2. **Verify Bean Name Matches @Async**
   ```java
   // Configuration
   @Bean(name = "ingestionTaskExecutor")
   public Executor taskExecutor() {
       // ...
   }
   
   // Usage
   @Async("ingestionTaskExecutor")
   public CompletableFuture<Void> processAsync() {
       // ...
   }
   ```

3. **Check Async Enabled Flag**
   ```yaml
   ingestion:
     async:
       enabled: true  # Must be true
   ```

### Issue 5: S3 Connection Failures After Migration

**Symptoms:**
- S3 operations fail with authentication errors
- "Access Denied" errors
- Connection timeout errors

**Diagnosis:**
```bash
# Check S3 configuration
grep -A 10 "storage:" {module}/infrastructure/src/main/resources/application-{module}.yml

# Check AWS credentials
echo $AWS_ACCESS_KEY_ID
echo $AWS_SECRET_ACCESS_KEY

# Test S3 connectivity
aws s3 ls s3://regtech-ingestion/ --region us-east-1
```

**Solutions:**

1. **Verify Storage Type**
   ```yaml
   ingestion:
     storage:
       type: s3  # Not 'local'
   ```

2. **Verify AWS Credentials**
   ```bash
   export AWS_ACCESS_KEY_ID=your-key
   export AWS_SECRET_ACCESS_KEY=your-secret
   ```

3. **Verify S3 Endpoint (for LocalStack)**
   ```yaml
   ingestion:
     storage:
       s3:
         endpoint: http://localhost:4566
   ```

4. **Verify Bucket Exists**
   ```bash
   aws s3 mb s3://regtech-ingestion --region us-east-1
   ```

### Issue 6: Security Configuration Not Working

**Symptoms:**
- Public endpoints require authentication
- Protected endpoints accessible without authentication
- 401 Unauthorized on health checks

**Diagnosis:**
```bash
# Check public paths configuration
grep -A 20 "public-paths:" regtech-app/src/main/resources/application.yml

# Test public endpoint
curl -v http://localhost:8080/api/health

# Check SecurityFilter logs
grep "SecurityFilter" logs/application.log
```

**Solutions:**

1. **Verify Public Paths Configuration**
   ```yaml
   iam:
     security:
       public-paths:
         - /api/health
         - /api/v1/ingestion/health
         - /actuator/health
   ```

2. **Verify SecurityFilter Injection**
   ```java
   @Component
   public class SecurityFilter implements WebFilter {
       private final IAMProperties iamProperties;
       
       public SecurityFilter(IAMProperties iamProperties) {
           this.iamProperties = iamProperties;
       }
   }
   ```

3. **Check Route Attributes**
   ```java
   RouterAttributes.withAttributes(
       route(GET("/api/v1/ingestion/batches"), handler::getBatches),
       new String[]{"ingestion:batch:read"},  // Required permissions
       new String[]{Tags.INGESTION},
       "Get all batches"
   )
   ```


---

## Rollback Procedure

If you need to rollback the configuration migration:

### Step 1: Restore Original Configuration

1. **Restore Root application.yml**
   ```bash
   git checkout HEAD~1 regtech-app/src/main/resources/application.yml
   ```

2. **Remove Module Configuration Files**
   ```bash
   rm regtech-ingestion/infrastructure/src/main/resources/application-ingestion.yml
   rm regtech-data-quality/infrastructure/src/main/resources/application-data-quality.yml
   rm regtech-risk-calculation/infrastructure/src/main/resources/application-risk-calculation.yml
   rm regtech-report-generation/infrastructure/src/main/resources/application-report-generation.yml
   rm regtech-billing/infrastructure/src/main/resources/application-billing.yml
   rm regtech-iam/infrastructure/src/main/resources/application-iam.yml
   ```

### Step 2: Restore Configuration Properties Classes

1. **Restore Old Prefixes**
   ```java
   // Revert to old prefix
   @ConfigurationProperties(prefix = "app.ingestion")
   public class IngestionProperties {
       // ...
   }
   ```

2. **Restore SecurityFilter**
   ```bash
   git checkout HEAD~1 regtech-iam/infrastructure/src/main/java/com/bcbs239/regtech/iam/infrastructure/security/SecurityFilter.java
   ```

### Step 3: Rebuild and Test

```bash
# Clean build
mvn clean install

# Run tests
mvn test

# Start application
mvn spring-boot:run
```

### Step 4: Verify Rollback

1. Check application starts successfully
2. Verify all modules load correctly
3. Test key functionality
4. Check logs for errors

---

## Validation Checklist

Use this checklist to verify successful migration:

### Configuration Files

- [ ] All module configuration files created in correct locations
- [ ] Root `application.yml` contains only shared infrastructure
- [ ] No duplicate configuration between root and module files
- [ ] Profile sections present in all configuration files
- [ ] Environment variables properly referenced with `${VAR_NAME}`

### Configuration Properties Classes

- [ ] All `@ConfigurationProperties` classes updated with new prefixes
- [ ] Validation annotations added (`@NotNull`, `@Min`, `@Max`)
- [ ] Nested classes created for logical grouping
- [ ] JavaDoc documentation added
- [ ] Classes registered with `@EnableConfigurationProperties`

### Async Configuration

- [ ] Async configuration classes created for each module
- [ ] `@EnableAsync` annotation present
- [ ] Thread pool beans created with correct names
- [ ] Configuration bound to module properties
- [ ] Profile-specific thread pool sizes configured

### Security Configuration

- [ ] All security configuration in `iam.security` section
- [ ] Public paths list complete and documented
- [ ] SecurityFilter updated to use configuration
- [ ] All module routes use `RouterAttributes`
- [ ] Health endpoints added to public paths

### Testing

- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Configuration validation tests passing
- [ ] Application starts with development profile
- [ ] Application starts with production profile
- [ ] All modules load configuration correctly

### Documentation

- [ ] Configuration reference document created
- [ ] Migration guide created (this document)
- [ ] Deployment documentation updated
- [ ] README files updated
- [ ] Inline comments added to configuration files

---

## Post-Migration Tasks

After completing the migration:

1. **Update CI/CD Pipelines**
   - Update environment variable names
   - Update configuration file paths
   - Update deployment scripts

2. **Update Monitoring**
   - Update configuration monitoring dashboards
   - Update alerts for configuration errors
   - Update documentation links

3. **Train Team**
   - Share migration guide with team
   - Conduct training session on new structure
   - Update onboarding documentation

4. **Archive Old Configuration**
   - Tag repository before migration
   - Document rollback procedure
   - Keep backup of old configuration

---

## Additional Resources

- [Configuration Reference](./CONFIGURATION_REFERENCE.md) - Complete property reference
- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) - Official documentation
- [Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html) - Property metadata
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles) - Profile documentation

---

## Support

If you encounter issues during migration:

1. **Check Troubleshooting Section**: Review common issues above
2. **Check Application Logs**: Look for configuration errors
3. **Run Tests**: Verify configuration with test suite
4. **Consult Team**: Reach out to development team
5. **Review Documentation**: Check configuration reference

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Migration Status**: Completed  
**Maintained By**: RegTech Development Team
