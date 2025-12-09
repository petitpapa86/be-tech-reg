# Report Generation Module - Deployment Guide

## Overview

This guide provides step-by-step instructions for deploying the Report Generation Module to production environments. The module requires PostgreSQL database, AWS S3 storage, and proper network connectivity to upstream modules (Risk Calculation and Data Quality).

## Prerequisites

### Infrastructure Requirements

- **Database**: PostgreSQL 15+ with minimum 2GB RAM, 20GB storage
- **AWS S3**: Bucket with versioning enabled and server-side encryption
- **Compute**: Minimum 4 CPU cores, 8GB RAM for application server
- **Network**: Connectivity to Risk Calculation and Data Quality modules
- **Java**: OpenJDK 17 or later

### Access Requirements

- AWS IAM credentials with S3 read/write permissions
- Database credentials with DDL and DML permissions
- Network access to upstream event publishers
- Access to artifact repository (Maven/Nexus)

## S3 Bucket Setup

### Step 1: Create S3 Bucket

```bash
# Create bucket with versioning and encryption
aws s3api create-bucket \
  --bucket risk-analysis-production \
  --region eu-central-1 \
  --create-bucket-configuration LocationConstraint=eu-central-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket risk-analysis-production \
  --versioning-configuration Status=Enabled

# Enable server-side encryption (AES-256)
aws s3api put-bucket-encryption \
  --bucket risk-analysis-production \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

### Step 2: Create Folder Structure

```bash
# Create folder structure for reports and source data
aws s3api put-object \
  --bucket risk-analysis-production \
  --key reports/html/

aws s3api put-object \
  --bucket risk-analysis-production \
  --key reports/xbrl/

aws s3api put-object \
  --bucket risk-analysis-production \
  --key calculated/

aws s3api put-object \
  --bucket risk-analysis-production \
  --key quality/
