# Report Generation Module - Operational Runbook

## Overview

This runbook provides step-by-step procedures for common operational scenarios, troubleshooting, and recovery procedures for the Report Generation Module. It is designed for operations teams managing production systems.

## Quick Reference

### Critical Contacts

- **Operations Team**: #regtech-ops (Slack), ops@example.com
- **Development Team**: #regtech-dev (Slack), dev@example.com
- **On-Call Engineer**: PagerDuty rotation
- **Database Admin**: dba@example.com
- **AWS Support**: aws-support@example.com

### Key Endpoints

- **Health Check**: `http://host:8080/actuator/health`
- **Readiness**: `http://host:8080/actuator/health/readiness`
- **Metrics**: `http://host:8080/actuator/metrics`
- **Logs**: `/var/log/regtech/report-generation.log`

### Service Control

```bash
# Check status
sudo systemctl status regtech-report-generation

# Start service
sudo systemctl start regtech-report-generation

# Stop service
sudo systemctl stop regtech-report-generation

# Restart service
sudo systemctl restart regtech-report-generation

# View logs
sudo journalctl -u regtech-report-generation -f
```

## Common Failure Scenarios

### Scenario 1: Reports Not Generating

**Symptoms**:
- No reports generated for recent batches
- Events arriving but no report creation
- Health check shows UP but no activity

**Diagnosis Steps**:

1. Check if both events are arriving:

```bash
# Check application logs for event reception
sudo journalctl -u regtech-report-generation | grep "BatchCalculationCompletedEvent\|BatchQualityCompletedEvent"

# Expected: Both event types should appear for each batch
```

2. Check event tracker state:
```bash
# Query health endpoint for event tracker details
curl http://localhost:8080/actuator/health | jq '.components.eventTracker'

# Expected output:
# {
#   "status": "UP",
#   "details": {
#     "pendingEvents": 5,
#     "oldestEventAge": "PT2M"
#   }
# }
```

3. Check for stale events:
```bash
# Look for events older than 24 hours
sudo journalctl -u regtech-report-generation | grep "Event is stale"
```

4. Check database for existing reports:
```sql
-- Connect to database
psql -h db-host -U regtech_app -d regtech_production

-- Check recent reports
SELECT batch_id, status, generated_at, failure_reason 
FROM generated_reports 
ORDER BY generated_at DESC 
LIMIT 10;
```

**Resolution**:

**If only one event type is arriving**:
- Check upstream modules (Risk Calculation or Data Quality)
- Verify event publishing is working
- Check network connectivity between modules


**If events are stale (>24 hours)**:
```bash
# Events are automatically cleaned up
# Check if upstream modules are processing batches
# Verify batch processing is not stuck

# Manually trigger cleanup if needed (restart service)
sudo systemctl restart regtech-report-generation
```

**If idempotency is preventing generation**:
```sql
-- Check if report already exists with COMPLETED status
SELECT * FROM generated_reports WHERE batch_id = 'BATCH_ID_HERE';

-- If report should be regenerated, update status
UPDATE generated_reports 
SET status = 'FAILED' 
WHERE batch_id = 'BATCH_ID_HERE' AND status = 'COMPLETED';
```

**If async executor is full**:
```bash
# Check executor queue size
curl http://localhost:8080/actuator/metrics/report.async.executor.queue.size | jq

# If queue is full (100/100), restart service
sudo systemctl restart regtech-report-generation
```

### Scenario 2: S3 Upload Failures

**Symptoms**:
- Reports marked as FAILED with S3-related errors
- Circuit breaker OPEN state
- Deferred uploads accumulating

**Diagnosis Steps**:

1. Check circuit breaker state:
```bash
# Check circuit breaker metric
curl http://localhost:8080/actuator/metrics/report.circuit.breaker.state | jq

# 0 = CLOSED (normal)
# 1 = OPEN (blocking requests)
# 2 = HALF_OPEN (testing)
```

2. Test S3 connectivity:
```bash
# Test S3 access from application server
aws s3 ls s3://risk-analysis-production/ --profile regtech

# Test upload
echo "test" > /tmp/test.txt
aws s3 cp /tmp/test.txt s3://risk-analysis-production/test.txt
```


3. Check deferred uploads:
```bash
# Check deferred uploads directory
ls -lh /tmp/deferred-reports/

# Check deferred upload count metric
curl http://localhost:8080/actuator/metrics/report.deferred.uploads.count | jq
```

4. Check application logs for S3 errors:
```bash
sudo journalctl -u regtech-report-generation | grep "S3\|AmazonS3Exception"
```

