# Rules Engine Integration Guide

## Overview

The Data Quality Rules Engine is a database-driven validation system that replaces hardcoded Java Specifications with configurable rules stored in PostgreSQL. This guide covers the integration, configuration, rule management, and operational procedures for the Rules Engine.

**Key Benefits:**
- **Configurable:** Modify validation rules without code deployment
- **Dynamic:** Enable/disable rules at runtime
- **Auditable:** Complete audit trail of all rule executions and violations
- **Flexible:** Support for exemptions, custom parameters, and severity levels
- **Maintainable:** Business users can manage rules through database updates

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Getting Started](#getting-started)
3. [Rule Management](#rule-management)
4. [Configuration](#configuration)
5. [Migration Process](#migration-process)
6. [Operational Runbook](#operational-runbook)
7. [Troubleshooting](#troubleshooting)
8. [Rollback Procedures](#rollback-procedures)

---

## Architecture Overview

### Components

The Rules Engine consists of several key components:

- **DataQualityRulesService**: Orchestrates rule execution and violation handling
- **DefaultRulesEngine**: Loads and caches rules, executes SpEL expressions
- **SpelExpressionEvaluator**: Evaluates Spring Expression Language (SpEL) expressions
- **RuleContext**: Provides access to exposure data and parameters during evaluation
- **BusinessRuleRepository**: Manages rule persistence and retrieval
- **RuleViolationRepository**: Persists detected violations for audit trail
- **RuleExecutionLogRepository**: Logs all rule executions with performance metrics

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Validation Request                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ValidationResult                           │
│  - Orchestrates validation flow                                 │
│  - Calls DataQualityRulesService                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DataQualityRulesService                        │
│  - Loads active rules from database                             │
│  - Executes rules via DefaultRulesEngine                        │
│  - Converts violations to ValidationErrors                      │
│  - Persists violations and execution logs                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DefaultRulesEngine                           │
│  - Caches rules in memory (configurable TTL)                    │
│  - Evaluates SpEL expressions                                   │
│  - Handles exemptions                                           │
│  - Returns RuleExecutionResults                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SpelExpressionEvaluator                        │
│  - Parses and evaluates SpEL expressions                        │
│  - Provides custom functions (DAYS_BETWEEN, NOW, TODAY)         │
│  - Handles null values gracefully                               │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Validation Request**: `ValidationResult.validate(exposure, rulesService)` is called
2. **Rule Loading**: Active rules loaded from database or cache
3. **Rule Execution**: Each rule evaluated against exposure using SpEL
4. **Violation Detection**: Failed rules create `RuleViolation` objects
5. **Error Mapping**: Violations converted to `ValidationError` objects
6. **Persistence**: Violations and execution logs saved to database
7. **Result Return**: `ValidationResult` returned with all errors

### Database Schema

The Rules Engine uses the following database tables:

**dataquality.business_rules**
- Stores rule definitions with SpEL expressions
- Fields: rule_code, rule_name, rule_type, rule_expression, enabled, severity

**dataquality.rule_parameters**
- Stores configurable parameters for rules
- Fields: rule_id, parameter_name, parameter_value, parameter_type

**dataquality.rule_lists**
- Stores list values for rules (e.g., valid currencies)
- Fields: rule_id, list_name, list_values (array)

**dataquality.rule_violations**
- Audit trail of all detected violations
- Fields: batch_id, exposure_id, rule_code, violation_message, detected_at

**dataquality.rule_execution_log**
- Performance metrics for rule executions
- Fields: rule_code, execution_duration_ms, status, violation_count, executed_at

**dataquality.rule_exemptions**
- Manages approved exemptions for specific rules
- Fields: rule_code, exemption_reason, valid_from, valid_to, status

---

## Getting Started

### Prerequisites

1. **PostgreSQL Database**: Version 12 or higher
2. **Spring Boot Application**: Version 2.7 or higher
3. **Java**: Version 17 or higher

### Step 1: Enable Rules Engine

Add the following configuration to your `application.yml`:

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 300  # 5 minutes
```

### Step 2: Run Database Migration

The Rules Engine tables are created automatically via Flyway migration:

```sql
-- Migration: V1.8__create_rules_engine_tables.sql
-- Creates: business_rules, rule_parameters, rule_lists, 
--          rule_violations, rule_execution_log, rule_exemptions
```

### Step 3: Populate Initial Rules

Enable rules migration on first startup:

```yaml
data-quality:
  rules-migration:
    enabled: true  # Set to false after initial migration
```

This will populate the database with all migrated rules from Specifications.

### Step 4: Verify Installation

Check the startup logs:

```
INFO  [RulesEngine] Loaded 25 active rules from database
INFO  [RulesEngine] Rules Engine initialized successfully
INFO  [RulesEngine] Cache enabled with TTL: 300s
```

### Step 5: Run Validation

The Rules Engine is now integrated into the validation flow:

```java
@Autowired
private DataQualityRulesService rulesService;

public void validateBatch(List<ExposureRecord> exposures) {
    for (ExposureRecord exposure : exposures) {
        ValidationResult result = ValidationResult.validate(exposure, rulesService);
        
        if (!result.isValid()) {
            // Handle validation errors
            List<ValidationError> errors = result.getErrors();
        }
    }
}
```

---

## Rule Management

### Viewing Rules

Query all active rules:

```sql
SELECT rule_code, rule_name, rule_type, enabled
FROM dataquality.business_rules
WHERE enabled = true
ORDER BY rule_type, rule_code;
```

View a specific rule with its parameters:

```sql
SELECT 
    br.rule_code,
    br.rule_name,
    br.rule_expression,
    br.severity,
    rp.parameter_name,
    rp.parameter_value
FROM dataquality.business_rules br
LEFT JOIN dataquality.rule_parameters rp ON br.id = rp.rule_id
WHERE br.rule_code = 'ACCURACY_POSITIVE_AMOUNT';
```

### Enabling/Disabling Rules

Disable a rule:

```sql
UPDATE dataquality.business_rules
SET enabled = false
WHERE rule_code = 'ACCURACY_REASONABLE_AMOUNT';
```

Enable a rule:

```sql
UPDATE dataquality.business_rules
SET enabled = true
WHERE rule_code = 'ACCURACY_REASONABLE_AMOUNT';
```

**Note:** Changes take effect after cache TTL expires (default: 5 minutes).

### Updating Rule Parameters

Update a parameter value:

```sql
UPDATE dataquality.rule_parameters
SET parameter_value = '1000000'
WHERE rule_id = (SELECT id FROM dataquality.business_rules WHERE rule_code = 'ACCURACY_REASONABLE_AMOUNT')
  AND parameter_name = 'maxReasonableAmount';
```

Update a list parameter:

```sql
UPDATE dataquality.rule_lists
SET list_values = ARRAY['EUR', 'USD', 'GBP', 'CHF', 'JPY']
WHERE rule_id = (SELECT id FROM dataquality.business_rules WHERE rule_code = 'ACCURACY_VALID_CURRENCY')
  AND list_name = 'validCurrencies';
```

### Modifying Rule Expressions

Update a rule's SpEL expression:

```sql
UPDATE dataquality.business_rules
SET rule_expression = '#amount > 0 && #amount < 10000000'
WHERE rule_code = 'ACCURACY_POSITIVE_AMOUNT';
```

**Warning:** Test expression changes thoroughly before deploying to production!

### Adding New Rules

Insert a new rule:

```sql
-- Insert the rule
INSERT INTO dataquality.business_rules (
    rule_code, 
    rule_name, 
    rule_type, 
    rule_expression, 
    error_message,
    enabled,
    severity
) VALUES (
    'ACCURACY_VALID_PRODUCT_TYPE',
    'Valid Product Type',
    'ACCURACY',
    '#validProductTypes.contains(#productType)',
    'Product type must be one of the valid types',
    true,
    'ERROR'
);

-- Add list parameter
INSERT INTO dataquality.rule_lists (
    rule_id,
    list_name,
    list_values
) VALUES (
    (SELECT id FROM dataquality.business_rules WHERE rule_code = 'ACCURACY_VALID_PRODUCT_TYPE'),
    'validProductTypes',
    ARRAY['LOAN', 'BOND', 'EQUITY', 'DERIVATIVE']
);
```

### Managing Exemptions

Create an exemption:

```sql
INSERT INTO dataquality.rule_exemptions (
    rule_id,
    exemption_reason,
    valid_from,
    valid_to,
    status,
    created_by
) VALUES (
    (SELECT id FROM dataquality.business_rules WHERE rule_code = 'TIMELINESS_REPORTING_PERIOD'),
    'Year-end processing exception',
    '2025-12-20',
    '2026-01-10',
    'ACTIVE',
    'admin@example.com'
);
```

Revoke an exemption:

```sql
UPDATE dataquality.rule_exemptions
SET status = 'REVOKED'
WHERE id = 123;
```

---

## Configuration

For complete configuration reference, see [RULES_ENGINE_CONFIGURATION_GUIDE.md](RULES_ENGINE_CONFIGURATION_GUIDE.md).

### Quick Configuration Examples

**Development:**
```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-ttl: 60  # Short TTL for faster updates
    logging:
      log-executions: true  # Verbose logging
```

**Production:**
```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-ttl: 600  # Long TTL for performance
    logging:
      log-executions: false  # Reduce log volume
      log-violations: true
```

---

## Migration Process

### Overview

The migration from Specifications to Rules Engine involves:

1. **Rule Migration**: Convert Specification logic to database rules
2. **Code Update**: Replace Specification calls with Rules Engine
3. **Testing**: Verify identical validation results
4. **Deployment**: Roll out to production with monitoring

### Phase 1: Rule Migration

All Specification logic has been migrated to database rules:

| Specification Class | Rule Count | Rule Type |
|-------------------|-----------|-----------|
| CompletenessSpecifications | 8 rules | COMPLETENESS |
| AccuracySpecifications | 5 rules | ACCURACY |
| ConsistencySpecifications | 3 rules | CONSISTENCY |
| TimelinessSpecifications | 3 rules | TIMELINESS |
| UniquenessSpecifications | 2 rules | UNIQUENESS |
| ValiditySpecifications | 3 rules | VALIDITY |

**Total: 24 rules migrated**

### Phase 2: Code Update

**Before (Deprecated):**
```java
ValidationResult result = ValidationResult.validate(
    exposure,
    CompletenessSpecifications.hasRequiredFields(),
    AccuracySpecifications.hasPositiveAmount()
);
```

**After (Current):**
```java
ValidationResult result = ValidationResult.validate(
    exposure,
    dataQualityRulesService
);
```

### Phase 3: Testing

Run all tests to verify equivalence:

```bash
# Run all data quality tests
mvn test -pl regtech-data-quality

# Expected: All 309 tests pass
```

### Phase 4: Deployment

1. **Enable Rules Engine** in configuration
2. **Run migration** to populate rules
3. **Monitor logs** for errors
4. **Verify validation** results match expectations
5. **Disable migration** flag after successful deployment

### Migration Checklist

- [ ] Rules Engine enabled in configuration
- [ ] Database migration completed successfully
- [ ] Initial rules populated (24 rules)
- [ ] All tests passing (309 tests)
- [ ] Validation results verified in staging
- [ ] Performance metrics within acceptable range (< 10% degradation)
- [ ] Migration flag disabled
- [ ] Specifications marked as deprecated
- [ ] Documentation updated

---

## Operational Runbook

### Daily Operations

#### Monitor Rule Execution

Check rule execution logs:

```sql
SELECT 
    rule_code,
    COUNT(*) as execution_count,
    AVG(execution_duration_ms) as avg_duration_ms,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failure_count
FROM dataquality.rule_execution_log
WHERE executed_at >= NOW() - INTERVAL '24 hours'
GROUP BY rule_code
ORDER BY avg_duration_ms DESC;
```

#### Monitor Violations

Check recent violations:

```sql
SELECT 
    rule_code,
    COUNT(*) as violation_count,
    COUNT(DISTINCT batch_id) as affected_batches
FROM dataquality.rule_violations
WHERE detected_at >= NOW() - INTERVAL '24 hours'
GROUP BY rule_code
ORDER BY violation_count DESC;
```

### Weekly Operations

#### Review Performance

Identify slow rules:

```sql
SELECT 
    rule_code,
    AVG(execution_duration_ms) as avg_duration_ms,
    MAX(execution_duration_ms) as max_duration_ms,
    COUNT(*) as execution_count
FROM dataquality.rule_execution_log
WHERE executed_at >= NOW() - INTERVAL '7 days'
GROUP BY rule_code
HAVING AVG(execution_duration_ms) > 100
ORDER BY avg_duration_ms DESC;
```

#### Review Cache Performance

Check cache hit rate in application logs:

```
INFO  [RulesEngine] Cache statistics: 
  - Hit rate: 98.5%
  - Miss rate: 1.5%
  - Evictions: 12
```

### Monthly Operations

#### Review Rule Effectiveness

Identify rules with high violation rates:

```sql
SELECT 
    br.rule_code,
    br.rule_name,
    COUNT(rv.id) as violation_count,
    COUNT(DISTINCT rv.batch_id) as affected_batches
FROM dataquality.business_rules br
LEFT JOIN dataquality.rule_violations rv ON br.rule_code = rv.rule_code
WHERE rv.detected_at >= NOW() - INTERVAL '30 days'
GROUP BY br.rule_code, br.rule_name
ORDER BY violation_count DESC
LIMIT 10;
```

#### Review Exemptions

Check expiring exemptions:

```sql
SELECT 
    br.rule_code,
    re.exemption_reason,
    re.valid_to,
    re.status
FROM dataquality.rule_exemptions re
JOIN dataquality.business_rules br ON re.rule_id = br.id
WHERE re.valid_to <= NOW() + INTERVAL '30 days'
  AND re.status = 'ACTIVE'
ORDER BY re.valid_to;
```

### Common Tasks

#### Clear Rule Cache

Restart the application or wait for TTL expiration. To force immediate reload:

```bash
# Restart the application
systemctl restart regtech-app
```

#### Bulk Enable/Disable Rules

Disable all rules of a specific type:

```sql
UPDATE dataquality.business_rules
SET enabled = false
WHERE rule_type = 'TIMELINESS';
```

Enable all rules:

```sql
UPDATE dataquality.business_rules
SET enabled = true;
```

#### Export Rules for Backup

```sql
COPY (
    SELECT rule_code, rule_name, rule_type, rule_expression, 
           error_message, enabled, severity
    FROM dataquality.business_rules
) TO '/tmp/rules_backup.csv' WITH CSV HEADER;
```

---

## Troubleshooting

### Issue: Rules Not Loading

**Symptoms:**
- Error: "No active rules found in database"
- Validation not executing

**Diagnosis:**
```sql
SELECT COUNT(*) FROM dataquality.business_rules WHERE enabled = true;
```

**Solutions:**
1. Run rules migration: Set `rules-migration.enabled: true` and restart
2. Check database connectivity
3. Verify Flyway migrations completed successfully

---

### Issue: Slow Validation Performance

**Symptoms:**
- Validation takes longer than expected
- Performance warnings in logs

**Diagnosis:**
```sql
SELECT rule_code, AVG(execution_duration_ms) as avg_ms
FROM dataquality.rule_execution_log
WHERE executed_at >= NOW() - INTERVAL '1 hour'
GROUP BY rule_code
ORDER BY avg_ms DESC
LIMIT 10;
```

**Solutions:**
1. Increase cache TTL: `cache-ttl: 600`
2. Disable verbose logging: `log-executions: false`
3. Optimize slow rule expressions
4. Check database query performance

---

### Issue: Cache Not Working

**Symptoms:**
- High database load
- Rules loaded on every validation

**Diagnosis:**
Check configuration:
```yaml
data-quality:
  rules-engine:
    cache-enabled: true  # Must be true
    cache-ttl: 300       # Must be positive
```

**Solutions:**
1. Verify cache is enabled
2. Check cache TTL is positive
3. Review application logs for cache errors

---

### Issue: Rule Expression Errors

**Symptoms:**
- Error: "Failed to evaluate rule expression"
- Rule execution marked as FAILED

**Diagnosis:**
```sql
SELECT rule_code, rule_expression
FROM dataquality.business_rules
WHERE rule_code = 'PROBLEMATIC_RULE';
```

**Solutions:**
1. Validate SpEL syntax
2. Check field names match ExposureRecord
3. Verify parameter names are correct
4. Test expression in isolation

---

### Issue: Violations Not Persisting

**Symptoms:**
- Violations detected but not in database
- Missing audit trail

**Diagnosis:**
```sql
SELECT COUNT(*) FROM dataquality.rule_violations
WHERE detected_at >= NOW() - INTERVAL '1 hour';
```

**Solutions:**
1. Check database connectivity
2. Verify transaction management
3. Review application logs for persistence errors
4. Check database permissions

---

## Rollback Procedures

### Scenario 1: Rules Engine Issues in Production

If the Rules Engine encounters critical issues in production:

#### Step 1: Assess Impact

- Check error logs
- Verify validation is failing
- Determine scope of impact

#### Step 2: Immediate Mitigation

**Option A: Disable Problematic Rules**

```sql
-- Disable specific problematic rule
UPDATE dataquality.business_rules
SET enabled = false
WHERE rule_code = 'PROBLEMATIC_RULE';
```

**Option B: Rollback to Previous Version**

```bash
# Deploy previous application version
kubectl rollout undo deployment/regtech-app

# Or using Docker
docker-compose down
docker-compose up -d --force-recreate
```

#### Step 3: Verify Recovery

- Check validation is working
- Monitor error logs
- Verify no data loss

#### Step 4: Root Cause Analysis

- Review rule changes
- Check configuration changes
- Analyze error logs
- Identify fix needed

---

### Scenario 2: Performance Degradation

If validation performance degrades significantly:

#### Step 1: Identify Slow Rules

```sql
SELECT rule_code, AVG(execution_duration_ms) as avg_ms
FROM dataquality.rule_execution_log
WHERE executed_at >= NOW() - INTERVAL '1 hour'
GROUP BY rule_code
HAVING AVG(execution_duration_ms) > 200
ORDER BY avg_ms DESC;
```

#### Step 2: Temporary Mitigation

```sql
-- Disable slow rules temporarily
UPDATE dataquality.business_rules
SET enabled = false
WHERE rule_code IN ('SLOW_RULE_1', 'SLOW_RULE_2');
```

#### Step 3: Optimize Configuration

```yaml
data-quality:
  rules-engine:
    cache-ttl: 1800  # Increase cache TTL
    logging:
      log-executions: false  # Reduce logging overhead
```

#### Step 4: Optimize Rules

- Review and optimize SpEL expressions
- Add indexes to database tables
- Consider rule consolidation

---

### Scenario 3: Data Corruption

If rule data becomes corrupted:

#### Step 1: Stop Processing

```bash
# Stop the application
systemctl stop regtech-app
```

#### Step 2: Restore from Backup

```sql
-- Restore rules from backup
TRUNCATE dataquality.business_rules CASCADE;

COPY dataquality.business_rules FROM '/backup/rules_backup.csv' WITH CSV HEADER;
```

#### Step 3: Verify Data Integrity

```sql
-- Check rule count
SELECT COUNT(*) FROM dataquality.business_rules;

-- Verify rule expressions are valid
SELECT rule_code, rule_expression
FROM dataquality.business_rules
WHERE rule_expression IS NULL OR rule_expression = '';
```

#### Step 4: Restart Application

```bash
systemctl start regtech-app
```

---

### Scenario 4: Complete Rollback to Specifications

If a complete rollback to Specifications is required (emergency only):

#### Step 1: Disable Rules Engine

```yaml
data-quality:
  rules-engine:
    enabled: false  # Disable Rules Engine
```

#### Step 2: Re-enable Specifications

This requires code changes to revert to Specification-based validation. Deploy previous version:

```bash
# Deploy version before Rules Engine integration
git checkout tags/v1.9.0
mvn clean package
# Deploy artifact
```

#### Step 3: Verify Validation

- Run all tests
- Verify validation results
- Monitor for errors

#### Step 4: Plan Forward

- Identify root cause of Rules Engine issues
- Plan fix and re-deployment
- Schedule re-migration

---

## Best Practices

### Rule Management

1. **Test Before Deploying**: Always test rule changes in staging first
2. **Use Exemptions**: For temporary exceptions, use exemptions instead of disabling rules
3. **Document Changes**: Add comments when modifying rules
4. **Version Control**: Keep backups of rule configurations
5. **Monitor Performance**: Regularly review rule execution times

### Configuration

1. **Production Settings**: Use conservative cache TTL and minimal logging
2. **Development Settings**: Use short cache TTL and verbose logging
3. **Environment Variables**: Use environment-specific configuration
4. **Validation**: Always validate configuration on startup

### Operations

1. **Regular Monitoring**: Check rule execution logs daily
2. **Performance Review**: Review slow rules weekly
3. **Exemption Management**: Review expiring exemptions monthly
4. **Backup Strategy**: Regular backups of rule configurations
5. **Incident Response**: Have rollback procedures ready

---

## Related Documentation

- [Rules Engine Configuration Guide](RULES_ENGINE_CONFIGURATION_GUIDE.md) - Complete configuration reference
- [Specification Deprecation Notice](SPECIFICATION_DEPRECATION_NOTICE.md) - Migration timeline and guidance
- [Design Document](../.kiro/specs/data-quality-rules-integration/design.md) - Architecture and design decisions
- [Requirements Document](../.kiro/specs/data-quality-rules-integration/requirements.md) - Functional requirements
- [Implementation Tasks](../.kiro/specs/data-quality-rules-integration/tasks.md) - Implementation checklist

---

## Support

For issues or questions about the Rules Engine:

1. Check this integration guide
2. Review the configuration guide
3. Check application logs
4. Query database for rule status
5. Contact the development team

---

*Last Updated: November 26, 2025*  
*Version: 2.0*  
*Requirement: 11.7