```

### Step 3: Configure Bucket Policy

Create `bucket-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowReportGenerationRead",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT_ID:role/ReportGenerationRole"
      },
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion"
      ],
      "Resource": [
        "arn:aws:s3:::risk-analysis-production/calculated/*",
        "arn:aws:s3:::risk-analysis-production/quality/*"
      ]
    },
    {
      "Sid": "AllowReportGenerationWrite",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT_ID:role/ReportGenerationRole"
      },
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::risk-analysis-production/reports/*"
      ]
    },
    {
      "Sid": "AllowPresignedUrlGeneration",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT_ID:role/ReportGenerationRole"
      },
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::risk-analysis-production/reports/*"
      ]
    }
  ]
}
```

Apply the policy:

```bash
aws s3api put-bucket-policy \
  --bucket risk-analysis-production \
  --policy file://bucket-policy.json
```

### Step 4: Configure Lifecycle Rules

Create `lifecycle-policy.json`:

```json
{
  "Rules": [
    {
      "Id": "DeleteOldReports",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "reports/"
      },
      "Expiration": {
        "Days": 90
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 30
      }
    },
    {
      "Id": "TransitionOldReportsToGlacier",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "reports/"
      },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

Apply lifecycle policy:

```bash
aws s3api put-bucket-lifecycle-configuration \
  --bucket risk-analysis-production \
  --lifecycle-configuration file://lifecycle-policy.json
```

## Database Setup

### Step 1: Create Database

```sql
-- Connect as superuser
CREATE DATABASE regtech_production
  WITH ENCODING 'UTF8'
  LC_COLLATE = 'en_US.UTF-8'
  LC_CTYPE = 'en_US.UTF-8'
  TEMPLATE template0;

-- Create application user
CREATE USER regtech_app WITH PASSWORD 'SECURE_PASSWORD_HERE';

-- Grant privileges
GRANT CONNECT ON DATABASE regtech_production TO regtech_app;
GRANT USAGE ON SCHEMA public TO regtech_app;
GRANT CREATE ON SCHEMA public TO regtech_app;
```

### Step 2: Run Database Migrations

The module uses Flyway for database migrations. Migrations run automatically on application startup.

**Migration Files Location**: `regtech-report-generation/infrastructure/src/main/resources/db/migration/`

**Key Migrations**:
- `V1__Create_generated_reports_table.sql` - Main reports table
- `V2__Create_report_metadata_failures_table.sql` - Fallback metadata table
- `V3__Add_indexes.sql` - Performance indexes

**Manual Migration** (if needed):

```bash
# Using Flyway CLI
flyway -url=jdbc:postgresql://localhost:5432/regtech_production \
  -user=regtech_app \
  -password=SECURE_PASSWORD_HERE \
  -locations=filesystem:./regtech-report-generation/infrastructure/src/main/resources/db/migration \
  migrate
```

### Step 3: Verify Database Schema

```sql
-- Connect to database
\c regtech_production

-- Verify tables exist
\dt

-- Expected tables:
-- - generated_reports
-- - report_metadata_failures
-- - flyway_schema_history

-- Verify indexes
\di

-- Expected indexes:
-- - idx_generated_reports_batch_id
-- - idx_generated_reports_bank_id
-- - idx_generated_reports_status
-- - idx_generated_reports_reporting_date
-- - unique_batch_id (UNIQUE constraint)
```

### Step 4: Configure Connection Pool

Recommended connection pool settings for production:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

## Application Deployment

### Step 1: Build Application

```bash
# Clone repository
git clone https://github.com/your-org/regtech-platform.git
cd regtech-platform

# Checkout release tag
git checkout v1.0.0

# Build with Maven
mvn clean package -DskipTests -P production

# Artifact location
ls -lh regtech-report-generation/target/regtech-report-generation-*.jar
```

### Step 2: Configure Environment Variables

Create `/etc/regtech/report-generation.env`:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/regtech_production
SPRING_DATASOURCE_USERNAME=regtech_app
SPRING_DATASOURCE_PASSWORD=SECURE_PASSWORD_HERE

# AWS Configuration
AWS_REGION=eu-central-1
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# S3 Configuration
REPORT_GENERATION_S3_BUCKET_NAME=risk-analysis-production
REPORT_GENERATION_S3_HTML_PATH=reports/html/
REPORT_GENERATION_S3_XBRL_PATH=reports/xbrl/

# File Path Configuration
REPORT_GENERATION_FILE_PATHS_CALCULATION_PATTERN=calculated/calc_batch_{batchId}.json
REPORT_GENERATION_FILE_PATHS_QUALITY_PATTERN=quality/quality_batch_{batchId}.json

# Async Executor Configuration
REPORT_GENERATION_ASYNC_CORE_POOL_SIZE=4
REPORT_GENERATION_ASYNC_MAX_POOL_SIZE=8
REPORT_GENERATION_ASYNC_QUEUE_CAPACITY=100

# Circuit Breaker Configuration
REPORT_GENERATION_CIRCUIT_BREAKER_FAILURE_THRESHOLD=10
REPORT_GENERATION_CIRCUIT_BREAKER_WAIT_DURATION=5m
REPORT_GENERATION_CIRCUIT_BREAKER_PERMITTED_CALLS_HALF_OPEN=1

# Logging Configuration
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_BCBS239_REGTECH=INFO
LOGGING_FILE_NAME=/var/log/regtech/report-generation.log

# JVM Options
JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Step 3: Create Systemd Service

Create `/etc/systemd/system/regtech-report-generation.service`:

```ini
[Unit]
Description=RegTech Report Generation Module
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=regtech
Group=regtech
WorkingDirectory=/opt/regtech/report-generation

# Environment file
EnvironmentFile=/etc/regtech/report-generation.env

# Java command
ExecStart=/usr/bin/java \
  $JAVA_OPTS \
  -jar /opt/regtech/report-generation/regtech-report-generation.jar

# Restart policy
Restart=always
RestartSec=10
StartLimitInterval=5min
StartLimitBurst=3

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=regtech-report-generation

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/regtech /tmp/deferred-reports

[Install]
WantedBy=multi-user.target
```

### Step 4: Deploy Application

```bash
# Create application directory
sudo mkdir -p /opt/regtech/report-generation
sudo chown regtech:regtech /opt/regtech/report-generation

# Copy JAR file
sudo cp regtech-report-generation/target/regtech-report-generation-*.jar \
  /opt/regtech/report-generation/regtech-report-generation.jar

# Create log directory
sudo mkdir -p /var/log/regtech
sudo chown regtech:regtech /var/log/regtech

# Create deferred uploads directory
sudo mkdir -p /tmp/deferred-reports
sudo chown regtech:regtech /tmp/deferred-reports

# Reload systemd
sudo systemctl daemon-reload

# Enable service
sudo systemctl enable regtech-report-generation

# Start service
sudo systemctl start regtech-report-generation

# Check status
sudo systemctl status regtech-report-generation
```

### Step 5: Verify Deployment

```bash
# Check application logs
sudo journalctl -u regtech-report-generation -f

# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "s3": {"status": "UP"},
#     "eventTracker": {"status": "UP"},
#     "asyncExecutor": {"status": "UP"}
#   }
# }

# Check readiness endpoint
curl http://localhost:8080/actuator/health/readiness

# Check metrics
curl http://localhost:8080/actuator/metrics
```

## Health Check Endpoints

### Liveness Probe

**Endpoint**: `GET /actuator/health/liveness`

**Purpose**: Indicates if the application is running

**Response Codes**:
- `200 OK`: Application is alive
- `503 Service Unavailable`: Application is dead (should be restarted)

**Kubernetes Configuration**:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Readiness Probe

**Endpoint**: `GET /actuator/health/readiness`

**Purpose**: Indicates if the application is ready to accept traffic

**Response Codes**:
- `200 OK`: Application is ready
- `503 Service Unavailable`: Application is not ready (should not receive traffic)

**Checks**:
- Database connectivity
- S3 accessibility
- Event tracker state
- Async executor capacity

**Kubernetes Configuration**:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### Health Check Details

**Endpoint**: `GET /actuator/health`

**Response Example**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "s3": {
      "status": "UP",
      "details": {
        "bucket": "risk-analysis-production",
        "region": "eu-central-1"
      }
    },
    "eventTracker": {
      "status": "UP",
      "details": {
        "pendingEvents": 5,
        "oldestEventAge": "PT2M"
      }
    },
    "asyncExecutor": {
      "status": "UP",
      "details": {
        "queueSize": 12,
        "queueCapacity": 100,
        "activeThreads": 3,
        "maxPoolSize": 8
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 53687091200,
        "threshold": 10485760
      }
    }
  }
}
```

## Monitoring Setup

### Prometheus Integration

Add Prometheus scrape configuration:

```yaml
scrape_configs:
  - job_name: 'regtech-report-generation'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['report-generation-host:8080']
        labels:
          application: 'regtech-report-generation'
          environment: 'production'
```

### Key Metrics to Monitor

**Performance Metrics**:
- `report_generation_comprehensive_duration_seconds` (histogram)
- `report_generation_html_duration_seconds` (histogram)
- `report_generation_xbrl_duration_seconds` (histogram)
- `report_data_fetch_duration_seconds` (histogram)

**Success/Failure Metrics**:
- `report_generation_comprehensive_success_total` (counter)
- `report_generation_comprehensive_failure_total` (counter)
- `report_generation_partial_total` (counter)

**Resource Metrics**:
- `report_database_pool_active` (gauge)
- `report_async_executor_queue_size` (gauge)
- `report_circuit_breaker_state` (gauge)

### Grafana Dashboard

Import the provided Grafana dashboard:

```bash
# Dashboard JSON location
regtech-report-generation/monitoring/grafana-dashboard.json
```

**Dashboard Panels**:
- Report generation rate (reports/minute)
- Success vs failure rate
- P95 generation duration
- Circuit breaker state
- Database connection pool usage
- Async executor queue size
- Error distribution by type

## Rollback Procedure

### Step 1: Stop Current Version

```bash
sudo systemctl stop regtech-report-generation
```

### Step 2: Restore Previous Version

```bash
# Restore previous JAR
sudo cp /opt/regtech/report-generation/regtech-report-generation.jar.backup \
  /opt/regtech/report-generation/regtech-report-generation.jar
```

### Step 3: Rollback Database (if needed)

```bash
# Using Flyway
flyway -url=jdbc:postgresql://localhost:5432/regtech_production \
  -user=regtech_app \
  -password=SECURE_PASSWORD_HERE \
  -locations=filesystem:./regtech-report-generation/infrastructure/src/main/resources/db/migration \
  undo
```

### Step 4: Restart Service

```bash
sudo systemctl start regtech-report-generation
sudo systemctl status regtech-report-generation
```

### Step 5: Verify Rollback

```bash
# Check health
curl http://localhost:8080/actuator/health

# Check application version
curl http://localhost:8080/actuator/info

# Monitor logs
sudo journalctl -u regtech-report-generation -f
```

## Security Considerations

### Secrets Management

**Recommended**: Use AWS Secrets Manager or HashiCorp Vault

```bash
# Store database password in AWS Secrets Manager
aws secretsmanager create-secret \
  --name regtech/report-generation/db-password \
  --secret-string "SECURE_PASSWORD_HERE"

# Retrieve in application
aws secretsmanager get-secret-value \
  --secret-id regtech/report-generation/db-password \
  --query SecretString \
  --output text
```

### IAM Role Configuration

Create IAM role with minimal permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::risk-analysis-production/calculated/*",
        "arn:aws:s3:::risk-analysis-production/quality/*",
        "arn:aws:s3:::risk-analysis-production/reports/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::risk-analysis-production"
      ]
    }
  ]
}
```

### Network Security

**Firewall Rules**:
- Allow inbound: 8080 (HTTP), 8443 (HTTPS)
- Allow outbound: 5432 (PostgreSQL), 443 (S3)
- Restrict access to health endpoints from monitoring systems only

**TLS Configuration**:
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: /etc/regtech/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

## Troubleshooting Deployment

### Issue: Application fails to start

**Check**:
```bash
# View logs
sudo journalctl -u regtech-report-generation -n 100

# Common causes:
# - Database connection failure
# - Missing environment variables
# - Port already in use
# - Insufficient memory
```

### Issue: Database migration fails

**Check**:
```bash
# Verify database connectivity
psql -h db-host -U regtech_app -d regtech_production

# Check Flyway schema history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

# Repair Flyway if needed
flyway repair
```

### Issue: S3 access denied

**Check**:
```bash
# Test S3 access
aws s3 ls s3://risk-analysis-production/ --profile regtech

# Verify IAM permissions
aws iam get-role-policy --role-name ReportGenerationRole --policy-name S3Access
```

### Issue: Health check fails

**Check**:
```bash
# Detailed health check
curl http://localhost:8080/actuator/health | jq

# Check specific component
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/s3
```

## Post-Deployment Validation

### Smoke Tests

```bash
# 1. Health check
curl http://localhost:8080/actuator/health

# 2. Metrics endpoint
curl http://localhost:8080/actuator/metrics

# 3. Check database connectivity
psql -h db-host -U regtech_app -d regtech_production -c "SELECT COUNT(*) FROM generated_reports;"

# 4. Check S3 connectivity
aws s3 ls s3://risk-analysis-production/reports/

# 5. Trigger test report generation (if test events available)
# Monitor logs for successful processing
```

### Performance Validation

```bash
# Monitor key metrics for 15 minutes
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/report.generation.comprehensive.duration | jq'

# Expected P95 < 10 seconds
# Expected success rate > 95%
```

## Support and Escalation

**Level 1 Support**: Operations team
- Health check failures
- Service restarts
- Log analysis

**Level 2 Support**: Development team
- Application errors
- Performance issues
- Configuration problems

**Level 3 Support**: Architecture team
- Design issues
- Integration problems
- Capacity planning

**Contact**:
- Slack: #regtech-support
- Email: regtech-ops@example.com
- On-call: PagerDuty rotation