**Resolution**:

**If S3 is temporarily unavailable**:
- Circuit breaker will open automatically after 10 failures
- Reports will be saved to local filesystem (`/tmp/deferred-reports/`)
- Wait for S3 to recover (circuit breaker will test after 5 minutes)
- Deferred uploads will retry automatically

**If S3 permissions are incorrect**:
```bash
# Verify IAM role/credentials
aws sts get-caller-identity

# Check bucket policy
aws s3api get-bucket-policy --bucket risk-analysis-production

# Fix permissions if needed (see DEPLOYMENT_GUIDE.md)
```

**If circuit breaker is stuck OPEN**:
```bash
# Wait for automatic recovery (5 minutes)
# Or restart service to reset circuit breaker
sudo systemctl restart regtech-report-generation
```

**Manual deferred upload processing**:
```bash
# List deferred files
ls -lh /tmp/deferred-reports/

# Manually upload to S3
for file in /tmp/deferred-reports/*.html; do
  aws s3 cp "$file" s3://risk-analysis-production/reports/html/
done

for file in /tmp/deferred-reports/*.xml; do
  aws s3 cp "$file" s3://risk-analysis-production/reports/xbrl/
done

# Update database records with S3 URIs
# (Contact development team for SQL script)
```


### Scenario 3: Database Connection Failures

**Symptoms**:
- Health check shows DOWN for database
- Connection timeout errors in logs
- Reports failing with database errors

**Diagnosis Steps**:

1. Check database connectivity:
```bash
# Test connection from application server
psql -h db-host -U regtech_app -d regtech_production -c "SELECT 1;"
```

2. Check connection pool:
```bash
# Check active connections
curl http://localhost:8080/actuator/metrics/report.database.pool.active | jq

# Check idle connections
curl http://localhost:8080/actuator/metrics/report.database.pool.idle | jq
```

3. Check database server:
```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Check database logs
sudo tail -f /var/log/postgresql/postgresql-15-main.log
```

4. Check for connection leaks:
```sql
-- Connect to database
psql -h db-host -U postgres -d regtech_production

-- Check active connections
SELECT count(*), state 
FROM pg_stat_activity 
WHERE datname = 'regtech_production' 
GROUP BY state;

-- Check long-running queries
SELECT pid, now() - query_start AS duration, query 
FROM pg_stat_activity 
WHERE state = 'active' AND now() - query_start > interval '5 minutes';
```

**Resolution**:

**If database is down**:
```bash
# Restart PostgreSQL
sudo systemctl restart postgresql

# Verify database is up
psql -h db-host -U regtech_app -d regtech_production -c "SELECT 1;"

# Restart application
sudo systemctl restart regtech-report-generation
```


**If connection pool is exhausted**:
```bash
# Restart application to reset pool
sudo systemctl restart regtech-report-generation

# If problem persists, increase pool size
# Edit /etc/regtech/report-generation.env
# Add: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30
# Restart service
```

**If there are connection leaks**:
```sql
-- Kill long-running queries
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state = 'active' 
  AND now() - query_start > interval '10 minutes'
  AND datname = 'regtech_production';
```

### Scenario 4: XBRL Validation Failures

**Symptoms**:
- Reports marked as PARTIAL
- XBRL validation errors in logs
- HTML generated but XBRL missing

**Diagnosis Steps**:

1. Check for validation errors:
```bash
# Search logs for XBRL validation errors
sudo journalctl -u regtech-report-generation | grep "XBRL validation\|XSD validation"
```

2. Check partial reports:
```sql
-- Query partial reports
SELECT batch_id, bank_id, reporting_date, failure_reason 
FROM generated_reports 
WHERE status = 'PARTIAL' 
ORDER BY generated_at DESC 
LIMIT 10;
```

3. Check for missing LEI codes:
```bash
# Search for LEI-related errors
sudo journalctl -u regtech-report-generation | grep "LEI\|Legal Entity Identifier"
```

**Resolution**:

**If LEI codes are missing**:
- XBRL generator uses "CONCAT" identifier type as fallback
- This is expected behavior for counterparties without LEI
- Report is marked as PARTIAL but is still valid
- No action needed unless regulatory requirement changes


**If XSD schema is missing**:
```bash
# Verify schema file exists
ls -lh /opt/regtech/report-generation/BOOT-INF/classes/xbrl/eba-corep-schema.xsd

# If missing, redeploy application
```

**If validation errors are data-related**:
- Review specific validation errors in logs
- Contact data quality team to fix source data
- Regenerate report after data is corrected

