# Rules Engine Operational Runbook

## Overview

This runbook provides step-by-step procedures for common operational tasks related to the Data Quality Rules Engine. It is intended for DevOps engineers, system administrators, and support personnel.

**Requirement: 11.7**

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Daily Operations](#daily-operations)
3. [Common Tasks](#common-tasks)
4. [Emergency Procedures](#emergency-procedures)
5. [Monitoring and Alerts](#monitoring-and-alerts)
6. [Performance Tuning](#performance-tuning)
7. [Troubleshooting Guide](#troubleshooting-guide)

---

## Quick Reference

### Key Configuration

```yaml
data-quality:
  rules-engine:
    enabled: true
    cache-enabled: true
    cache-ttl: 300
```

### Important Database Tables

- `business_rules` - Rule definitions
- `rule_parameters` - Rule parameters
- `rule_violations` - Violation audit trail
- `rule_execution_log` - Performance metrics
- `rule_exemptions` - Approved exemptions

### Key Metrics

- **Rule Execution Time**: < 100ms per rule (warning threshold)
- **Total Validation Time**: < 5000ms per batch (warning threshold)
- **Cache Hit Rate**: > 95% (target)
- **Violation Rate**: Varies by rule (monitor trends)

---

## Daily Operations

### Morning Health Check

**Frequency:** Daily at 9:00 AM

**Steps:**

1. **Check Application Status**
   ```bash
   systemctl status regtech-app
   ```

2. **Verify Rules Engine is Running**
   ```bash
   grep "Rules Engine initialized successfully" /var/log/regtech/application.log
   ```

3. **Check Rule Count**
   ```sql
   SELECT COUNT(*) as active_rules 
   FROM business_rules 
   WHERE enabled = true;
   -- Expected: 24 rules
   ```

4. **Review Overnight Violations**
   ```sql
   SELECT 
       rule_code,
       COUNT(*) as violation_count
   FROM rule_violations
   WHERE detected_at >= CURRENT_DATE
   GROUP BY rule_code
   ORDER BY violation_count DESC
   LIMIT 10;
   ```

5. **Check for Errors**
   ```bash
   grep -i "error\|exception" /var/log/regtech/application.log | tail -20
   ```

**Expected Results:**
- Application running
- Rules Engine initialized
- 24 active rules
- Violations within normal range
- No critical errors

---

### End of Day Review

**Frequency:** Daily at 5:00 PM

**Steps:**

1. **Review Daily Statistics**
   ```sql
   SELECT 
       COUNT(DISTINCT batch_id) as batches_processed,
       COUNT(*) as total_validations,
       SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_validations
   FROM rule_execution_log
   WHERE executed_at >= CURRENT_DATE;
   ```

2. **Check Performance Metrics**
   ```sql
   SELECT 
       rule_code,
       AVG(execution_duration_ms) as avg_ms,
       MAX(execution_duration_ms) as max_ms
   FROM rule_execution_log
   WHERE executed_at >= CURRENT_DATE
   GROUP BY rule_code
   HAVING AVG(execution_duration_ms) > 100
   ORDER BY avg_ms DESC;
   ```

3. **Review Cache Performance**
   ```bash
   grep "Cache statistics" /var/log/regtech/application.log | tail -1
   ```

4. **Document Issues**
   - Note any anomalies
   - Create tickets for investigation
   - Update runbook if needed

---

## Common Tasks

### Task 1: Enable a Rule

**When:** Business requests a rule to be activated

**Steps:**

1. **Identify the Rule**
   ```sql
   SELECT id, rule_code, rule_name, enabled
   FROM business_rules
   WHERE rule_code = 'RULE_CODE_HERE';
   ```

2. **Enable the Rule**
   ```sql
   UPDATE business_rules
   SET enabled = true
   WHERE rule_code = 'RULE_CODE_HERE';
   ```

3. **Verify Change**
   ```sql
   SELECT rule_code, enabled
   FROM business_rules
   WHERE rule_code = 'RULE_CODE_HERE';
   ```

4. **Wait for Cache Refresh**
   - Default TTL: 5 minutes
   - Or restart application for immediate effect

5. **Monitor Execution**
   ```sql
   SELECT *
   FROM rule_execution_log
   WHERE rule_code = 'RULE_CODE_HERE'
   ORDER BY executed_at DESC
   LIMIT 10;
   ```

**Rollback:**
```sql
UPDATE business_rules
SET enabled = false
WHERE rule_code = 'RULE_CODE_HERE';
```

---

### Task 2: Disable a Rule

**When:** Rule causing issues or business requests deactivation

**Steps:**

1. **Document Reason**
   - Create ticket with justification
   - Note expected duration

2. **Disable the Rule**
   ```sql
   UPDATE business_rules
   SET enabled = false
   WHERE rule_code = 'RULE_CODE_HERE';
   ```

3. **Verify Change**
   ```sql
   SELECT rule_code, enabled
   FROM business_rules
   WHERE rule_code = 'RULE_CODE_HERE';
   ```

4. **Notify Stakeholders**
   - Send email to business users
   - Update status dashboard

5. **Monitor Impact**
   ```sql
   -- Check if violations stopped
   SELECT COUNT(*)
   FROM rule_violations
   WHERE rule_code = 'RULE_CODE_HERE'
     AND detected_at >= NOW() - INTERVAL '1 hour';
   ```

---

### Task 3: Update Rule Parameter

**When:** Business requests parameter adjustment (e.g., threshold change)

**Steps:**

1. **Identify Current Value**
   ```sql
   SELECT 
       br.rule_code,
       rp.parameter_name,
       rp.parameter_value
   FROM business_rules br
   JOIN rule_parameters rp ON br.id = rp.rule_id
   WHERE br.rule_code = 'RULE_CODE_HERE';
   ```

2. **Backup Current Value**
   ```sql
   -- Document in ticket or spreadsheet
   ```

3. **Update Parameter**
   ```sql
   UPDATE rule_parameters
   SET parameter_value = 'NEW_VALUE'
   WHERE rule_id = (SELECT id FROM business_rules WHERE rule_code = 'RULE_CODE_HERE')
     AND parameter_name = 'PARAMETER_NAME';
   ```

4. **Verify Change**
   ```sql
   SELECT parameter_name, parameter_value
   FROM rule_parameters
   WHERE rule_id = (SELECT id FROM business_rules WHERE rule_code = 'RULE_CODE_HERE');
   ```

5. **Wait for Cache Refresh**
   - Default TTL: 5 minutes
   - Monitor next validation batch

6. **Verify Impact**
   ```sql
   -- Check violation rate after change
   SELECT COUNT(*)
   FROM rule_violations
   WHERE rule_code = 'RULE_CODE_HERE'
     AND detected_at >= NOW() - INTERVAL '1 hour';
   ```

**Rollback:**
```sql
UPDATE rule_parameters
SET parameter_value = 'OLD_VALUE'
WHERE rule_id = (SELECT id FROM business_rules WHERE rule_code = 'RULE_CODE_HERE')
  AND parameter_name = 'PARAMETER_NAME';
```

---

### Task 4: Create Rule Exemption

**When:** Business approves temporary exception to a rule

**Steps:**

1. **Verify Approval**
   - Confirm exemption is approved
   - Document approval reference

2. **Create Exemption**
   ```sql
   INSERT INTO rule_exemptions (
       rule_id,
       exemption_reason,
       valid_from,
       valid_to,
       status,
       created_by,
       created_at
   ) VALUES (
       (SELECT id FROM business_rules WHERE rule_code = 'RULE_CODE_HERE'),
       'Reason for exemption',
       '2025-12-01',
       '2025-12-31',
       'ACTIVE',
       'admin@example.com',
       NOW()
   );
   ```

3. **Verify Exemption**
   ```sql
   SELECT 
       br.rule_code,
       re.exemption_reason,
       re.valid_from,
       re.valid_to,
       re.status
   FROM rule_exemptions re
   JOIN business_rules br ON re.rule_id = br.id
   WHERE br.rule_code = 'RULE_CODE_HERE'
     AND re.status = 'ACTIVE';
   ```

4. **Monitor Impact**
   ```sql
   -- Violations should stop for exempted rule
   SELECT COUNT(*)
   FROM rule_violations
   WHERE rule_code = 'RULE_CODE_HERE'
     AND detected_at >= NOW() - INTERVAL '1 hour';
   ```

5. **Set Reminder**
   - Create calendar reminder for expiration date
   - Review exemption before expiry

---

### Task 5: Revoke Rule Exemption

**When:** Exemption period ends or needs to be cancelled early

**Steps:**

1. **Identify Exemption**
   ```sql
   SELECT 
       re.id,
       br.rule_code,
       re.exemption_reason,
       re.valid_to,
       re.status
   FROM rule_exemptions re
   JOIN business_rules br ON re.rule_id = br.id
   WHERE br.rule_code = 'RULE_CODE_HERE'
     AND re.status = 'ACTIVE';
   ```

2. **Revoke Exemption**
   ```sql
   UPDATE rule_exemptions
   SET status = 'REVOKED',
       revoked_at = NOW(),
       revoked_by = 'admin@example.com'
   WHERE id = EXEMPTION_ID;
   ```

3. **Verify Revocation**
   ```sql
   SELECT status, revoked_at
   FROM rule_exemptions
   WHERE id = EXEMPTION_ID;
   ```

4. **Monitor Impact**
   ```sql
   -- Violations should resume
   SELECT COUNT(*)
   FROM rule_violations
   WHERE rule_code = 'RULE_CODE_HERE'
     AND detected_at >= NOW() - INTERVAL '1 hour';
   ```

---

### Task 6: Clear Rule Cache

**When:** Need immediate rule changes without waiting for TTL

**Steps:**

1. **Restart Application**
   ```bash
   systemctl restart regtech-app
   ```

2. **Verify Restart**
   ```bash
   systemctl status regtech-app
   ```

3. **Check Logs**
   ```bash
   tail -f /var/log/regtech/application.log | grep "Rules Engine"
   ```

4. **Verify Rules Loaded**
   ```bash
   grep "Loaded .* active rules" /var/log/regtech/application.log | tail -1
   ```

**Expected Output:**
```
INFO  [RulesEngine] Loaded 24 active rules from database
INFO  [RulesEngine] Rules Engine initialized successfully
```

---

### Task 7: Export Rules for Backup

**When:** Before major changes or monthly backup

**Steps:**

1. **Export Rules**
   ```sql
   COPY (
       SELECT 
           rule_code,
           rule_name,
           rule_type,
           rule_expression,
           error_message,
           enabled,
           severity
       FROM business_rules
       ORDER BY rule_code
   ) TO '/tmp/rules_backup_YYYYMMDD.csv' WITH CSV HEADER;
   ```

2. **Export Parameters**
   ```sql
   COPY (
       SELECT 
           br.rule_code,
           rp.parameter_name,
           rp.parameter_value,
           rp.parameter_type
       FROM rule_parameters rp
       JOIN business_rules br ON rp.rule_id = br.id
       ORDER BY br.rule_code, rp.parameter_name
   ) TO '/tmp/parameters_backup_YYYYMMDD.csv' WITH CSV HEADER;
   ```

3. **Export Lists**
   ```sql
   COPY (
       SELECT 
           br.rule_code,
           rl.list_name,
           rl.list_values
       FROM rule_lists rl
       JOIN business_rules br ON rl.rule_id = br.id
       ORDER BY br.rule_code, rl.list_name
   ) TO '/tmp/lists_backup_YYYYMMDD.csv' WITH CSV HEADER;
   ```

4. **Store Backups**
   ```bash
   # Move to backup location
   mv /tmp/*_backup_YYYYMMDD.csv /backup/rules/
   
   # Set permissions
   chmod 600 /backup/rules/*_backup_YYYYMMDD.csv
   ```

5. **Verify Backups**
   ```bash
   ls -lh /backup/rules/*_backup_YYYYMMDD.csv
   ```

---

## Emergency Procedures

### Emergency 1: Rules Engine Not Loading

**Symptoms:**
- Error: "No active rules found"
- Validation failing
- Application startup errors

**Immediate Actions:**

1. **Check Database Connection**
   ```bash
   psql -h localhost -U regtech -d regtech_db -c "SELECT 1;"
   ```

2. **Check Rule Count**
   ```sql
   SELECT COUNT(*) FROM business_rules WHERE enabled = true;
   ```

3. **If No Rules Found:**
   ```yaml
   # Enable migration
   data-quality:
     rules-migration:
       enabled: true
   ```
   
   ```bash
   # Restart application
   systemctl restart regtech-app
   ```

4. **Verify Rules Loaded**
   ```bash
   grep "Loaded .* active rules" /var/log/regtech/application.log | tail -1
   ```

5. **Disable Migration**
   ```yaml
   data-quality:
     rules-migration:
       enabled: false
   ```

---

### Emergency 2: Validation Performance Degradation

**Symptoms:**
- Slow validation times
- Timeouts
- High CPU usage

**Immediate Actions:**

1. **Identify Slow Rules**
   ```sql
   SELECT 
       rule_code,
       AVG(execution_duration_ms) as avg_ms,
       COUNT(*) as execution_count
   FROM rule_execution_log
   WHERE executed_at >= NOW() - INTERVAL '1 hour'
   GROUP BY rule_code
   HAVING AVG(execution_duration_ms) > 200
   ORDER BY avg_ms DESC;
   ```

2. **Temporarily Disable Slow Rules**
   ```sql
   UPDATE business_rules
   SET enabled = false
   WHERE rule_code IN ('SLOW_RULE_1', 'SLOW_RULE_2');
   ```

3. **Increase Cache TTL**
   ```yaml
   data-quality:
     rules-engine:
       cache-ttl: 1800  # 30 minutes
   ```

4. **Reduce Logging**
   ```yaml
   data-quality:
     rules-engine:
       logging:
         log-executions: false
   ```

5. **Restart Application**
   ```bash
   systemctl restart regtech-app
   ```

6. **Monitor Performance**
   ```sql
   SELECT AVG(execution_duration_ms)
   FROM rule_execution_log
   WHERE executed_at >= NOW() - INTERVAL '15 minutes';
   ```

---

### Emergency 3: High Violation Rate

**Symptoms:**
- Sudden spike in violations
- Unexpected validation failures
- Business complaints

**Immediate Actions:**

1. **Identify Affected Rules**
   ```sql
   SELECT 
       rule_code,
       COUNT(*) as violation_count
   FROM rule_violations
   WHERE detected_at >= NOW() - INTERVAL '1 hour'
   GROUP BY rule_code
   ORDER BY violation_count DESC
   LIMIT 5;
   ```

2. **Check Recent Rule Changes**
   ```sql
   -- Check if parameters were recently updated
   SELECT 
       br.rule_code,
       rp.parameter_name,
       rp.parameter_value,
       rp.updated_at
   FROM rule_parameters rp
   JOIN business_rules br ON rp.rule_id = br.id
   WHERE rp.updated_at >= NOW() - INTERVAL '24 hours';
   ```

3. **Review Sample Violations**
   ```sql
   SELECT 
       exposure_id,
       rule_code,
       violation_message,
       field_name
   FROM rule_violations
   WHERE rule_code = 'PROBLEMATIC_RULE'
     AND detected_at >= NOW() - INTERVAL '1 hour'
   LIMIT 10;
   ```

4. **If Parameter Issue:**
   ```sql
   -- Rollback parameter to previous value
   UPDATE rule_parameters
   SET parameter_value = 'PREVIOUS_VALUE'
   WHERE rule_id = (SELECT id FROM business_rules WHERE rule_code = 'RULE_CODE')
     AND parameter_name = 'PARAMETER_NAME';
   ```

5. **If Rule Issue:**
   ```sql
   -- Temporarily disable problematic rule
   UPDATE business_rules
   SET enabled = false
   WHERE rule_code = 'PROBLEMATIC_RULE';
   ```

6. **Notify Stakeholders**
   - Send email to business users
   - Explain issue and mitigation
   - Provide timeline for resolution

---

### Emergency 4: Database Connection Loss

**Symptoms:**
- Error: "Unable to connect to database"
- Validation failing
- Application errors

**Immediate Actions:**

1. **Check Database Status**
   ```bash
   systemctl status postgresql
   ```

2. **Check Network Connectivity**
   ```bash
   ping database-host
   telnet database-host 5432
   ```

3. **Check Connection Pool**
   ```bash
   grep "connection pool" /var/log/regtech/application.log | tail -20
   ```

4. **If Database Down:**
   ```bash
   # Restart database
   systemctl restart postgresql
   ```

5. **If Connection Pool Exhausted:**
   ```yaml
   # Increase pool size
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
   ```

6. **Restart Application**
   ```bash
   systemctl restart regtech-app
   ```

7. **Verify Recovery**
   ```bash
   grep "Rules Engine initialized" /var/log/regtech/application.log | tail -1
   ```

---

## Monitoring and Alerts

### Key Metrics to Monitor

1. **Rule Execution Time**
   - Threshold: > 100ms per rule
   - Alert: Email to ops team

2. **Total Validation Time**
   - Threshold: > 5000ms per batch
   - Alert: Email to ops team

3. **Cache Hit Rate**
   - Threshold: < 90%
   - Alert: Warning in logs

4. **Violation Rate**
   - Threshold: > 20% increase from baseline
   - Alert: Email to business team

5. **Rule Execution Failures**
   - Threshold: > 5 failures per hour
   - Alert: Email to ops team

### Monitoring Queries

**Daily Summary:**
```sql
SELECT 
    COUNT(DISTINCT batch_id) as batches,
    COUNT(*) as total_executions,
    AVG(execution_duration_ms) as avg_duration,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failures
FROM rule_execution_log
WHERE executed_at >= CURRENT_DATE;
```

**Violation Trends:**
```sql
SELECT 
    DATE(detected_at) as date,
    rule_code,
    COUNT(*) as violation_count
FROM rule_violations
WHERE detected_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(detected_at), rule_code
ORDER BY date DESC, violation_count DESC;
```

**Performance Trends:**
```sql
SELECT 
    DATE(executed_at) as date,
    AVG(execution_duration_ms) as avg_duration,
    MAX(execution_duration_ms) as max_duration
FROM rule_execution_log
WHERE executed_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(executed_at)
ORDER BY date DESC;
```

---

## Performance Tuning

### Tuning Cache Settings

**Problem:** Frequent database queries

**Solution:**
```yaml
data-quality:
  rules-engine:
    cache-enabled: true
    cache-ttl: 600  # Increase from 300 to 600
```

**Impact:** Reduces database load, improves performance

---

### Tuning Logging

**Problem:** High log volume

**Solution:**
```yaml
data-quality:
  rules-engine:
    logging:
      log-executions: false  # Disable verbose logging
      log-violations: true   # Keep violation logs
      log-summary: true      # Keep summary logs
```

**Impact:** Reduces log volume by 80-90%

---

### Tuning Database Queries

**Problem:** Slow rule loading

**Solution:**
```sql
-- Add index on enabled column
CREATE INDEX idx_business_rules_enabled ON business_rules(enabled);

-- Add index on rule_code
CREATE INDEX idx_business_rules_code ON business_rules(rule_code);

-- Add index on violation timestamps
CREATE INDEX idx_rule_violations_detected_at ON rule_violations(detected_at);
```

**Impact:** Faster rule loading and query performance

---

## Troubleshooting Guide

### Issue: Rule Not Executing

**Check:**
1. Is rule enabled? `SELECT enabled FROM business_rules WHERE rule_code = 'RULE_CODE';`
2. Is cache stale? Wait for TTL or restart application
3. Is expression valid? Check for syntax errors
4. Are parameters correct? Verify parameter values

---

### Issue: Incorrect Violations

**Check:**
1. Review rule expression
2. Check parameter values
3. Verify field mappings
4. Test with sample data

---

### Issue: Missing Violations

**Check:**
1. Is rule enabled?
2. Is exemption active?
3. Is expression too permissive?
4. Check execution logs for errors

---

## Contact Information

**Development Team:** dev-team@example.com  
**Operations Team:** ops-team@example.com  
**Business Team:** business-team@example.com  

**On-Call:** +1-555-0100  
**Escalation:** manager@example.com  

---

*Last Updated: November 26, 2025*  
*Version: 2.0*  
*Requirement: 11.7*
