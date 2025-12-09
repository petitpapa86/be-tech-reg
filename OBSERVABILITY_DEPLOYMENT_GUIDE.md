# Observability Stack Deployment Guide (Hetzner)

This guide provides step-by-step instructions for deploying the complete observability stack on Hetzner infrastructure.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Deployment Steps](#deployment-steps)
4. [Configuration](#configuration)
5. [Verification](#verification)
6. [Backup Strategy](#backup-strategy)
7. [Monitoring and Maintenance](#monitoring-and-maintenance)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Hetzner Server Requirements

**Minimum Configuration:**
- Server: CX31 (2 vCPU, 8GB RAM, 80GB SSD)
- Cost: ~€10/month
- Suitable for: Development and small production deployments

**Recommended Configuration:**
- Server: CX41 (4 vCPU, 16GB RAM, 160GB SSD)
- Cost: ~€15/month
- Suitable for: Production deployments with moderate traffic

**High-Volume Configuration:**
- Server: CX51 (8 vCPU, 32GB RAM, 240GB SSD)
- Additional Volume: 100GB SSD (~€5/month)
- Cost: ~€35/month
- Suitable for: High-traffic production deployments

### Software Requirements

- Docker Engine 24.0+
- Docker Compose 2.20+
- Git
- OpenSSL (for SSL certificate generation)

### Network Requirements

- Static IP address or domain name
- Firewall access for:
  - Port 3000 (Grafana)
  - Port 4317/4318 (OTLP receivers - internal only)
  - Port 22 (SSH - restricted to your IP)

## Infrastructure Setup

### 1. Create Hetzner Server

```bash
# Using Hetzner Cloud CLI (hcloud)
hcloud server create \
  --name observability-stack \
  --type cx41 \
  --image ubuntu-22.04 \
  --datacenter fsn1-dc14 \
  --ssh-key your-ssh-key

# Note the server IP address
export SERVER_IP=<your-server-ip>
```

### 2. Initial Server Configuration

```bash
# SSH into the server
ssh root@$SERVER_IP

# Update system packages
apt update && apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose
apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version

# Create observability user
useradd -m -s /bin/bash observability
usermod -aG docker observability

# Set up firewall
ufw allow 22/tcp
ufw allow 3000/tcp
ufw enable
```

### 3. Create Additional Storage Volume (Optional)

For high-volume deployments:

```bash
# Create volume via Hetzner Cloud Console or CLI
hcloud volume create \
  --name observability-data \
  --size 100 \
  --server observability-stack

# Mount volume
mkdir -p /mnt/observability-data
mount /dev/disk/by-id/scsi-0HC_Volume_* /mnt/observability-data

# Add to /etc/fstab for persistence
echo "/dev/disk/by-id/scsi-0HC_Volume_* /mnt/observability-data ext4 defaults 0 0" >> /etc/fstab
```

## Deployment Steps

### 1. Clone Repository

```bash
# Switch to observability user
su - observability

# Clone repository
git clone https://github.com/your-org/bcbs239-platform.git
cd bcbs239-platform
```

### 2. Configure Environment Variables

```bash
# Create .env file
cat > .env << EOF
# Grafana Configuration
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=$(openssl rand -base64 32)
GRAFANA_ROOT_URL=https://grafana.yourdomain.com

# Hetzner Configuration
HETZNER_DATACENTER=fsn1
HETZNER_ZONE=fsn1-dc14

# Application Configuration
OTEL_COLLECTOR_ENDPOINT=http://otel-collector:4318
TRACE_SAMPLING_RATE=0.1
EOF

# Secure the .env file
chmod 600 .env

# Display generated password
echo "Grafana Admin Password: $(grep GRAFANA_ADMIN_PASSWORD .env | cut -d= -f2)"
```

### 3. Deploy Observability Stack

```bash
# Start the observability stack
docker compose -f docker-compose-observability.yml up -d

# Verify all containers are running
docker compose -f docker-compose-observability.yml ps

# Check logs
docker compose -f docker-compose-observability.yml logs -f
```

### 4. Configure SSL/TLS (Production)

```bash
# Install Nginx
sudo apt install nginx certbot python3-certbot-nginx -y

# Create Nginx configuration
sudo tee /etc/nginx/sites-available/grafana << EOF
server {
    listen 80;
    server_name grafana.yourdomain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Enable site
sudo ln -s /etc/nginx/sites-available/grafana /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Obtain SSL certificate
sudo certbot --nginx -d grafana.yourdomain.com
```

### 5. Configure Application to Send Telemetry

Update your application's `application.yml`:

```yaml
management:
  tracing:
    export:
      otlp:
        endpoint: http://<SERVER_IP>:4318/v1/traces
  metrics:
    export:
      otlp:
        endpoint: http://<SERVER_IP>:4318/v1/metrics
```

## Configuration

### 1. Grafana Initial Setup

1. Access Grafana: `https://grafana.yourdomain.com`
2. Login with admin credentials from `.env` file
3. Change admin password (Settings → Profile → Change Password)
4. Verify data sources (Configuration → Data Sources)
5. Import dashboards (see `observability/grafana/dashboards/README.md`)

### 2. Configure Alerting (Optional)

```bash
# Create Alertmanager configuration
mkdir -p observability/alertmanager
cat > observability/alertmanager/config.yml << EOF
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'email'

receivers:
  - name: 'email'
    email_configs:
      - to: 'alerts@yourdomain.com'
        from: 'alertmanager@yourdomain.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
EOF
```

### 3. Configure Backup

```bash
# Create backup script
cat > /home/observability/backup-observability.sh << 'EOF'
#!/bin/bash
set -e

BACKUP_DIR="/mnt/observability-data/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup Grafana data
docker run --rm \
  -v grafana-data:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/grafana-$DATE.tar.gz -C /data .

# Backup Prometheus data
docker run --rm \
  -v prometheus-data:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/prometheus-$DATE.tar.gz -C /data .

# Backup Tempo data
docker run --rm \
  -v tempo-data:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/tempo-$DATE.tar.gz -C /data .

# Backup Loki data
docker run --rm \
  -v loki-data:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/loki-$DATE.tar.gz -C /data .

# Remove backups older than 30 days
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete

echo "Backup completed: $DATE"
EOF

chmod +x /home/observability/backup-observability.sh

# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /home/observability/backup-observability.sh >> /var/log/observability-backup.log 2>&1") | crontab -
```

## Verification

### 1. Health Checks

```bash
# Check all services are healthy
docker compose -f docker-compose-observability.yml ps

# Test OpenTelemetry Collector
curl http://localhost:13133/

# Test Tempo
curl http://localhost:3200/ready

# Test Loki
curl http://localhost:3100/ready

# Test Prometheus
curl http://localhost:9090/-/healthy

# Test Grafana
curl http://localhost:3000/api/health
```

### 2. Send Test Telemetry

```bash
# Send test trace
curl -X POST http://localhost:4318/v1/traces \
  -H "Content-Type: application/json" \
  -d '{
    "resourceSpans": [{
      "resource": {
        "attributes": [{
          "key": "service.name",
          "value": {"stringValue": "test-service"}
        }]
      },
      "scopeSpans": [{
        "spans": [{
          "traceId": "5B8EFFF798038103D269B633813FC60C",
          "spanId": "EEE19B7EC3C1B174",
          "name": "test-span",
          "startTimeUnixNano": "1544712660000000000",
          "endTimeUnixNano": "1544712661000000000"
        }]
      }]
    }]
  }'

# Verify in Grafana Explore → Tempo
```

### 3. Verify Data Flow

1. Open Grafana: `https://grafana.yourdomain.com`
2. Go to Explore
3. Select Tempo data source
4. Search for recent traces
5. Select Loki data source
6. Query logs: `{service_name="bcbs239-platform"}`
7. Select Prometheus data source
8. Query metrics: `up`

## Backup Strategy

### Automated Backups

- **Frequency**: Daily at 2 AM
- **Retention**: 30 days local, 90 days in Hetzner Object Storage
- **Components**: Grafana, Prometheus, Tempo, Loki data

### Manual Backup

```bash
# Run backup script manually
/home/observability/backup-observability.sh

# Upload to Hetzner Object Storage (optional)
# Install s3cmd first: apt install s3cmd
s3cmd put /mnt/observability-data/backups/*.tar.gz s3://your-bucket/observability/
```

### Restore from Backup

```bash
# Stop services
docker compose -f docker-compose-observability.yml down

# Restore Grafana
docker run --rm \
  -v grafana-data:/data \
  -v /mnt/observability-data/backups:/backup \
  alpine tar xzf /backup/grafana-YYYYMMDD_HHMMSS.tar.gz -C /data

# Restore other services similarly...

# Start services
docker compose -f docker-compose-observability.yml up -d
```

## Monitoring and Maintenance

### Regular Maintenance Tasks

**Daily:**
- Check service health
- Review error logs
- Monitor disk usage

**Weekly:**
- Review dashboard performance
- Check backup completion
- Update security patches

**Monthly:**
- Review data retention policies
- Optimize query performance
- Update Docker images

### Monitoring Commands

```bash
# Check disk usage
df -h

# Check Docker resource usage
docker stats

# Check service logs
docker compose -f docker-compose-observability.yml logs --tail=100 <service>

# Check backup logs
tail -f /var/log/observability-backup.log
```

### Updating Services

```bash
# Pull latest images
docker compose -f docker-compose-observability.yml pull

# Restart services
docker compose -f docker-compose-observability.yml up -d

# Remove old images
docker image prune -a
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker compose -f docker-compose-observability.yml logs <service>

# Check configuration
docker compose -f docker-compose-observability.yml config

# Restart service
docker compose -f docker-compose-observability.yml restart <service>
```

### High Memory Usage

```bash
# Check memory usage
docker stats

# Adjust memory limits in docker-compose-observability.yml
# Add under service definition:
deploy:
  resources:
    limits:
      memory: 2G
```

### Disk Space Issues

```bash
# Check disk usage
df -h

# Clean up old data
docker system prune -a --volumes

# Adjust retention policies in configuration files
```

### Network Connectivity Issues

```bash
# Check network
docker network ls
docker network inspect observability

# Test connectivity between services
docker exec otel-collector ping tempo
docker exec otel-collector ping loki
docker exec otel-collector ping prometheus
```

### Data Not Appearing in Grafana

1. Check data source connections in Grafana
2. Verify services are receiving data (check logs)
3. Test queries in Explore view
4. Check time range and refresh interval
5. Verify application is sending telemetry

## Cost Estimation

### Monthly Costs (Hetzner)

**Small Deployment:**
- CX31 Server: €10/month
- Total: ~€10/month

**Medium Deployment:**
- CX41 Server: €15/month
- 100GB Volume: €5/month
- Total: ~€20/month

**Large Deployment:**
- CX51 Server: €35/month
- 200GB Volume: €10/month
- Object Storage (100GB): €5/month
- Total: ~€50/month

### Cost Optimization

- Use data retention policies to limit storage
- Adjust sampling rates for traces
- Use recording rules in Prometheus for expensive queries
- Enable compression for data export
- Use Hetzner Object Storage for long-term backups

## Security Considerations

1. **Firewall**: Restrict access to Grafana and internal ports
2. **SSL/TLS**: Use HTTPS for Grafana access
3. **Authentication**: Enable strong passwords and 2FA
4. **Network**: Use internal network for service communication
5. **Backups**: Encrypt backups before uploading to object storage
6. **Updates**: Keep Docker images and system packages updated
7. **Monitoring**: Set up alerts for security events

## Support and Resources

- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Tempo Documentation](https://grafana.com/docs/tempo/)
- [Loki Documentation](https://grafana.com/docs/loki/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Hetzner Cloud Documentation](https://docs.hetzner.com/cloud/)

## Next Steps

1. Create custom dashboards for your application
2. Set up alerting rules
3. Configure log aggregation from application
4. Implement distributed tracing in application code
5. Set up SLA monitoring
6. Create runbooks for common issues