**Manual XBRL regeneration**:
```sql
-- Mark report as FAILED to allow regeneration
UPDATE generated_reports 
SET status = 'FAILED' 
WHERE batch_id = 'BATCH_ID_HERE';

-- Republish events to trigger regeneration
-- (Contact development team for event republishing procedure)
```

### Scenario 5: High Memory Usage / Out of Memory

**Symptoms**:
- Application crashes with OutOfMemoryError
- Slow performance
- GC overhead limit exceeded

**Diagnosis Steps**:

1. Check memory usage:
```bash
# Check JVM memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
curl http://localhost:8080/actuator/metrics/jvm.memory.max | jq

# Check system memory
free -h
```

2. Check for memory leaks:
```bash
# Generate heap dump
sudo -u regtech jmap -dump:live,format=b,file=/tmp/heap-dump.hprof $(pgrep -f regtech-report-generation)

# Analyze with Eclipse MAT or VisualVM
```

3. Check GC activity:
```bash
# Check GC metrics
curl http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
```

**Resolution**:

**If heap size is too small**:
```bash
# Edit /etc/regtech/report-generation.env
# Increase heap size
JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Restart service
sudo systemctl restart regtech-report-generation
```


**If there's a memory leak**:
- Analyze heap dump to identify leak source
- Contact development team with heap dump analysis
- Restart service as temporary mitigation
- Apply hotfix or upgrade to fixed version

**Emergency restart**:
```bash
# Force restart if service is unresponsive
sudo systemctl kill -s SIGKILL regtech-report-generation
sudo systemctl start regtech-report-generation
```

### Scenario 6: Slow Report Generation

**Symptoms**:
- P95 duration > 10 seconds
- Reports taking longer than expected
- Timeout errors

**Diagnosis Steps**:

1. Check performance metrics:
```bash
# Check overall duration
curl http://localhost:8080/actuator/metrics/report.generation.comprehensive.duration | jq

# Check component durations
curl http://localhost:8080/actuator/metrics/report.generation.html.duration | jq
curl http://localhost:8080/actuator/metrics/report.generation.xbrl.duration | jq
curl http://localhost:8080/actuator/metrics/report.data.fetch.duration | jq
curl http://localhost:8080/actuator/metrics/report.s3.upload.duration | jq
```

2. Check database performance:
```sql
-- Check slow queries
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
WHERE mean_exec_time > 1000 
ORDER BY mean_exec_time DESC 
LIMIT 10;
```

3. Check S3 latency:
```bash
# Test S3 download speed
time aws s3 cp s3://risk-analysis-production/calculated/calc_batch_test.json /tmp/
```

4. Check CPU and I/O:
```bash
# Check CPU usage
top -b -n 1 | grep java

# Check I/O wait
iostat -x 1 5
```

**Resolution**:

**If database is slow**:
```sql
-- Rebuild indexes
REINDEX TABLE generated_reports;

-- Update statistics
ANALYZE generated_reports;

-- Check for missing indexes
-- (Contact DBA team)
```


**If S3 is slow**:
- Check S3 service health dashboard
- Consider using S3 Transfer Acceleration
- Verify network connectivity and bandwidth

**If CPU is bottleneck**:
```bash
# Increase async executor pool size
# Edit /etc/regtech/report-generation.env
REPORT_GENERATION_ASYNC_MAX_POOL_SIZE=16

# Restart service
sudo systemctl restart regtech-report-generation
```

**If template rendering is slow**:
- Verify Thymeleaf template caching is enabled in production
- Check for large data sets in reports
- Contact development team for optimization

## Circuit Breaker Management

### Understanding Circuit Breaker States

**CLOSED** (Normal Operation):
- All S3 operations proceed normally
- Failures are counted
- Transitions to OPEN after 10 consecutive failures OR 50% failure rate over 5 minutes

**OPEN** (Blocking Requests):
- All S3 operations are blocked immediately
- Requests fail fast without attempting S3
- Reports saved to local filesystem
- Transitions to HALF_OPEN after 5 minutes

**HALF_OPEN** (Testing):
- Allows 1 test S3 operation
- If successful: transitions to CLOSED
- If fails: transitions back to OPEN for another 5 minutes

### Monitoring Circuit Breaker

```bash
# Check current state
curl http://localhost:8080/actuator/metrics/report.circuit.breaker.state | jq

# Check state transition events
sudo journalctl -u regtech-report-generation | grep "Circuit breaker"

# Check deferred uploads (accumulated during OPEN state)
curl http://localhost:8080/actuator/metrics/report.deferred.uploads.count | jq
```

