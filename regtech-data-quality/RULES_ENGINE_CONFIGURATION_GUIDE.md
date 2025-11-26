# Rules Engine Configuration Guide

## Overview

This guide provides comprehensive documentation for configuring the Rules Engine in the Data Quality module. The Rules Engine provides database-driven validation using Spring Expression Language (SpEL) expressions, allowing business users to modify validation rules without code deployment.

**Requirements:** 7.1, 7.2, 7.3, 7.4, 7.5

## Table of Contents

1. [Quick Start](#quick-start)
2. [Migration from Specifications](#migration-from-specifications)
3. [Configuration Properties](#configuration-properties)
4. [Configuration Examples](#configuration-examples)
5. [Validation on Startup](#validation-on-startup)
6. [Performance Tuning](#performance-tuning)
7. [Troubleshooting](#troubleshooting)

## Quick Start

### Minimal Configuration

The minimal configuration required to enable the Rules Engine:

```yaml
data-quality:
  rules-engine:
    enabled: true  # REQUIRED - must be explicitly set
```

### Recommended Production Configuration

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 300  # 5 minutes
    
    logging:
      log-executions: false  # Reduce log volume
      log-violations: true   # Keep violation logs
      log-summary: true      # Keep summary logs
    
    performance:
      warn-threshold-ms: 100
      max-execution-time-ms: 5000
```

## Migration from Specifications

### Deprecated Specification Classes

As of version 2.0, all Specification-based validation classes have been **deprecated** and will be removed in a future release. The following classes are now deprecated:

- `CompletenessSpecifications` - Replaced by database rules with `COMPLETENESS` rule type
- `AccuracySpecifications` - Replaced by database rules with `ACCURACY` rule type
- `ConsistencySpecifications` - Replaced by database rules with `CONSISTENCY` rule type
- `TimelinessSpecifications` - Replaced by database rules with `TIMELINESS` rule type
- `UniquenessSpecifications` - Replaced by database rules with `UNIQUENESS` rule type

### Why Migrate?

The Rules Engine provides several advantages over hardcoded Specifications:

1. **Configurable:** Rules can be modified through database updates without code deployment
2. **Dynamic:** Enable/disable rules at runtime based on business needs
3. **Auditable:** All rule executions and violations are persisted to the database
4. **Flexible:** Rule parameters can be adjusted without code changes
5. **Maintainable:** Business users can manage rules without developer involvement

### Migration Path

All validation logic from Specifications has been migrated to database rules. The Rules Engine provides identical validation results while offering greater flexibility.

**Before (Deprecated):**
```java
// Using hardcoded Specifications
ValidationResult result = ValidationResult.validate(
    exposure,
    CompletenessSpecifications.hasRequiredFields(),
    AccuracySpecifications.hasPositiveAmount()
);
```

**After (Recommended):**
```java
// Using Rules Engine
ValidationResult result = ValidationResult.validate(
    exposure,
    dataQualityRulesService
);
```

### Equivalent Rules

Each Specification method has been migrated to one or more database rules:

| Specification Method | Database Rule Code | Rule Type |
|---------------------|-------------------|-----------|
| `hasRequiredFields()` | `COMPLETENESS_EXPOSURE_ID_REQUIRED`, `COMPLETENESS_AMOUNT_REQUIRED`, etc. | COMPLETENESS |
| `hasPositiveAmount()` | `ACCURACY_POSITIVE_AMOUNT` | ACCURACY |
| `hasValidCurrency()` | `ACCURACY_VALID_CURRENCY` | ACCURACY |
| `currencyMatchesCountry()` | `CONSISTENCY_CURRENCY_COUNTRY` | CONSISTENCY |
| `isWithinReportingPeriod()` | `TIMELINESS_REPORTING_PERIOD` | TIMELINESS |
| `hasUniqueExposureIds()` | `UNIQUENESS_EXPOSURE_IDS` | UNIQUENESS |

See the [Design Document](../../.kiro/specs/data-quality-rules-integration/design.md) for a complete mapping of all rules.

### Timeline

- **Version 2.0:** Specifications marked as deprecated
- **Version 2.1:** Deprecation warnings in logs
- **Version 3.0:** Specifications will be removed

---

## Configuration Properties

### Core Properties

#### `data-quality.rules-engine.enabled`

- **Type:** Boolean
- **Required:** Yes (Requirement 7.1)
- **Default:** None (must be explicitly set)
- **Description:** Enable or disable the Rules Engine. Must be set to `true` for validation to work.

**Example:**
```yaml
data-quality:
  rules-engine:
    enabled: true
```

**Validation:**
- Application will fail to start if not explicitly set
- Application will throw `IllegalStateException` if set to `false`

---

#### `data-quality.rules-engine.cache-enabled`

- **Type:** Boolean
- **Required:** No
- **Default:** `true`
- **Description:** Enable in-memory caching of rules to improve performance (Requirement 7.2)

**Example:**
```yaml
data-quality:
  rules-engine:
    cache-enabled: true
```

**Impact:**
- **Enabled:** Rules loaded once and cached in memory (recommended)
- **Disabled:** Rules loaded from database on every validation (slower)

---

#### `data-quality.rules-engine.cache-ttl`

- **Type:** Integer
- **Required:** No
- **Default:** `300` (5 minutes)
- **Unit:** Seconds
- **Minimum:** `0`
- **Description:** Time-to-live for cached rules. Rules are reloaded after this period (Requirement 7.2)

**Example:**
```yaml
data-quality:
  rules-engine:
    cache-ttl: 600  # 10 minutes
```

**Recommendations:**
- **Development:** 60-120 seconds (faster rule updates)
- **Production:** 300-600 seconds (better performance)
- **High-volume:** 600-1800 seconds (maximum performance)

**Validation:**
- Must be non-negative
- Warning logged if less than 60 seconds

---

#### `data-quality.rules-engine.parallel-execution`

- **Type:** Boolean
- **Required:** No
- **Default:** `false`
- **Description:** Enable parallel rule execution (future feature, not yet implemented)

**Example:**
```yaml
data-quality:
  rules-engine:
    parallel-execution: false
```

---

### Logging Properties (Requirement 7.4)

#### `data-quality.rules-engine.logging.log-executions`

- **Type:** Boolean
- **Required:** No
- **Default:** `true`
- **Description:** Log each individual rule execution with duration (Requirement 6.1)

**Example:**
```yaml
data-quality:
  rules-engine:
    logging:
      log-executions: true
```

**Log Output:**
```
INFO  [RulesEngine] Executed rule ACCURACY_POSITIVE_AMOUNT in 5ms - SUCCESS
INFO  [RulesEngine] Executed rule COMPLETENESS_EXPOSURE_ID in 3ms - FAILED (1 violation)
```

---

#### `data-quality.rules-engine.logging.log-violations`

- **Type:** Boolean
- **Required:** No
- **Default:** `true`
- **Description:** Log all rule violations with exposure details (Requirement 6.2)

**Example:**
```yaml
data-quality:
  rules-engine:
    logging:
      log-violations: true
```

**Log Output:**
```
WARN  [RulesEngine] Violation: ACCURACY_POSITIVE_AMOUNT for exposure EXP_001 - Amount must be positive
```

---

#### `data-quality.rules-engine.logging.log-summary`

- **Type:** Boolean
- **Required:** No
- **Default:** `true`
- **Description:** Log summary statistics after validation completes (Requirement 6.4)

**Example:**
```yaml
data-quality:
  rules-engine:
    logging:
      log-summary: true
```

**Log Output:**
```
INFO  [RulesEngine] Validation completed: 50 rules executed, 5 violations found, 250ms total
```

---

### Performance Properties (Requirement 7.3)

#### `data-quality.rules-engine.performance.warn-threshold-ms`

- **Type:** Integer
- **Required:** No
- **Default:** `100`
- **Unit:** Milliseconds
- **Minimum:** `0`
- **Description:** Warn if a single rule execution exceeds this threshold (Requirement 6.5)

**Example:**
```yaml
data-quality:
  rules-engine:
    performance:
      warn-threshold-ms: 100
```

**Log Output:**
```
WARN  [RulesEngine] Slow rule execution: CONSISTENCY_CURRENCY_COUNTRY took 150ms (threshold: 100ms)
```

---

#### `data-quality.rules-engine.performance.max-execution-time-ms`

- **Type:** Integer
- **Required:** No
- **Default:** `5000`
- **Unit:** Milliseconds
- **Minimum:** `0`
- **Description:** Warn if total validation time exceeds this threshold (Requirement 6.5)

**Example:**
```yaml
data-quality:
  rules-engine:
    performance:
      max-execution-time-ms: 5000
```

**Log Output:**
```
WARN  [RulesEngine] Slow validation: Total time 6500ms exceeded threshold of 5000ms
```

---

### Migration Properties (Requirement 7.5)

#### `data-quality.rules-migration.enabled`

- **Type:** Boolean
- **Required:** No
- **Default:** `true`
- **Description:** Enable rules migration on startup to populate initial rules

**Example:**
```yaml
data-quality:
  rules-migration:
    enabled: false  # Disable after initial migration
```

**Important:**
- Should be `true` only during initial setup
- Set to `false` after migration completes to avoid re-running
- Warning logged if enabled in production

---

## Configuration Examples

### Example 1: Development Environment

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 60  # Short TTL for faster rule updates
    
    logging:
      log-executions: true   # Verbose logging for debugging
      log-violations: true
      log-summary: true
    
    performance:
      warn-threshold-ms: 200  # Relaxed thresholds
      max-execution-time-ms: 10000
  
  rules-migration:
    enabled: true  # Enable for initial setup
```

### Example 2: Production Environment

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 600  # Long TTL for performance
    
    logging:
      log-executions: false  # Reduce log volume
      log-violations: true   # Keep violation logs
      log-summary: true      # Keep summary logs
    
    performance:
      warn-threshold-ms: 100  # Strict thresholds
      max-execution-time-ms: 5000
  
  rules-migration:
    enabled: false  # Disable after initial setup
```

### Example 3: High-Volume Production

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 1800  # 30 minutes for maximum performance
    
    logging:
      log-executions: false
      log-violations: false  # Only log summary
      log-summary: true
    
    performance:
      warn-threshold-ms: 50   # Very strict thresholds
      max-execution-time-ms: 2000
  
  rules-migration:
    enabled: false
```

### Example 4: Debugging Configuration

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: false  # Disable cache to see rule changes immediately
    
    logging:
      log-executions: true  # Maximum verbosity
      log-violations: true
      log-summary: true
    
    performance:
      warn-threshold-ms: 1000  # Very relaxed thresholds
      max-execution-time-ms: 30000
  
  rules-migration:
    enabled: true  # Re-run migration if needed
```

---

## Validation on Startup

The Rules Engine configuration is validated on application startup by `RulesEngineConfigurationValidator`. This ensures that all required properties are properly set before the application starts processing validation requests.

### Validation Checks

1. **Rules Engine Enabled** (Requirement 7.1)
   - Verifies that `rules-engine.enabled` is explicitly set to `true`
   - Throws `IllegalStateException` if disabled or not set

2. **Cache Configuration** (Requirement 7.2)
   - Validates cache TTL is positive if caching is enabled
   - Warns if cache TTL is very low (< 60 seconds)

3. **Performance Thresholds** (Requirement 7.3)
   - Validates thresholds are non-negative
   - Warns if warn threshold exceeds max execution time

4. **Logging Configuration** (Requirement 7.4)
   - Validates logging settings
   - Warns if all logging is disabled

5. **Migration Configuration** (Requirement 7.5)
   - Warns if migration is enabled (should be disabled after initial setup)

### Startup Log Output

```
================================================================================
Validating Rules Engine Configuration
================================================================================
✓ Rules Engine is ENABLED
✓ Cache configuration valid:
  - Cache enabled: true
  - Cache TTL: 300s
✓ Performance thresholds valid:
  - Warn threshold: 100ms
  - Max execution time: 5000ms
✓ Logging configuration:
  - Log executions: true
  - Log violations: true
  - Log summary: true
✓ Rules migration is DISABLED (normal operation mode)
✓ Rules Engine configuration validation completed successfully
================================================================================
```

---

## Performance Tuning

### Cache Configuration

**Problem:** Validation is slow due to frequent database queries

**Solution:** Enable caching with appropriate TTL

```yaml
data-quality:
  rules-engine:
    cache-enabled: true
    cache-ttl: 600  # Increase TTL
```

**Impact:**
- Reduces database load
- Improves validation performance
- Rules updated less frequently

---

### Logging Configuration

**Problem:** Too many log messages in production

**Solution:** Disable execution logging, keep violations and summary

```yaml
data-quality:
  rules-engine:
    logging:
      log-executions: false  # Disable verbose logging
      log-violations: true   # Keep violation logs
      log-summary: true      # Keep summary logs
```

**Impact:**
- Reduces log volume by 80-90%
- Still captures important information
- Easier to monitor violations

---

### Performance Thresholds

**Problem:** Too many performance warnings

**Solution:** Adjust thresholds based on actual performance

```yaml
data-quality:
  rules-engine:
    performance:
      warn-threshold-ms: 200  # Increase threshold
      max-execution-time-ms: 10000  # Increase threshold
```

**Impact:**
- Fewer false-positive warnings
- Focus on actual performance issues
- Better signal-to-noise ratio

---

## Troubleshooting

### Error: "Rules Engine is DISABLED"

**Symptom:**
```
IllegalStateException: Rules Engine is DISABLED. Validation will not work.
```

**Cause:** `rules-engine.enabled` is not set or set to `false`

**Solution:**
```yaml
data-quality:
  rules-engine:
    enabled: true  # Must be explicitly set
```

---

### Error: "Cache TTL must be positive"

**Symptom:**
```
IllegalStateException: Cache TTL must be positive when caching is enabled.
```

**Cause:** `cache-ttl` is set to 0 or negative value

**Solution:**
```yaml
data-quality:
  rules-engine:
    cache-ttl: 300  # Must be positive
```

---

### Warning: "Cache TTL is very low"

**Symptom:**
```
WARN: Cache TTL is very low (30s). Consider increasing to at least 60s.
```

**Cause:** `cache-ttl` is less than 60 seconds

**Solution:**
```yaml
data-quality:
  rules-engine:
    cache-ttl: 60  # Increase to at least 60
```

---

### Warning: "Rules migration is ENABLED"

**Symptom:**
```
WARN: Rules migration is ENABLED. This should only be enabled during initial setup.
```

**Cause:** `rules-migration.enabled` is still `true` after initial setup

**Solution:**
```yaml
data-quality:
  rules-migration:
    enabled: false  # Disable after migration
```

---

### Warning: "All logging is disabled"

**Symptom:**
```
WARN: All logging is disabled. This may make troubleshooting difficult.
```

**Cause:** All logging flags are set to `false`

**Solution:**
```yaml
data-quality:
  rules-engine:
    logging:
      log-executions: false
      log-violations: true   # Enable at least violations
      log-summary: true      # Enable at least summary
```

---

## Environment Variables

Configuration properties can be overridden using environment variables:

```bash
# Enable Rules Engine
export DATA_QUALITY_RULES_ENGINE_ENABLED=true

# Configure cache
export DATA_QUALITY_RULES_ENGINE_CACHE_ENABLED=true
export DATA_QUALITY_RULES_ENGINE_CACHE_TTL=600

# Configure logging
export DATA_QUALITY_RULES_ENGINE_LOGGING_LOG_EXECUTIONS=false
export DATA_QUALITY_RULES_ENGINE_LOGGING_LOG_VIOLATIONS=true
export DATA_QUALITY_RULES_ENGINE_LOGGING_LOG_SUMMARY=true

# Configure performance
export DATA_QUALITY_RULES_ENGINE_PERFORMANCE_WARN_THRESHOLD_MS=100
export DATA_QUALITY_RULES_ENGINE_PERFORMANCE_MAX_EXECUTION_TIME_MS=5000

# Disable migration
export DATA_QUALITY_RULES_MIGRATION_ENABLED=false
```

---

## Related Documentation

- [Design Document](../../.kiro/specs/data-quality-rules-integration/design.md)
- [Requirements Document](../../.kiro/specs/data-quality-rules-integration/requirements.md)
- [Implementation Tasks](../../.kiro/specs/data-quality-rules-integration/tasks.md)

---

## Support

For issues or questions about Rules Engine configuration:

1. Check the startup logs for validation errors
2. Review this configuration guide
3. Consult the design document for architecture details
4. Contact the development team
