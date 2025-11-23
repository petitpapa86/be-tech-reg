# RegTech Application Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the RegTech application in different environments. The application uses a modular configuration structure with environment-specific profiles and supports multiple deployment targets.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Local Development Deployment](#local-development-deployment)
4. [Production Deployment](#production-deployment)
5. [Cloud Platform Deployments](#cloud-platform-deployments)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Health Checks](#monitoring-and-health-checks)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

- **Java**: JDK 17 or higher
- **Maven**: 3.8 or higher
- **PostgreSQL**: 14 or higher
- **Docker**: 20.10 or higher (optional, for containerized deployment)
- **AWS CLI**: 2.x (for S3 integration)

### Required Accounts

- **AWS Account**: For S3 storage (production)
- **Stripe Account**: For billing functionality
- **Database**: PostgreSQL instance

### Build the Application

```bash
# Clone repository
git clone <repository-url>
cd regtech

# Build all modules
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

---

## Environment Configuration

### Configuration Profiles

The application supports multiple Spring profiles:

| Profile | Purpose | Storage | Thread Pools | Logging |
|---------|---------|---------|--------------|---------|
| `development` | Local development | Local filesystem | Small (2-4) | DEBUG |
| `production` | Production deployment | AWS S3 | Large (10-20) | INFO |
| `gcp` | Google Cloud Platform | AWS S3 | Large (10-20) | JSON structured |

### Profile Selection

Choose a profile using one of these methods:

**1. Application Property (application.yml):**
```yaml
spring:
  profiles:
    active: development
```

**2. Environment Variable:**
```bash
export SPRING_PROFILES_ACTIVE=production
```

**3. Command-Line Argument:**
```bash
java -jar regtech-app.jar --spring.profiles.active=production
```

**4. JVM System Property:**
```bash
java -Dspring.profiles.active=production -jar regtech-app.jar
```

### Required Environment Variables

#### Database Configuration

```bash
# Development (optional, has defaults)
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# Production (required)
export DB_URL=jdbc:postgresql://prod-db-host:5432/regtech
export DB_USERNAME=regtech_prod
export DB_PASSWORD=<secure-password>
```

#### AWS/S3 Configuration

```bash
# Required for production (when storage.type=s3)
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>

# Optional: For LocalStack testing
export AWS_S3_ENDPOINT=http://localhost:4566
```

#### Security Configuration

```bash
# JWT Secret (required for production)
export JWT_SECRET=<secure-random-string-min-64-chars>

# OAuth2 (optional)
export GOOGLE_CLIENT_ID=<google-client-id>
export GOOGLE_CLIENT_SECRET=<google-client-secret>
export FACEBOOK_CLIENT_ID=<facebook-client-id>
export FACEBOOK_CLIENT_SECRET=<facebook-client-secret>
```

#### Billing Configuration

```bash
# Development
export STRIPE_TEST_API_KEY=<stripe-test-key>
export STRIPE_TEST_WEBHOOK_SECRET=<stripe-test-webhook-secret>

# Production
export STRIPE_LIVE_API_KEY=<stripe-live-key>
export STRIPE_LIVE_WEBHOOK_SECRET=<stripe-live-webhook-secret>
```

#### External Services

```bash
# Bank Registry Service
export BANK_REGISTRY_URL=http://bank-registry-service:8081
```


---

## Local Development Deployment

### Quick Start

1. **Start PostgreSQL Database**
   ```bash
   # Using Docker
   docker run -d \
     --name regtech-postgres \
     -e POSTGRES_DB=regtech \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     postgres:14
   ```

2. **Set Environment Variables (Optional)**
   ```bash
   # Create .env file (don't commit!)
   cat > .env << EOF
   SPRING_PROFILES_ACTIVE=development
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   EOF
   
   # Load environment variables
   export $(cat .env | xargs)
   ```

3. **Run Application**
   ```bash
   # Using Maven
   mvn spring-boot:run -pl regtech-app
   
   # Or using JAR
   java -jar regtech-app/target/regtech-app-1.0.0-SNAPSHOT.jar
   ```

4. **Verify Application**
   ```bash
   # Check health endpoint
   curl http://localhost:8080/api/health
   
   # Check actuator health
   curl http://localhost:8080/actuator/health
   ```

### Development Configuration

The `development` profile automatically configures:

- **Storage**: Local filesystem (`./data/`)
- **Thread Pools**: Small sizes (2-4 threads)
- **Logging**: DEBUG level for all modules
- **Database**: Local PostgreSQL with defaults
- **S3**: Disabled (uses local storage)
- **Scheduled Jobs**: Disabled
- **Currency Provider**: Mock implementation

### Local Testing with LocalStack

To test S3 integration locally:

1. **Start LocalStack**
   ```bash
   docker run -d \
     --name localstack \
     -p 4566:4566 \
     -e SERVICES=s3 \
     localstack/localstack
   ```

2. **Create S3 Buckets**
   ```bash
   aws --endpoint-url=http://localhost:4566 s3 mb s3://regtech-ingestion
   aws --endpoint-url=http://localhost:4566 s3 mb s3://regtech-data-quality
   aws --endpoint-url=http://localhost:4566 s3 mb s3://regtech-risk-calculations
   aws --endpoint-url=http://localhost:4566 s3 mb s3://risk-analysis-dev
   ```

3. **Configure Application**
   ```bash
   export AWS_S3_ENDPOINT=http://localhost:4566
   export AWS_ACCESS_KEY_ID=test
   export AWS_SECRET_ACCESS_KEY=test
   ```

4. **Override Storage Type**
   ```bash
   # Override to use S3 instead of local
   export INGESTION_STORAGE_TYPE=s3
   export DATA_QUALITY_STORAGE_TYPE=s3
   export RISK_CALCULATION_STORAGE_TYPE=s3
   ```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Database provisioned and accessible
- [ ] S3 buckets created with appropriate permissions
- [ ] AWS credentials configured
- [ ] JWT secret generated (min 64 characters)
- [ ] Stripe production keys obtained
- [ ] Environment variables documented
- [ ] Monitoring and alerting configured
- [ ] Backup strategy in place
- [ ] Rollback plan documented

### Production Environment Setup

#### 1. Database Setup

```bash
# Create production database
createdb -h prod-db-host -U postgres regtech

# Create application user
psql -h prod-db-host -U postgres -d regtech << EOF
CREATE USER regtech_prod WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE regtech TO regtech_prod;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO regtech_prod;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO regtech_prod;
EOF

# Run migrations (if using Flyway)
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db-host:5432/regtech \
  -Dflyway.user=regtech_prod \
  -Dflyway.password=<secure-password>
```

#### 2. S3 Bucket Setup

```bash
# Create S3 buckets
aws s3 mb s3://regtech-ingestion --region us-east-1
aws s3 mb s3://regtech-data-quality --region us-east-1
aws s3 mb s3://regtech-risk-calculations --region us-east-1
aws s3 mb s3://risk-analysis-production --region us-east-1

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket regtech-ingestion \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Repeat for other buckets

# Set lifecycle policies (optional)
aws s3api put-bucket-lifecycle-configuration \
  --bucket regtech-ingestion \
  --lifecycle-configuration file://lifecycle-policy.json
```

#### 3. IAM User Setup

```bash
# Create IAM user for application
aws iam create-user --user-name regtech-app

# Create access key
aws iam create-access-key --user-name regtech-app

# Attach S3 policy
aws iam attach-user-policy \
  --user-name regtech-app \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

**IAM Policy (Least Privilege):**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::regtech-*/*",
        "arn:aws:s3:::regtech-*",
        "arn:aws:s3:::risk-analysis-production/*",
        "arn:aws:s3:::risk-analysis-production"
      ]
    }
  ]
}
```

#### 4. Environment Variables Configuration

Create a secure environment file:

```bash
# /etc/regtech/environment
SPRING_PROFILES_ACTIVE=production

# Database
DB_URL=jdbc:postgresql://prod-db-host:5432/regtech
DB_USERNAME=regtech_prod
DB_PASSWORD=<secure-password>

# AWS
AWS_ACCESS_KEY_ID=<access-key>
AWS_SECRET_ACCESS_KEY=<secret-key>

# Security
JWT_SECRET=<64-char-random-string>

# Billing
STRIPE_LIVE_API_KEY=<stripe-live-key>
STRIPE_LIVE_WEBHOOK_SECRET=<stripe-webhook-secret>

# External Services
BANK_REGISTRY_URL=http://bank-registry:8081
```

**Secure the file:**
```bash
chmod 600 /etc/regtech/environment
chown regtech:regtech /etc/regtech/environment
```


#### 5. Application Deployment

**Option A: Systemd Service**

Create systemd service file:

```bash
# /etc/systemd/system/regtech.service
[Unit]
Description=RegTech Application
After=network.target postgresql.service

[Service]
Type=simple
User=regtech
Group=regtech
WorkingDirectory=/opt/regtech
EnvironmentFile=/etc/regtech/environment
ExecStart=/usr/bin/java \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar /opt/regtech/regtech-app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=regtech

[Install]
WantedBy=multi-user.target
```

Deploy and start:

```bash
# Copy JAR to deployment directory
sudo cp regtech-app/target/regtech-app-1.0.0-SNAPSHOT.jar /opt/regtech/regtech-app.jar

# Reload systemd
sudo systemctl daemon-reload

# Enable service
sudo systemctl enable regtech

# Start service
sudo systemctl start regtech

# Check status
sudo systemctl status regtech

# View logs
sudo journalctl -u regtech -f
```

**Option B: Docker Container**

Create Dockerfile:

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# Create app user
RUN addgroup -S regtech && adduser -S regtech -G regtech

# Set working directory
WORKDIR /app

# Copy JAR
COPY regtech-app/target/regtech-app-1.0.0-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R regtech:regtech /app

# Switch to app user
USER regtech

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-Xms2g", "-Xmx4g", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", "app.jar"]
```

Build and run:

```bash
# Build image
docker build -t regtech-app:1.0.0 .

# Run container
docker run -d \
  --name regtech-app \
  --env-file /etc/regtech/environment \
  -p 8080:8080 \
  --restart unless-stopped \
  regtech-app:1.0.0

# View logs
docker logs -f regtech-app

# Check health
docker exec regtech-app wget -qO- http://localhost:8080/actuator/health
```

**Option C: Docker Compose**

Create docker-compose.yml:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: regtech
      POSTGRES_USER: regtech_prod
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U regtech_prod"]
      interval: 10s
      timeout: 5s
      retries: 5

  regtech-app:
    image: regtech-app:1.0.0
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: production
      DB_URL: jdbc:postgresql://postgres:5432/regtech
      DB_USERNAME: regtech_prod
      DB_PASSWORD: ${DB_PASSWORD}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      JWT_SECRET: ${JWT_SECRET}
      STRIPE_LIVE_API_KEY: ${STRIPE_LIVE_API_KEY}
      STRIPE_LIVE_WEBHOOK_SECRET: ${STRIPE_LIVE_WEBHOOK_SECRET}
    ports:
      - "8080:8080"
    volumes:
      - app-logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres-data:
  app-logs:
```

Deploy:

```bash
# Load environment variables
export $(cat /etc/regtech/environment | xargs)

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Check status
docker-compose ps
```

#### 6. Post-Deployment Verification

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check module health endpoints
curl http://localhost:8080/api/v1/ingestion/health
curl http://localhost:8080/api/v1/data-quality/health
curl http://localhost:8080/api/v1/risk-calculation/health
curl http://localhost:8080/api/v1/report-generation/health

# Check metrics
curl http://localhost:8080/actuator/metrics

# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Test S3 connectivity (check logs)
tail -f /var/log/regtech/application.log | grep S3
```


---

## Cloud Platform Deployments

### Google Cloud Platform (GCP)

#### Configuration

Use the `production,gcp` profiles for GCP deployment:

```bash
export SPRING_PROFILES_ACTIVE=production,gcp
```

This enables:
- Production settings (S3, optimized pools)
- JSON structured logging for Google Cloud Logging
- Increased verbosity for event processing

#### Cloud Run Deployment

1. **Build Container**
   ```bash
   gcloud builds submit --tag gcr.io/PROJECT_ID/regtech-app
   ```

2. **Deploy to Cloud Run**
   ```bash
   gcloud run deploy regtech-app \
     --image gcr.io/PROJECT_ID/regtech-app \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars SPRING_PROFILES_ACTIVE=production,gcp \
     --set-env-vars DB_URL=jdbc:postgresql://CLOUD_SQL_IP:5432/regtech \
     --set-secrets DB_PASSWORD=regtech-db-password:latest \
     --set-secrets AWS_ACCESS_KEY_ID=aws-access-key:latest \
     --set-secrets AWS_SECRET_ACCESS_KEY=aws-secret-key:latest \
     --set-secrets JWT_SECRET=jwt-secret:latest \
     --memory 4Gi \
     --cpu 2 \
     --max-instances 10
   ```

3. **Configure Cloud SQL**
   ```bash
   gcloud sql instances create regtech-db \
     --database-version=POSTGRES_14 \
     --tier=db-custom-2-7680 \
     --region=us-central1
   
   gcloud sql databases create regtech --instance=regtech-db
   
   gcloud sql users create regtech_prod \
     --instance=regtech-db \
     --password=<secure-password>
   ```

#### GKE Deployment

1. **Create Kubernetes Deployment**
   ```yaml
   # deployment.yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: regtech-app
   spec:
     replicas: 3
     selector:
       matchLabels:
         app: regtech-app
     template:
       metadata:
         labels:
           app: regtech-app
       spec:
         containers:
         - name: regtech-app
           image: gcr.io/PROJECT_ID/regtech-app:latest
           ports:
           - containerPort: 8080
           env:
           - name: SPRING_PROFILES_ACTIVE
             value: "production,gcp"
           - name: DB_URL
             valueFrom:
               secretKeyRef:
                 name: regtech-secrets
                 key: db-url
           - name: DB_USERNAME
             valueFrom:
               secretKeyRef:
                 name: regtech-secrets
                 key: db-username
           - name: DB_PASSWORD
             valueFrom:
               secretKeyRef:
                 name: regtech-secrets
                 key: db-password
           resources:
             requests:
               memory: "2Gi"
               cpu: "1000m"
             limits:
               memory: "4Gi"
               cpu: "2000m"
           livenessProbe:
             httpGet:
               path: /actuator/health
               port: 8080
             initialDelaySeconds: 60
             periodSeconds: 10
           readinessProbe:
             httpGet:
               path: /actuator/health
               port: 8080
             initialDelaySeconds: 30
             periodSeconds: 5
   ```

2. **Create Service**
   ```yaml
   # service.yaml
   apiVersion: v1
   kind: Service
   metadata:
     name: regtech-app
   spec:
     type: LoadBalancer
     ports:
     - port: 80
       targetPort: 8080
     selector:
       app: regtech-app
   ```

3. **Deploy**
   ```bash
   kubectl apply -f deployment.yaml
   kubectl apply -f service.yaml
   ```

### AWS Elastic Beanstalk

1. **Create Application**
   ```bash
   eb init -p "Corretto 17 running on 64bit Amazon Linux 2" regtech-app
   ```

2. **Configure Environment**
   ```bash
   # .ebextensions/environment.config
   option_settings:
     aws:elasticbeanstalk:application:environment:
       SPRING_PROFILES_ACTIVE: production
       DB_URL: jdbc:postgresql://rds-endpoint:5432/regtech
     aws:elasticbeanstalk:environment:proxy:
       ProxyServer: nginx
     aws:autoscaling:launchconfiguration:
       InstanceType: t3.large
       IamInstanceProfile: aws-elasticbeanstalk-ec2-role
   ```

3. **Deploy**
   ```bash
   eb create regtech-prod-env
   eb deploy
   ```

### Hetzner Cloud Deployment

Hetzner is a cost-effective European cloud provider suitable for RegTech deployments.

#### 1. Create Server

```bash
# Create server via Hetzner Cloud Console or CLI
hcloud server create \
  --name regtech-prod \
  --type cx31 \
  --image ubuntu-22.04 \
  --ssh-key my-key \
  --location nbg1
```

#### 2. Initial Server Setup

```bash
# SSH into server
ssh root@<server-ip>

# Update system
apt update && apt upgrade -y

# Install Java
apt install -y openjdk-17-jre-headless

# Install PostgreSQL
apt install -y postgresql postgresql-contrib

# Create application user
useradd -m -s /bin/bash regtech
```

#### 3. Configure PostgreSQL

```bash
# Switch to postgres user
sudo -u postgres psql

# Create database and user
CREATE DATABASE regtech;
CREATE USER regtech_prod WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE regtech TO regtech_prod;
\q

# Configure PostgreSQL to accept connections
echo "host    regtech    regtech_prod    127.0.0.1/32    md5" >> /etc/postgresql/14/main/pg_hba.conf
systemctl restart postgresql
```

#### 4. Deploy Application

```bash
# Create application directory
mkdir -p /opt/regtech
chown regtech:regtech /opt/regtech

# Copy JAR (from local machine)
scp regtech-app/target/regtech-app-1.0.0-SNAPSHOT.jar root@<server-ip>:/opt/regtech/

# Create environment file
cat > /etc/regtech/environment << EOF
SPRING_PROFILES_ACTIVE=production
DB_URL=jdbc:postgresql://localhost:5432/regtech
DB_USERNAME=regtech_prod
DB_PASSWORD=<secure-password>
AWS_ACCESS_KEY_ID=<access-key>
AWS_SECRET_ACCESS_KEY=<secret-key>
JWT_SECRET=<jwt-secret>
STRIPE_LIVE_API_KEY=<stripe-key>
STRIPE_LIVE_WEBHOOK_SECRET=<webhook-secret>
EOF

chmod 600 /etc/regtech/environment
chown regtech:regtech /etc/regtech/environment
```

#### 5. Configure Systemd Service

```bash
# Create service file (same as production deployment above)
cat > /etc/systemd/system/regtech.service << EOF
[Unit]
Description=RegTech Application
After=network.target postgresql.service

[Service]
Type=simple
User=regtech
Group=regtech
WorkingDirectory=/opt/regtech
EnvironmentFile=/etc/regtech/environment
ExecStart=/usr/bin/java -Xms2g -Xmx4g -jar /opt/regtech/regtech-app-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# Start service
systemctl daemon-reload
systemctl enable regtech
systemctl start regtech
```

#### 6. Configure Nginx Reverse Proxy

```bash
# Install Nginx
apt install -y nginx

# Configure reverse proxy
cat > /etc/nginx/sites-available/regtech << EOF
server {
    listen 80;
    server_name regtech.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Enable site
ln -s /etc/nginx/sites-available/regtech /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx
```

#### 7. Configure SSL with Let's Encrypt

```bash
# Install Certbot
apt install -y certbot python3-certbot-nginx

# Obtain certificate
certbot --nginx -d regtech.example.com

# Auto-renewal is configured automatically
```

#### 8. Configure Firewall

```bash
# Install UFW
apt install -y ufw

# Configure firewall
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow http
ufw allow https
ufw enable
```


---

## Configuration Management

### Configuration Override Priority

Spring Boot loads configuration in the following order (later sources override earlier):

1. Default properties in JAR
2. `application.yml` in JAR
3. `application-{profile}.yml` in JAR
4. `application.yml` in external config directory
5. `application-{profile}.yml` in external config directory
6. Environment variables
7. Command-line arguments

### External Configuration Directory

Place configuration files in an external directory:

```bash
# Create config directory
mkdir -p /etc/regtech/config

# Place configuration files
cp application.yml /etc/regtech/config/
cp application-production.yml /etc/regtech/config/

# Run application with external config
java -jar regtech-app.jar --spring.config.location=file:/etc/regtech/config/
```

### Environment-Specific Overrides

Override specific properties without changing files:

```bash
# Override ingestion thread pool
export INGESTION_ASYNC_CORE_POOL_SIZE=20
export INGESTION_ASYNC_MAX_POOL_SIZE=40

# Override S3 bucket
export REPORT_GENERATION_S3_BUCKET=custom-bucket

# Override database URL
export DB_URL=jdbc:postgresql://custom-host:5432/regtech
```

### Secrets Management

#### Using Environment Variables

```bash
# Store secrets in secure location
cat > /etc/regtech/secrets << EOF
export JWT_SECRET=$(openssl rand -base64 64)
export DB_PASSWORD=$(openssl rand -base64 32)
export AWS_SECRET_ACCESS_KEY=<secret-key>
export STRIPE_LIVE_API_KEY=<stripe-key>
EOF

chmod 600 /etc/regtech/secrets

# Load secrets before starting application
source /etc/regtech/secrets
java -jar regtech-app.jar
```

#### Using AWS Secrets Manager

```bash
# Store secret
aws secretsmanager create-secret \
  --name regtech/db-password \
  --secret-string "<secure-password>"

# Retrieve secret in application startup script
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id regtech/db-password \
  --query SecretString \
  --output text)
```

#### Using HashiCorp Vault

```bash
# Store secret
vault kv put secret/regtech/db password="<secure-password>"

# Retrieve secret
export DB_PASSWORD=$(vault kv get -field=password secret/regtech/db)
```

### Configuration Validation

Validate configuration before deployment:

```bash
# Dry-run to check configuration
java -jar regtech-app.jar --spring.profiles.active=production --dry-run

# Check configuration properties
java -jar regtech-app.jar --spring.config.location=file:/etc/regtech/config/ \
  --debug | grep "ConfigurationProperties"

# Validate environment variables
./scripts/validate-env.sh
```

**Validation Script (validate-env.sh):**
```bash
#!/bin/bash

REQUIRED_VARS=(
  "SPRING_PROFILES_ACTIVE"
  "DB_URL"
  "DB_USERNAME"
  "DB_PASSWORD"
  "AWS_ACCESS_KEY_ID"
  "AWS_SECRET_ACCESS_KEY"
  "JWT_SECRET"
)

MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var}" ]; then
    MISSING_VARS+=("$var")
  fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
  echo "ERROR: Missing required environment variables:"
  printf '  - %s\n' "${MISSING_VARS[@]}"
  exit 1
fi

echo "✓ All required environment variables are set"
exit 0
```

---

## Monitoring and Health Checks

### Health Endpoints

The application exposes health endpoints for monitoring:

```bash
# Overall application health
curl http://localhost:8080/actuator/health

# Module-specific health
curl http://localhost:8080/api/v1/ingestion/health
curl http://localhost:8080/api/v1/data-quality/health
curl http://localhost:8080/api/v1/risk-calculation/health
curl http://localhost:8080/api/v1/report-generation/health
```

### Metrics Endpoints

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Logging

#### Log Locations

- **Systemd**: `journalctl -u regtech -f`
- **Docker**: `docker logs -f regtech-app`
- **File**: `/var/log/regtech/application.log` (if configured)

#### Log Levels

Adjust log levels without restart:

```bash
# Via actuator endpoint
curl -X POST http://localhost:8080/actuator/loggers/com.bcbs239.regtech \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# Via environment variable (requires restart)
export LOGGING_LEVEL_COM_BCBS239_REGTECH=DEBUG
```

### Monitoring Tools Integration

#### Prometheus

Add Prometheus endpoint:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Scrape configuration:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'regtech'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

#### Grafana

Import Spring Boot dashboard:
- Dashboard ID: 4701 (JVM Micrometer)
- Dashboard ID: 11378 (Spring Boot Statistics)

#### ELK Stack

Configure Logstash to parse application logs:

```ruby
# logstash.conf
input {
  file {
    path => "/var/log/regtech/application.log"
    start_position => "beginning"
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "regtech-%{+YYYY.MM.dd}"
  }
}
```


---

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
# Systemd
journalctl -u regtech -n 100 --no-pager

# Docker
docker logs regtech-app --tail 100

# File
tail -n 100 /var/log/regtech/application.log
```

**Common issues:**

1. **Database connection failure**
   ```
   Error: Could not connect to database
   Solution: Verify DB_URL, DB_USERNAME, DB_PASSWORD
   ```

2. **Missing environment variables**
   ```
   Error: Could not resolve placeholder 'JWT_SECRET'
   Solution: Set required environment variables
   ```

3. **Port already in use**
   ```
   Error: Port 8080 is already in use
   Solution: Change port or stop conflicting service
   ```

4. **Insufficient memory**
   ```
   Error: OutOfMemoryError
   Solution: Increase heap size: -Xmx4g
   ```

### S3 Connection Issues

**Test S3 connectivity:**
```bash
# Test AWS credentials
aws s3 ls s3://regtech-ingestion/

# Test from application server
curl -v https://s3.amazonaws.com

# Check application logs
grep "S3" /var/log/regtech/application.log
```

**Common issues:**

1. **Invalid credentials**
   ```
   Error: The AWS Access Key Id you provided does not exist
   Solution: Verify AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
   ```

2. **Bucket not found**
   ```
   Error: The specified bucket does not exist
   Solution: Create bucket or verify bucket name
   ```

3. **Permission denied**
   ```
   Error: Access Denied
   Solution: Update IAM policy to grant S3 permissions
   ```

### Performance Issues

**Check resource usage:**
```bash
# CPU and memory
top -p $(pgrep -f regtech-app)

# Disk I/O
iostat -x 1

# Network
netstat -an | grep 8080

# Thread count
ps -eLf | grep regtech-app | wc -l
```

**Tune JVM:**
```bash
# Increase heap size
-Xms4g -Xmx8g

# Use G1GC
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Enable GC logging
-Xlog:gc*:file=/var/log/regtech/gc.log:time,uptime:filecount=5,filesize=10M
```

**Tune thread pools:**
```bash
# Increase thread pool sizes
export INGESTION_ASYNC_CORE_POOL_SIZE=20
export INGESTION_ASYNC_MAX_POOL_SIZE=40
export RISK_CALCULATION_ASYNC_CORE_POOL_SIZE=15
export RISK_CALCULATION_ASYNC_MAX_POOL_SIZE=30
```

### Database Issues

**Check database connectivity:**
```bash
# Test connection
psql -h localhost -U regtech_prod -d regtech -c "SELECT 1"

# Check active connections
psql -h localhost -U regtech_prod -d regtech -c "SELECT count(*) FROM pg_stat_activity"

# Check slow queries
psql -h localhost -U regtech_prod -d regtech -c "SELECT query, query_start FROM pg_stat_activity WHERE state = 'active' AND query_start < now() - interval '1 minute'"
```

**Tune connection pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Circuit Breaker Issues

**Check circuit breaker status:**
```bash
# Via actuator
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Check logs
grep "CircuitBreaker" /var/log/regtech/application.log
```

**Reset circuit breaker:**
```bash
# Via actuator (if enabled)
curl -X POST http://localhost:8080/actuator/circuitbreakers/s3-upload/reset
```

---

## Backup and Recovery

### Database Backup

**Automated backup script:**
```bash
#!/bin/bash
# /usr/local/bin/backup-regtech-db.sh

BACKUP_DIR="/var/backups/regtech"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/regtech_$TIMESTAMP.sql.gz"

# Create backup directory
mkdir -p $BACKUP_DIR

# Perform backup
pg_dump -h localhost -U regtech_prod regtech | gzip > $BACKUP_FILE

# Keep only last 7 days
find $BACKUP_DIR -name "regtech_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
```

**Schedule with cron:**
```bash
# Daily backup at 2 AM
0 2 * * * /usr/local/bin/backup-regtech-db.sh
```

### S3 Backup

S3 data is automatically replicated by AWS. Enable versioning for additional protection:

```bash
aws s3api put-bucket-versioning \
  --bucket regtech-ingestion \
  --versioning-configuration Status=Enabled
```

### Application State Backup

Backup configuration and environment:

```bash
#!/bin/bash
# /usr/local/bin/backup-regtech-config.sh

BACKUP_DIR="/var/backups/regtech/config"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup configuration
tar -czf $BACKUP_DIR/config_$TIMESTAMP.tar.gz \
  /etc/regtech/ \
  /opt/regtech/regtech-app.jar

# Keep only last 30 days
find $BACKUP_DIR -name "config_*.tar.gz" -mtime +30 -delete
```

### Recovery Procedure

1. **Stop application**
   ```bash
   systemctl stop regtech
   ```

2. **Restore database**
   ```bash
   gunzip -c /var/backups/regtech/regtech_20240101_020000.sql.gz | \
     psql -h localhost -U regtech_prod regtech
   ```

3. **Restore configuration**
   ```bash
   tar -xzf /var/backups/regtech/config/config_20240101_020000.tar.gz -C /
   ```

4. **Start application**
   ```bash
   systemctl start regtech
   ```

5. **Verify recovery**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## Security Considerations

### SSL/TLS Configuration

Always use HTTPS in production:

```nginx
server {
    listen 443 ssl http2;
    server_name regtech.example.com;

    ssl_certificate /etc/letsencrypt/live/regtech.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/regtech.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Firewall Configuration

Restrict access to application:

```bash
# Allow only from specific IPs
ufw allow from 10.0.0.0/8 to any port 8080

# Or use Nginx as reverse proxy and block direct access
ufw deny 8080
```

### Secrets Rotation

Rotate secrets regularly:

```bash
# Generate new JWT secret
NEW_JWT_SECRET=$(openssl rand -base64 64)

# Update environment
sed -i "s/JWT_SECRET=.*/JWT_SECRET=$NEW_JWT_SECRET/" /etc/regtech/environment

# Restart application
systemctl restart regtech
```

### Audit Logging

Enable audit logging for security events:

```yaml
logging:
  level:
    com.bcbs239.regtech.iam.infrastructure.security: DEBUG
    com.bcbs239.regtech.iam.authorization: DEBUG
```

---

## Additional Resources

- [Configuration Reference](./CONFIGURATION_REFERENCE.md) - Complete property reference
- [Migration Guide](./CONFIGURATION_MIGRATION.md) - Migration from old structure
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/) - Official docs
- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/) - S3 integration
- [PostgreSQL Documentation](https://www.postgresql.org/docs/) - Database setup

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: RegTech Development Team