### Manual Circuit Breaker Reset

**Option 1: Wait for automatic recovery** (Recommended)
- Circuit breaker will test S3 after 5 minutes
- If S3 is healthy, circuit closes automatically
- No manual intervention needed

**Option 2: Restart service** (If urgent)
```bash
# Restart service to reset circuit breaker
sudo systemctl restart regtech-report-generation

# Verify circuit breaker is CLOSED
curl http://localhost:8080/actuator/metrics/report.circuit.breaker.state | jq
```


### Processing Deferred Uploads

When circuit breaker is OPEN, reports are saved locally. After S3 recovers:

1. **Automatic Processing** (Preferred):
```bash
# Deferred uploads are retried automatically by scheduled job
# Check logs for retry attempts
sudo journalctl -u regtech-report-generation | grep "deferred upload"

# Monitor deferred upload count (should decrease)
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/report.deferred.uploads.count | jq'
```

2. **Manual Processing** (If automatic fails):
```bash
# List deferred files
ls -lh /tmp/deferred-reports/

# Upload HTML files
for file in /tmp/deferred-reports/*.html; do
  filename=$(basename "$file")
  aws s3 cp "$file" "s3://risk-analysis-production/reports/html/$filename"
  echo "Uploaded: $filename"
done

# Upload XBRL files
for file in /tmp/deferred-reports/*.xml; do
  filename=$(basename "$file")
  aws s3 cp "$file" "s3://risk-analysis-production/reports/xbrl/$filename"
  echo "Uploaded: $filename"
done

# Update database records
# (Contact development team for SQL script to update S3 URIs and presigned URLs)

# Clean up local files after successful upload
rm -f /tmp/deferred-reports/*
```

## Orphaned File Cleanup

### Understanding Orphaned Files

Orphaned files occur when:
- S3 upload succeeds but database insert fails
- Application crashes after upload but before database commit
- Manual uploads without database records

### Scheduled Cleanup Job

The module includes a scheduled job that runs daily:
- Identifies S3 files without corresponding database records
- Deletes files older than 7 days
- Logs cleanup actions

### Manual Orphaned File Identification

```bash
# List all S3 files
aws s3 ls s3://risk-analysis-production/reports/html/ --recursive > /tmp/s3-files.txt
aws s3 ls s3://risk-analysis-production/reports/xbrl/ --recursive >> /tmp/s3-files.txt

# Extract file names
cat /tmp/s3-files.txt | awk '{print $4}' > /tmp/s3-filenames.txt
```


```sql
-- Query database for all report S3 URIs
\copy (SELECT html_s3_uri, xbrl_s3_uri FROM generated_reports) TO '/tmp/db-files.txt' CSV;

-- Compare lists to find orphaned files
-- (Use diff or custom script)
```

### Manual Orphaned File Cleanup

```bash
# Delete specific orphaned file
aws s3 rm s3://risk-analysis-production/reports/html/orphaned-file.html

# Delete all orphaned files older than 7 days
# (Use custom script with date comparison)

# Example script:
#!/bin/bash
CUTOFF_DATE=$(date -d '7 days ago' +%s)

aws s3 ls s3://risk-analysis-production/reports/html/ --recursive | while read -r line; do
  FILE_DATE=$(echo "$line" | awk '{print $1" "$2}')
  FILE_PATH=$(echo "$line" | awk '{print $4}')
  FILE_TIMESTAMP=$(date -d "$FILE_DATE" +%s)
  
  if [ $FILE_TIMESTAMP -lt $CUTOFF_DATE ]; then
    # Check if file exists in database
    # If not, delete
    echo "Would delete: $FILE_PATH"
    # aws s3 rm "s3://risk-analysis-production/$FILE_PATH"
  fi
done
```

### Reconciliation Job

If database insert failed but S3 upload succeeded:

```sql
-- Check report_metadata_failures table for failed inserts
SELECT * FROM report_metadata_failures 
ORDER BY failed_at DESC 
LIMIT 10;

-- Attempt to insert into generated_reports
INSERT INTO generated_reports (
  report_id, batch_id, bank_id, reporting_date, report_type,
  html_s3_uri, xbrl_s3_uri, status, generated_at
)
SELECT 
  report_id, batch_id, bank_id, reporting_date, 'COMPREHENSIVE',
  html_s3_uri, xbrl_s3_uri, 'COMPLETED', failed_at
FROM report_metadata_failures
WHERE batch_id = 'BATCH_ID_HERE'
ON CONFLICT (batch_id) DO NOTHING;

-- Delete from failures table after successful insert
DELETE FROM report_metadata_failures 
WHERE batch_id = 'BATCH_ID_HERE';
```

