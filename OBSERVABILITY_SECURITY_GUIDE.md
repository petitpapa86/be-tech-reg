# Observability Stack Security Guide

This guide provides comprehensive security configuration for the observability stack deployment on Hetzner infrastructure.

## Table of Contents

1. [Network Security](#network-security)
2. [Access Control](#access-control)
3. [Authentication](#authentication)
4. [Encryption](#encryption)
5. [Monitoring and Auditing](#monitoring-and-auditing)
6. [Security Best Practices](#security-best-practices)

## Network Security

### Firewall Configuration

#### UFW (Uncomplicated Firewall) Setup

```bash
# Reset firewall rules
sudo ufw --force reset

# Default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (restrict to your IP)
sudo ufw allow from YOUR_IP_ADDRESS to any port 22 proto tcp

# Allow Grafana (restrict to VPN or specific IPs)
sudo ufw allow from YOUR_VPN_NETWORK to any port 3000 proto tcp

# Or allow from specific IPs
sudo ufw allow from 203.0.113.0/24 to any port 3000 proto tcp

# Internal services (deny external access)
# These ports should only be accessible within Docker network
# No UFW rules needed as they're not exposed to host

# Enable firewall
sudo ufw enable

# Verify rules
sudo ufw status verbose
```

#### Hetzner Cloud Firewall

Create firewall rules in Hetzner Cloud Console:

```yaml
# Inbound Rules
- name: SSH
  direction: in
  protocol: tcp
  port: 22
  source_ips:
    - YOUR_IP_ADDRESS/32

- name: Grafana
  direction: in
  protocol: tcp
  port: 3000
  source_ips:
    - YOUR_VPN_NETWORK/24

- name: HTTPS
  direction: in
  protocol: tcp
  port: 443
  source_ips:
    - 0.0.0.0/0  # If using reverse proxy

# Outbound Rules
- name: Allow All Outbound
  direction: out
  protocol: tcp
  port: any
  destination_ips:
    - 0.0.0.0/0
```

### Internal Network Isolation

The observability stack uses an isolated Docker network (10.0.1.0/24):

```yaml
# docker-compose-observability.yml
networks:
  observability:
    driver: bridge
    ipam:
      config:
        - subnet: 10.0.1.0/24
```

**Service IP Assignments:**
- OpenTelemetry Collector: 10.0.1.10
- Tempo: 10.0.1.20
- Loki: 10.0.1.30
- Prometheus: 10.0.1.40
- Grafana: 10.0.1.50

**Benefits:**
- Services communicate only within isolated network
- No external access to internal services
- Predictable IP addressing for configuration

### Port Exposure Strategy

**Exposed Ports (Host):**
- 3000: Grafana (behind reverse proxy)
- 4317/4318: OTLP receivers (application access only)

**Internal Ports (Docker network only):**
- 3100: Loki
- 3200: Tempo
- 9090: Prometheus
- 8888/8889: OTel Collector metrics

## Access Control

### Grafana Authentication

#### Local Authentication

```bash
# Set strong admin password
export GRAFANA_ADMIN_PASSWORD=$(openssl rand -base64 32)

# Update docker-compose-observability.yml
environment:
  - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
  - GF_USERS_ALLOW_SIGN_UP=false
  - GF_AUTH_ANONYMOUS_ENABLED=false
```

#### OAuth2 Authentication (Recommended for Production)

```yaml
# Add to Grafana environment in docker-compose-observability.yml
environment:
  # Google OAuth
  - GF_AUTH_GOOGLE_ENABLED=true
  - GF_AUTH_GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
  - GF_AUTH_GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
  - GF_AUTH_GOOGLE_SCOPES=openid email profile
  - GF_AUTH_GOOGLE_AUTH_URL=https://accounts.google.com/o/oauth2/auth
  - GF_AUTH_GOOGLE_TOKEN_URL=https://accounts.google.com/o/oauth2/token
  - GF_AUTH_GOOGLE_ALLOWED_DOMAINS=yourdomain.com
  - GF_AUTH_GOOGLE_ALLOW_SIGN_UP=true
```

#### LDAP Authentication

```yaml
# Create ldap.toml configuration
cat > observability/grafana/ldap.toml << EOF
[[servers]]
host = "ldap.yourdomain.com"
port = 389
use_ssl = false
start_tls = true
ssl_skip_verify = false

bind_dn = "cn=admin,dc=yourdomain,dc=com"
bind_password = 'your-bind-password'

search_filter = "(uid=%s)"
search_base_dns = ["ou=users,dc=yourdomain,dc=com"]

[servers.attributes]
name = "givenName"
surname = "sn"
username = "uid"
member_of = "memberOf"
email = "mail"

[[servers.group_mappings]]
group_dn = "cn=admins,ou=groups,dc=yourdomain,dc=com"
org_role = "Admin"

[[servers.group_mappings]]
group_dn = "cn=editors,ou=groups,dc=yourdomain,dc=com"
org_role = "Editor"

[[servers.group_mappings]]
group_dn = "cn=viewers,ou=groups,dc=yourdomain,dc=com"
org_role = "Viewer"
EOF

# Mount in docker-compose-observability.yml
volumes:
  - ./observability/grafana/ldap.toml:/etc/grafana/ldap.toml

# Enable LDAP in environment
environment:
  - GF_AUTH_LDAP_ENABLED=true
  - GF_AUTH_LDAP_CONFIG_FILE=/etc/grafana/ldap.toml
```

### Role-Based Access Control (RBAC)

#### Grafana Roles

**Admin:**
- Full access to all dashboards and data sources
- Can create and modify dashboards
- Can manage users and permissions
- Can configure data sources

**Editor:**
- Can view and edit dashboards
- Cannot modify data sources
- Cannot manage users

**Viewer:**
- Read-only access to dashboards
- Cannot edit or create dashboards
- Cannot access admin settings

#### Creating Teams and Permissions

```bash
# Via Grafana API
curl -X POST http://localhost:3000/api/teams \
  -H "Authorization: Bearer ${GRAFANA_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Operations Team",
    "email": "ops@yourdomain.com"
  }'

# Assign dashboard permissions
curl -X POST http://localhost:3000/api/dashboards/uid/${DASHBOARD_UID}/permissions \
  -H "Authorization: Bearer ${GRAFANA_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "teamId": 1,
        "permission": 2
      }
    ]
  }'
```

### OTLP Collector Authentication

#### Basic Authentication

```yaml
# Add to otel-collector-config.yaml
extensions:
  basicauth/server:
    htpasswd:
      file: /etc/otel-collector/htpasswd
      inline: |
        user1:$apr1$...
        user2:$apr1$...

receivers:
  otlp:
    protocols:
      http:
        auth:
          authenticator: basicauth/server

service:
  extensions: [basicauth/server]
```

#### API Key Authentication

```yaml
# Add to otel-collector-config.yaml
extensions:
  headers_setter:
    headers:
      - key: X-API-Key
        from_context: api_key

receivers:
  otlp:
    protocols:
      http:
        include_metadata: true

processors:
  attributes:
    actions:
      - key: api_key
        action: delete  # Remove after validation
```

### Rate Limiting

```yaml
# Add to otel-collector-config.yaml
processors:
  batch:
    timeout: 10s
    send_batch_size: 1024
    send_batch_max_size: 2048

  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128

  # Rate limiting processor
  probabilistic_sampler:
    sampling_percentage: 10  # Sample 10% of traces
```

## Encryption

### SSL/TLS Configuration

#### Nginx Reverse Proxy

```nginx
# /etc/nginx/sites-available/grafana
server {
    listen 443 ssl http2;
    server_name grafana.yourdomain.com;

    # SSL certificates
    ssl_certificate /etc/letsencrypt/live/grafana.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/grafana.yourdomain.com/privkey.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;

    # Proxy configuration
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=grafana:10m rate=10r/s;
    limit_req zone=grafana burst=20 nodelay;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name grafana.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

#### Let's Encrypt Certificate

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtain certificate
sudo certbot --nginx -d grafana.yourdomain.com

# Auto-renewal (already configured by Certbot)
sudo certbot renew --dry-run
```

### Data Encryption at Rest

#### Encrypt Docker Volumes

```bash
# Install cryptsetup
sudo apt install cryptsetup -y

# Create encrypted volume
sudo cryptsetup luksFormat /dev/sdb
sudo cryptsetup luksOpen /dev/sdb observability-encrypted

# Create filesystem
sudo mkfs.ext4 /dev/mapper/observability-encrypted

# Mount
sudo mkdir -p /mnt/observability-encrypted
sudo mount /dev/mapper/observability-encrypted /mnt/observability-encrypted

# Update docker-compose-observability.yml to use encrypted volume
volumes:
  grafana-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/observability-encrypted/grafana
```

### Backup Encryption

```bash
# Encrypt backups before upload
gpg --symmetric --cipher-algo AES256 backup.tar.gz

# Upload encrypted backup
s3cmd put backup.tar.gz.gpg s3://your-bucket/observability/

# Decrypt backup
gpg --decrypt backup.tar.gz.gpg > backup.tar.gz
```

## Monitoring and Auditing

### Audit Logging

#### Grafana Audit Logs

```yaml
# Add to Grafana environment
environment:
  - GF_LOG_MODE=console file
  - GF_LOG_LEVEL=info
  - GF_LOG_FILTERS=alerting.notifier:debug

# Mount audit log volume
volumes:
  - ./observability/grafana/logs:/var/log/grafana
```

#### Access Log Monitoring

```bash
# Monitor Nginx access logs
tail -f /var/log/nginx/access.log | grep grafana

# Monitor failed login attempts
docker logs grafana 2>&1 | grep "Invalid username or password"

# Set up log rotation
cat > /etc/logrotate.d/observability << EOF
/var/log/nginx/access.log
/var/log/nginx/error.log
/home/observability/observability/grafana/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        systemctl reload nginx
    endscript
}
EOF
```

### Security Monitoring

#### Failed Authentication Alerts

```yaml
# Prometheus alert rule
groups:
  - name: security
    rules:
      - alert: HighFailedLoginRate
        expr: rate(grafana_api_login_post_total{status="error"}[5m]) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate of failed login attempts"
          description: "More than 5 failed login attempts per second in the last 5 minutes"
```

#### Suspicious Activity Detection

```bash
# Monitor for suspicious patterns
# Create monitoring script
cat > /home/observability/monitor-security.sh << 'EOF'
#!/bin/bash

# Check for brute force attempts
FAILED_LOGINS=$(docker logs grafana 2>&1 | grep -c "Invalid username or password")
if [ $FAILED_LOGINS -gt 10 ]; then
    echo "WARNING: $FAILED_LOGINS failed login attempts detected"
    # Send alert
fi

# Check for unauthorized access attempts
UNAUTHORIZED=$(grep -c "401\|403" /var/log/nginx/access.log)
if [ $UNAUTHORIZED -gt 50 ]; then
    echo "WARNING: $UNAUTHORIZED unauthorized access attempts"
    # Send alert
fi
EOF

chmod +x /home/observability/monitor-security.sh

# Add to crontab (every 5 minutes)
(crontab -l 2>/dev/null; echo "*/5 * * * * /home/observability/monitor-security.sh >> /var/log/security-monitor.log 2>&1") | crontab -
```

## Security Best Practices

### 1. Regular Updates

```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Update Docker images
docker compose -f docker-compose-observability.yml pull
docker compose -f docker-compose-observability.yml up -d

# Check for security vulnerabilities
docker scan grafana/grafana:10.2.0
```

### 2. Principle of Least Privilege

- Use separate service accounts for each component
- Grant minimum required permissions
- Regularly review and audit permissions
- Use read-only access where possible

### 3. Network Segmentation

- Isolate observability stack in separate network
- Use firewall rules to restrict access
- Implement VPN for remote access
- Use bastion hosts for SSH access

### 4. Secrets Management

```bash
# Use Docker secrets instead of environment variables
echo "your-secret-password" | docker secret create grafana_admin_password -

# Update docker-compose-observability.yml
services:
  grafana:
    secrets:
      - grafana_admin_password
    environment:
      - GF_SECURITY_ADMIN_PASSWORD__FILE=/run/secrets/grafana_admin_password

secrets:
  grafana_admin_password:
    external: true
```

### 5. Regular Security Audits

**Weekly:**
- Review access logs
- Check for failed authentication attempts
- Verify firewall rules
- Review user permissions

**Monthly:**
- Update all components
- Review and rotate credentials
- Audit user accounts
- Test backup restoration
- Review security alerts

**Quarterly:**
- Penetration testing
- Security policy review
- Disaster recovery drill
- Compliance audit

### 6. Incident Response Plan

```markdown
# Security Incident Response

## Detection
1. Monitor security alerts
2. Review audit logs
3. Check for anomalies

## Containment
1. Isolate affected systems
2. Block malicious IPs
3. Revoke compromised credentials

## Eradication
1. Remove malicious code
2. Patch vulnerabilities
3. Update security rules

## Recovery
1. Restore from clean backups
2. Verify system integrity
3. Monitor for recurrence

## Lessons Learned
1. Document incident
2. Update security policies
3. Improve detection mechanisms
```

### 7. Compliance Considerations

**GDPR:**
- Implement data retention policies
- Provide data export capabilities
- Enable data deletion
- Maintain audit logs

**SOC 2:**
- Implement access controls
- Enable audit logging
- Encrypt data in transit and at rest
- Regular security assessments

**ISO 27001:**
- Document security policies
- Implement risk management
- Regular security training
- Incident response procedures

## Security Checklist

### Initial Setup
- [ ] Configure firewall rules
- [ ] Enable SSL/TLS
- [ ] Set strong passwords
- [ ] Disable default accounts
- [ ] Configure authentication
- [ ] Set up RBAC
- [ ] Enable audit logging
- [ ] Configure backup encryption

### Ongoing Maintenance
- [ ] Regular security updates
- [ ] Monitor access logs
- [ ] Review user permissions
- [ ] Rotate credentials
- [ ] Test backups
- [ ] Review security alerts
- [ ] Update documentation

### Incident Response
- [ ] Document incident response plan
- [ ] Test incident response procedures
- [ ] Maintain contact list
- [ ] Regular security drills
- [ ] Post-incident reviews

## Resources

- [OWASP Security Guidelines](https://owasp.org/)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [Grafana Security Documentation](https://grafana.com/docs/grafana/latest/setup-grafana/configure-security/)
- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