## Recovery Procedures

### Procedure 1: Recover from Complete Service Failure

**Scenario**: Service crashed and won't start

**Steps**:

1. Check service status:
```bash
sudo systemctl status regtech-report-generation
```

2. Check logs for error:
```bash
sudo journalctl -u regtech-report-generation -n 100 --no-pager
```


3. Common startup failures:

**Port already in use**:
```bash
# Find process using port 8080
sudo lsof -i :8080

# Kill process if needed
sudo kill -9 <PID>

# Start service
sudo systemctl start regtech-report-generation
```

**Database connection failure**:
```bash
# Test database connectivity
psql -h db-host -U regtech_app -d regtech_production -c "SELECT 1;"

# If database is down, start it
sudo systemctl start postgresql

# Restart application
sudo systemctl start regtech-report-generation
```

**Missing environment variables**:
```bash
# Verify environment file exists
cat /etc/regtech/report-generation.env

# Check for required variables
grep -E "SPRING_DATASOURCE_URL|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY" /etc/regtech/report-generation.env

# Add missing variables and restart
sudo systemctl restart regtech-report-generation
```

**Corrupted JAR file**:
```bash
# Verify JAR integrity
jar tf /opt/regtech/report-generation/regtech-report-generation.jar | head

# If corrupted, redeploy from backup
sudo cp /opt/regtech/report-generation/regtech-report-generation.jar.backup \
  /opt/regtech/report-generation/regtech-report-generation.jar

# Restart service
sudo systemctl start regtech-report-generation
```

### Procedure 2: Recover from Data Corruption

**Scenario**: Database records are corrupted or inconsistent

**Steps**:

1. Identify corrupted records:
```sql
-- Find reports with missing required fields
SELECT * FROM generated_reports 
WHERE html_s3_uri IS NULL OR xbrl_s3_uri IS NULL;

-- Find reports with invalid status
SELECT * FROM generated_reports 
WHERE status NOT IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL', 'FAILED');
```

2. Backup corrupted data:
```sql
-- Create backup table
CREATE TABLE generated_reports_backup AS 
SELECT * FROM generated_reports 
WHERE batch_id IN ('BATCH_1', 'BATCH_2');
```


3. Fix or delete corrupted records:
```sql
-- Option 1: Fix records if S3 files exist
UPDATE generated_reports 
SET status = 'COMPLETED', 
    html_s3_uri = 's3://risk-analysis-production/reports/html/...',
    xbrl_s3_uri = 's3://risk-analysis-production/reports/xbrl/...'
WHERE batch_id = 'BATCH_ID_HERE';

-- Option 2: Delete corrupted records to allow regeneration
DELETE FROM generated_reports 
WHERE batch_id = 'BATCH_ID_HERE' AND status = 'CORRUPTED';
```

4. Trigger regeneration:
```bash
# Republish events to trigger regeneration
# (Contact development team for event republishing procedure)
```

### Procedure 3: Recover from S3 Data Loss

**Scenario**: S3 files deleted or bucket corrupted

**Steps**:

1. Check S3 versioning:
```bash
# List versions of deleted file
aws s3api list-object-versions \
  --bucket risk-analysis-production \
  --prefix reports/html/Comprehensive_Risk_Analysis_BANK123_2024-01-15.html

# Restore previous version
aws s3api copy-object \
  --bucket risk-analysis-production \
  --copy-source risk-analysis-production/reports/html/file.html?versionId=VERSION_ID \
  --key reports/html/file.html
```

2. If versioning doesn't help, regenerate reports:
```sql
-- Mark reports as FAILED to allow regeneration
UPDATE generated_reports 
SET status = 'FAILED', 
    failure_reason = 'S3 files lost - regeneration required'
WHERE html_s3_uri LIKE '%MISSING_FILE%';
```

3. Republish events to trigger regeneration
4. Verify new files are uploaded to S3

### Procedure 4: Emergency Rollback

**Scenario**: New deployment causing issues, need to rollback

**Steps**:

1. Stop current version:
```bash
sudo systemctl stop regtech-report-generation
```

2. Restore previous version:
```bash
# Restore JAR
sudo cp /opt/regtech/report-generation/regtech-report-generation.jar.backup \
  /opt/regtech/report-generation/regtech-report-generation.jar

# Restore configuration if changed
sudo cp /etc/regtech/report-generation.env.backup \
  /etc/regtech/report-generation.env
```


3. Rollback database if schema changed:
```bash
# Using Flyway
flyway -url=jdbc:postgresql://db-host:5432/regtech_production \
  -user=regtech_app \
  -password=PASSWORD \
  undo

# Verify schema version
flyway info
```

4. Start service:
```bash
sudo systemctl start regtech-report-generation
```

5. Verify rollback:
```bash
# Check health
curl http://localhost:8080/actuator/health

# Check version
curl http://localhost:8080/actuator/info | jq '.build.version'

# Monitor logs
sudo journalctl -u regtech-report-generation -f
```

## Maintenance Procedures

### Procedure 1: Planned Service Restart

**When**: During maintenance window or after configuration changes

**Steps**:

1. Notify stakeholders:
```bash
# Send notification to #regtech-ops channel
# "Report Generation Module will be restarted at HH:MM for maintenance"
```

2. Check current state:
```bash
# Check for pending events
curl http://localhost:8080/actuator/health | jq '.components.eventTracker'

# Check async queue
curl http://localhost:8080/actuator/metrics/report.async.executor.queue.size | jq

# Wait for queue to drain if possible
```

3. Graceful shutdown:
```bash
# Stop service (allows in-flight requests to complete)
sudo systemctl stop regtech-report-generation

# Wait for shutdown (max 30 seconds)
sleep 30
```

4. Restart service:
```bash
sudo systemctl start regtech-report-generation
```

5. Verify restart:
```bash
# Check health
curl http://localhost:8080/actuator/health

# Check logs for startup
sudo journalctl -u regtech-report-generation -n 50
```

### Procedure 2: Database Maintenance

**When**: Database backup, index rebuild, or schema changes

**Steps**:

1. Put application in maintenance mode:
```bash
# Stop service to prevent new database connections
sudo systemctl stop regtech-report-generation
```


2. Perform database maintenance:
```sql
-- Backup database
pg_dump -h db-host -U regtech_app regtech_production > /backup/regtech_$(date +%Y%m%d).sql

-- Rebuild indexes
REINDEX TABLE generated_reports;
REINDEX TABLE report_metadata_failures;

-- Update statistics
ANALYZE generated_reports;
ANALYZE report_metadata_failures;

-- Vacuum
VACUUM ANALYZE generated_reports;
```

3. Restart application:
```bash
sudo systemctl start regtech-report-generation
```

4. Verify database connectivity:
```bash
curl http://localhost:8080/actuator/health | jq '.components.db'
```

### Procedure 3: S3 Bucket Maintenance

**When**: Bucket policy changes, lifecycle rule updates, or cleanup

**Steps**:

1. Verify current bucket configuration:
```bash
# Check bucket policy
aws s3api get-bucket-policy --bucket risk-analysis-production

# Check lifecycle rules
aws s3api get-bucket-lifecycle-configuration --bucket risk-analysis-production

# Check versioning
aws s3api get-bucket-versioning --bucket risk-analysis-production
```

2. Apply changes:
```bash
# Update bucket policy
aws s3api put-bucket-policy --bucket risk-analysis-production --policy file://new-policy.json

# Update lifecycle rules
aws s3api put-bucket-lifecycle-configuration \
  --bucket risk-analysis-production \
  --lifecycle-configuration file://new-lifecycle.json
```

3. Test S3 access:
```bash
# Test from application server
aws s3 ls s3://risk-analysis-production/

# Test upload
echo "test" > /tmp/test.txt
aws s3 cp /tmp/test.txt s3://risk-analysis-production/test.txt
aws s3 rm s3://risk-analysis-production/test.txt
```

4. Monitor application:
```bash
# Check for S3 errors
sudo journalctl -u regtech-report-generation | grep "S3\|AmazonS3Exception"

# Check circuit breaker state
curl http://localhost:8080/actuator/metrics/report.circuit.breaker.state | jq
```

## Monitoring and Alerting

### Key Metrics to Monitor

**Performance Metrics**:
- `report.generation.comprehensive.duration` - Target: P95 < 10s
- `report.generation.html.duration` - Target: P95 < 3s
- `report.generation.xbrl.duration` - Target: P95 < 1.5s

**Success Rate Metrics**:
- `report.generation.comprehensive.success` - Target: > 95%
- `report.generation.comprehensive.failure` - Target: < 5%
- `report.generation.partial` - Target: < 10%


**Resource Metrics**:
- `report.database.pool.active` - Alert if > 80% of max
- `report.async.executor.queue.size` - Alert if > 80 (80% of capacity)
- `report.deferred.uploads.count` - Alert if > 50
- `report.circuit.breaker.state` - Alert if OPEN (value = 1)

### Alert Configuration Examples

**Prometheus Alert Rules**:

```yaml
groups:
  - name: report_generation_alerts
    interval: 30s
    rules:
      - alert: ReportGenerationHighFailureRate
        expr: rate(report_generation_comprehensive_failure_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High report generation failure rate"
          description: "Failure rate is {{ $value }} (>10%) over last 5 minutes"

      - alert: ReportGenerationSlowPerformance
        expr: histogram_quantile(0.95, report_generation_comprehensive_duration_seconds) > 10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Report generation is slow"
          description: "P95 duration is {{ $value }}s (>10s)"

      - alert: ReportGenerationCircuitBreakerOpen
        expr: report_circuit_breaker_state == 1
        for: 2m
        labels:
          severity: high
        annotations:
          summary: "S3 circuit breaker is OPEN"
          description: "Circuit breaker has been OPEN for 2+ minutes"

      - alert: ReportGenerationDatabasePoolExhausted
        expr: report_database_pool_active / report_database_pool_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Pool usage is {{ $value | humanizePercentage }}"

      - alert: ReportGenerationDeferredUploadsAccumulating
        expr: report_deferred_uploads_count > 50
        for: 15m
        labels:
          severity: high
        annotations:
          summary: "Deferred uploads accumulating"
          description: "{{ $value }} deferred uploads pending"

      - alert: ReportGenerationAsyncQueueFull
        expr: report_async_executor_queue_size > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Async executor queue is filling up"
          description: "Queue size is {{ $value }}/100"
```

### Dashboard Panels

**Recommended Grafana Panels**:

1. **Report Generation Rate** (Graph)
   - Metric: `rate(report_generation_comprehensive_success_total[5m])`
   - Unit: reports/second

2. **Success vs Failure Rate** (Graph)
   - Success: `rate(report_generation_comprehensive_success_total[5m])`
   - Failure: `rate(report_generation_comprehensive_failure_total[5m])`

3. **Generation Duration** (Graph)
   - P50: `histogram_quantile(0.50, report_generation_comprehensive_duration_seconds)`
   - P95: `histogram_quantile(0.95, report_generation_comprehensive_duration_seconds)`
   - P99: `histogram_quantile(0.99, report_generation_comprehensive_duration_seconds)`

4. **Circuit Breaker State** (Stat)
   - Metric: `report_circuit_breaker_state`
   - Mapping: 0=CLOSED, 1=OPEN, 2=HALF_OPEN

5. **Database Pool Usage** (Gauge)
   - Active: `report_database_pool_active`
   - Max: `report_database_pool_max`

6. **Async Executor Queue** (Gauge)
   - Queue Size: `report_async_executor_queue_size`
   - Capacity: 100

## Troubleshooting Checklist

### Quick Diagnostic Checklist

When investigating issues, run through this checklist:

- [ ] Check service status: `sudo systemctl status regtech-report-generation`
- [ ] Check health endpoint: `curl http://localhost:8080/actuator/health`
- [ ] Check recent logs: `sudo journalctl -u regtech-report-generation -n 100`
- [ ] Check database connectivity: `psql -h db-host -U regtech_app -d regtech_production -c "SELECT 1;"`
- [ ] Check S3 connectivity: `aws s3 ls s3://risk-analysis-production/`
- [ ] Check circuit breaker state: `curl http://localhost:8080/actuator/metrics/report.circuit.breaker.state`
- [ ] Check async queue size: `curl http://localhost:8080/actuator/metrics/report.async.executor.queue.size`
- [ ] Check event tracker: `curl http://localhost:8080/actuator/health | jq '.components.eventTracker'`
- [ ] Check recent reports: `SELECT * FROM generated_reports ORDER BY generated_at DESC LIMIT 5;`
- [ ] Check for errors: `sudo journalctl -u regtech-report-generation | grep ERROR`

### Log Analysis Commands

```bash
# Count errors by type
sudo journalctl -u regtech-report-generation --since "1 hour ago" | grep ERROR | awk '{print $NF}' | sort | uniq -c | sort -rn

# Find slow operations
sudo journalctl -u regtech-report-generation --since "1 hour ago" | grep "duration" | awk '{print $NF}' | sort -rn | head -20

# Check event processing
sudo journalctl -u regtech-report-generation --since "1 hour ago" | grep "BatchCalculationCompletedEvent\|BatchQualityCompletedEvent"

# Check S3 operations
sudo journalctl -u regtech-report-generation --since "1 hour ago" | grep "S3\|upload\|download"

# Check database operations
sudo journalctl -u regtech-report-generation --since "1 hour ago" | grep "database\|SQL\|JPA"
```

## Escalation Procedures

### Level 1: Operations Team

**Responsibilities**:
- Monitor health checks and alerts
- Restart services
- Check logs for obvious errors
- Verify infrastructure (database, S3, network)

**Escalate to Level 2 if**:
- Service won't start after restart
- Persistent errors in logs
- Performance degradation continues
- Data corruption suspected

### Level 2: Development Team

**Responsibilities**:
- Analyze application logs
- Debug code issues
- Apply hotfixes
- Investigate data issues

**Escalate to Level 3 if**:
- Architecture changes needed
- Integration issues with other modules
- Capacity planning required
- Design flaws identified

### Level 3: Architecture Team

**Responsibilities**:
- Design changes
- Integration architecture
- Capacity planning
- Long-term solutions

## Contact Information

**Operations Team**:
- Slack: #regtech-ops
- Email: ops@example.com
- PagerDuty: Report Generation On-Call

**Development Team**:
- Slack: #regtech-dev
- Email: dev@example.com
- Lead: John Doe (john.doe@example.com)

**Database Team**:
- Slack: #database-team
- Email: dba@example.com

**AWS Support**:
- Email: aws-support@example.com
- Phone: +1-XXX-XXX-XXXX

## Appendix

### Useful SQL Queries

```sql
-- Report generation statistics (last 24 hours)
SELECT 
  status,
  COUNT(*) as count,
  AVG(EXTRACT(EPOCH FROM (completed_at - generated_at))) as avg_duration_seconds
FROM generated_reports
WHERE generated_at > NOW() - INTERVAL '24 hours'
GROUP BY status;

-- Failed reports with reasons
SELECT batch_id, bank_id, reporting_date, failure_reason, generated_at
FROM generated_reports
WHERE status = 'FAILED'
ORDER BY generated_at DESC
LIMIT 20;

-- Partial reports
SELECT batch_id, bank_id, reporting_date, failure_reason
FROM generated_reports
WHERE status = 'PARTIAL'
ORDER BY generated_at DESC
LIMIT 20;

-- Reports by bank
SELECT bank_id, COUNT(*) as report_count, 
       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
       SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed
FROM generated_reports
WHERE generated_at > NOW() - INTERVAL '7 days'
GROUP BY bank_id
ORDER BY report_count DESC;

-- Slow reports (>10 seconds)
SELECT batch_id, bank_id, 
       EXTRACT(EPOCH FROM (completed_at - generated_at)) as duration_seconds
FROM generated_reports
WHERE completed_at IS NOT NULL
  AND EXTRACT(EPOCH FROM (completed_at - generated_at)) > 10
ORDER BY duration_seconds DESC
LIMIT 20;
```

### Useful AWS CLI Commands

```bash
# List recent reports
aws s3 ls s3://risk-analysis-production/reports/html/ --recursive --human-readable | tail -20

# Check bucket size
aws s3 ls s3://risk-analysis-production/reports/ --recursive --summarize

# Download report for inspection
aws s3 cp s3://risk-analysis-production/reports/html/Comprehensive_Risk_Analysis_BANK123_2024-01-15.html /tmp/

# Check S3 bucket policy
aws s3api get-bucket-policy --bucket risk-analysis-production | jq

# Check S3 bucket encryption
aws s3api get-bucket-encryption --bucket risk-analysis-production

# List object versions (for recovery)
aws s3api list-object-versions --bucket risk-analysis-production --prefix reports/html/
```

### Configuration File Locations

- **Application JAR**: `/opt/regtech/report-generation/regtech-report-generation.jar`
- **Environment Variables**: `/etc/regtech/report-generation.env`
- **Systemd Service**: `/etc/systemd/system/regtech-report-generation.service`
- **Application Logs**: `/var/log/regtech/report-generation.log`
- **System Logs**: `journalctl -u regtech-report-generation`
- **Deferred Uploads**: `/tmp/deferred-reports/`
- **XBRL Schema**: `BOOT-INF/classes/xbrl/eba-corep-schema.xsd` (inside JAR)
- **HTML Templates**: `BOOT-INF/classes/templates/reports/` (inside JAR)

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-15  
**Maintained By**: RegTech Operations Team
