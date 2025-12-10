# RegTech Platform Runbooks

## Overview

This document contains operational runbooks for common scenarios in the RegTech platform. Each runbook provides step-by-step procedures for handling specific situations.

## Runbook: Application Deployment

### Scenario
Deploying a new version of the RegTech application with observability validation.

### Prerequisites
- Application code changes committed to repository
- CI/CD pipeline completed successfully
- Observability stack running

### Steps

1. **Pre-deployment Checks**
   ```bash
   # Verify observability stack health
   docker-compose -f docker-compose-observability.yml ps

   # Check current application status
   docker-compose ps regtech-app

   # Backup current metrics (optional)
   docker exec prometheus tar czf /tmp/prometheus-backup-$(date +%Y%m%d-%H%M%S).tar.gz /prometheus
   ```

2. **Build Application**
   ```bash
   # Build with tests
   ./mvnw clean package

   # Verify build artifacts
   ls -la target/*.jar
   ```

3. **Deploy Application**
   ```bash
   # Stop current application
   docker-compose stop regtech-app

   # Deploy new version
   docker-compose up -d regtech-app

   # Wait for health check
   sleep 30
   ```

4. **Post-deployment Validation**
   ```bash
   # Check application logs
   docker-compose logs -f regtech-app | head -50

   # Verify health endpoint
   curl -f http://localhost:8080/actuator/health

   # Check metrics endpoint
   curl -s http://localhost:8080/actuator/prometheus | head -20

   # Verify in Grafana dashboards
   # - Operations Overview should show service as UP
   # - API endpoints should be responding
   ```

5. **Rollback Procedure** (if needed)
   ```bash
   # Stop failed deployment
   docker-compose stop regtech-app

   # Restore previous version
   docker-compose up -d --scale regtech-app=0
   docker tag regtech-app:previous regtech-app:latest
   docker-compose up -d regtech-app

   # Verify rollback success
   curl -f http://localhost:8080/actuator/health
   ```

### Success Criteria
- Application starts within 60 seconds
- Health endpoint returns HTTP 200
- Metrics are being collected
- API endpoints respond normally
- No errors in application logs

### Monitoring
- Watch Operations Overview dashboard for 15 minutes post-deployment
- Monitor error rates and response times
- Alert if error rate > 1% or P95 > 1 second

---

## Runbook: Service Restart Procedures

### Scenario
Restarting individual services in the observability stack.

### OpenTelemetry Collector Restart

**When to use**: Collector not processing telemetry data, high memory usage.

```bash
# Check current status
docker-compose -f docker-compose-observability.yml ps otel-collector

# Restart collector
docker-compose -f docker-compose-observability.yml restart otel-collector

# Verify restart
docker-compose -f docker-compose-observability.yml logs otel-collector | tail -20

# Check metrics endpoint
curl -s http://localhost:8888/metrics | grep otelcol
```

### Prometheus Restart

**When to use**: Metrics not being scraped, Prometheus UI unresponsive.

```bash
# Check current status
docker-compose -f docker-compose-observability.yml ps prometheus

# Restart Prometheus
docker-compose -f docker-compose-observability.yml restart prometheus

# Verify targets are healthy
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[].health'

# Check metrics collection
curl -s http://localhost:9090/api/v1/query?query=up | jq '.data.result'
```

### Grafana Restart

**When to use**: Grafana UI unresponsive, dashboards not loading.

```bash
# Check current status
docker-compose -f docker-compose-observability.yml ps grafana

# Restart Grafana
docker-compose -f docker-compose-observability.yml restart grafana

# Verify dashboards are loaded
curl -s http://localhost:3000/api/search | jq '.[].title'

# Check data sources
curl -u admin:admin -s http://localhost:3000/api/datasources | jq '.[].name'
```

### Tempo Restart

**When to use**: Traces not appearing, Tempo UI errors.

```bash
# Check current status
docker-compose -f docker-compose-observability.yml ps tempo

# Restart Tempo
docker-compose -f docker-compose-observability.yml restart tempo

# Verify Tempo health
curl -s http://localhost:3200/ready

# Check trace ingestion
curl -s http://localhost:3200/api/search | jq '.traces'
```

### Loki Restart

**When to use**: Logs not appearing, Loki query errors.

```bash
# Check current status
docker-compose -f docker-compose-observability.yml ps loki

# Restart Loki
docker-compose -f docker-compose-observability.yml restart loki

# Verify Loki health
curl -s http://localhost:3100/ready

# Check log ingestion
curl -s "http://localhost:3100/loki/api/v1/query?query={job=\"regtech-app\"}" | jq '.data.result'
```

---

## Runbook: High Error Rate Response

### Scenario
API error rate exceeds 5%, indicating application issues.

### Detection
- SLA API Performance dashboard shows Error Rate > 5%
- Operations Overview shows red indicators
- Alert notifications received

### Immediate Actions

1. **Assess Impact**
   ```bash
   # Check current error rate
   curl -s "http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~\"4..|5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100" | jq '.data.result[0].value[1]'

   # Identify failing endpoints
   curl -s "http://localhost:9090/api/v1/query?query=topk(5, sum(rate(http_server_requests_seconds_count{status=~\"4..|5..\"}[5m])) by (uri, method))" | jq '.data.result'
   ```

2. **Check Application Logs**
   ```bash
   # View recent error logs
   docker-compose logs --tail=100 regtech-app | grep -i error

   # Follow logs in real-time
   docker-compose logs -f regtech-app | grep -i error
   ```

3. **Check System Resources**
   ```bash
   # Check container resource usage
   docker stats regtech-app

   # Check JVM memory
   curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements'

   # Check database connections
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements'
   ```

4. **Database Health Check**
   ```bash
   # Check database connectivity
   docker-compose exec postgres pg_isready -h localhost -U myuser -d mydatabase

   # Check active connections
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';"
   ```

### Mitigation Steps

1. **If Database Issues**
   ```bash
   # Restart database
   docker-compose restart postgres

   # Check database logs
   docker-compose logs postgres | tail -50
   ```

2. **If Memory Issues**
   ```bash
   # Restart application
   docker-compose restart regtech-app

   # Monitor memory usage post-restart
   docker stats regtech-app
   ```

3. **If Application Code Issues**
   - Rollback to previous version (see Application Deployment runbook)
   - Contact development team for investigation

### Communication

- Notify stakeholders of incident
- Provide ETA for resolution
- Update status every 15 minutes

### Post-Incident

1. **Root Cause Analysis**
   - Review application logs during incident
   - Check monitoring dashboards for patterns
   - Analyze metrics before/during/after incident

2. **Preventive Measures**
   - Implement additional monitoring if needed
   - Update alert thresholds if too sensitive
   - Create tickets for code fixes

---

## Runbook: High Response Time Response

### Scenario
API P95 response time exceeds 2 seconds, affecting user experience.

### Detection
- SLA API Performance dashboard shows P95 > 2s
- Operations Overview shows orange/red response time indicators
- User complaints about slow performance

### Investigation Steps

1. **Identify Slow Endpoints**
   ```bash
   # Find slowest endpoints
   curl -s "http://localhost:9090/api/v1/query?query=topk(5, histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)))" | jq '.data.result'
   ```

2. **Check System Resources**
   ```bash
   # CPU usage
   docker stats regtech-app --format "table {{.Container}}\t{{.CPUPerc}}"

   # Memory usage
   curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements[] | select(.statistic == "VALUE") | .value / 1024 / 1024 | floor'

   # Thread count
   curl -s http://localhost:8080/actuator/metrics/jvm.threads.live | jq '.measurements[0].value'
   ```

3. **Database Performance**
   ```bash
   # Check slow queries
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE state = 'active' AND now() - pg_stat_activity.query_start > interval '1 second' ORDER BY duration DESC;"

   # Check connection pool
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements'
   ```

4. **External Dependencies**
   ```bash
   # Check network connectivity
   docker-compose exec regtech-app ping -c 3 postgres

   # Check external service health (if applicable)
   curl -f https://external-service.com/health
   ```

### Mitigation Steps

1. **Scale Resources** (if applicable)
   ```bash
   # Increase container resources
   docker-compose up -d --scale regtech-app=2

   # Or increase memory limits
   docker-compose up -d regtech-app --memory=4g
   ```

2. **Database Optimization**
   ```bash
   # Check for long-running transactions
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT * FROM pg_stat_activity WHERE state = 'idle in transaction' AND now() - state_change > interval '1 minute';"

   # Kill problematic connections if needed
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle in transaction' AND now() - state_change > interval '5 minutes';"
   ```

3. **Application Restart**
   ```bash
   # Graceful restart
   docker-compose restart regtech-app

   # Monitor performance post-restart
   ```

### Performance Tuning

1. **JVM Tuning**
   ```bash
   # Adjust GC settings
   -XX:+UseG1GC
   -XX:MaxGCPauseMillis=100
   -XX:G1HeapRegionSize=16m
   ```

2. **Connection Pool Tuning**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
   ```

3. **Caching Implementation**
   - Implement Redis caching for frequently accessed data
   - Add database query result caching

---

## Runbook: Database Connection Issues

### Scenario
Application cannot connect to PostgreSQL database.

### Symptoms
- Application logs show connection errors
- Health endpoint fails
- Database-related API calls fail

### Investigation

1. **Check Database Status**
   ```bash
   # Database container status
   docker-compose ps postgres

   # Database connectivity
   docker-compose exec postgres pg_isready -h localhost -U myuser -d mydatabase

   # Database logs
   docker-compose logs postgres | tail -50
   ```

2. **Check Connection Pool**
   ```bash
   # Active connections
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements'

   # Connection pool status
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections | jq '.available[0].measurements'
   ```

3. **Network Connectivity**
   ```bash
   # Test from application container
   docker-compose exec regtech-app nc -zv postgres 5432

   # DNS resolution
   docker-compose exec regtech-app nslookup postgres
   ```

### Resolution Steps

1. **Restart Database**
   ```bash
   docker-compose restart postgres

   # Wait for startup
   sleep 30

   # Verify connectivity
   docker-compose exec postgres pg_isready -h localhost -U myuser -d mydatabase
   ```

2. **Check Disk Space**
   ```bash
   # Database disk usage
   docker-compose exec postgres df -h /var/lib/postgresql/data

   # Clean up if needed
   docker-compose exec postgres vacuumdb --analyze --verbose mydatabase
   ```

3. **Connection Pool Reset**
   ```bash
   # Restart application to reset connection pool
   docker-compose restart regtech-app

   # Monitor connection establishment
   docker-compose logs -f regtech-app | grep -i "connection\|pool"
   ```

4. **Database Recovery** (if corrupted)
   ```bash
   # Stop application
   docker-compose stop regtech-app

   # Backup current data
   docker exec postgres pg_dump -U myuser mydatabase > backup_$(date +%Y%m%d_%H%M%S).sql

   # Recreate database
   docker-compose exec postgres psql -U myuser -c "DROP DATABASE mydatabase;"
   docker-compose exec postgres psql -U myuser -c "CREATE DATABASE mydatabase;"

   # Restore from backup
   docker exec -i postgres psql -U myuser mydatabase < backup_file.sql

   # Restart application
   docker-compose up -d regtech-app
   ```

---

## Runbook: Observability Stack Failure

### Scenario
One or more observability components are down or malfunctioning.

### Detection
- System Observability Health dashboard shows services as DOWN
- Grafana cannot connect to data sources
- Metrics/logs/traces not appearing

### Component-Specific Recovery

#### OpenTelemetry Collector Down
```bash
# Check status
docker-compose -f docker-compose-observability.yml ps otel-collector

# Restart collector
docker-compose -f docker-compose-observability.yml restart otel-collector

# Verify telemetry flow
curl -s http://localhost:8888/metrics | grep otelcol_processor_batch
```

#### Prometheus Down
```bash
# Check status
docker-compose -f docker-compose-observability.yml ps prometheus

# Restart Prometheus
docker-compose -f docker-compose-observability.yml restart prometheus

# Verify targets
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets | length'

# Check data ingestion
curl -s http://localhost:9090/api/v1/query?query=up | jq '.data.result | length'
```

#### Grafana Down
```bash
# Check status
docker-compose -f docker-compose-observability.yml ps grafana

# Restart Grafana
docker-compose -f docker-compose-observability.yml restart grafana

# Verify dashboard loading
curl -u admin:admin -s http://localhost:3000/api/search | jq 'length'

# Check data source connections
curl -u admin:admin -s http://localhost:3000/api/datasources | jq '.[].status'
```

### Full Stack Restart

**When to use**: Multiple components failing, or complete stack recovery needed.

```bash
# Stop entire stack
docker-compose -f docker-compose-observability.yml down

# Clean up (optional, removes volumes)
# docker-compose -f docker-compose-observability.yml down -v

# Start stack
docker-compose -f docker-compose-observability.yml up -d

# Wait for services to be healthy
sleep 60

# Verify all services
docker-compose -f docker-compose-observability.yml ps

# Check data flow
curl -s http://localhost:9090/api/v1/query?query=up | jq '.data.result[0].value[1]'
```

### Data Recovery

**If data loss occurred:**

1. **Prometheus Data**
   - Check if data volume still exists
   - Restore from backup if available
   - Re-scrape metrics from applications

2. **Grafana Configuration**
   - Dashboards auto-reload from templates
   - Data sources reconfigure via provisioning

3. **Logs and Traces**
   - Loki and Tempo data may be lost if volumes were removed
   - Applications will continue sending new data

### Prevention

- Regular backup of observability data
- Monitor disk space on observability volumes
- Implement health checks and automated restarts
- Use persistent volumes for data storage

---

## Runbook: Security Incident Response

### Scenario
Potential security breach or suspicious activity detected.

### Immediate Actions

1. **Isolate Affected Systems**
   ```bash
   # Stop external access
   docker-compose stop regtech-app

   # Block suspicious IPs (if known)
   # Implement firewall rules
   ```

2. **Preserve Evidence**
   ```bash
   # Collect logs
   docker-compose logs regtech-app > incident_logs_$(date +%Y%m%d_%H%M%S).log

   # Database snapshot
   docker-compose exec postgres pg_dump -U myuser mydatabase > incident_db_$(date +%Y%m%d_%H%M%S).sql

   # System state
   docker stats --no-stream > incident_stats_$(date +%Y%m%d_%H%M%S).txt
   ```

3. **Security Assessment**
   ```bash
   # Check for unauthorized access
   docker-compose logs | grep -i "unauthorized\|forbidden\|authentication"

   # Review recent configuration changes
   git log --oneline -10

   # Check file integrity
   find . -name "*.jar" -exec sha256sum {} \; > jar_checksums.txt
   ```

### Investigation Steps

1. **Log Analysis**
   - Review access logs for suspicious patterns
   - Check authentication failures
   - Analyze API usage patterns

2. **System Inspection**
   - Check running processes
   - Review network connections
   - Examine file system changes

3. **Contact Security Team**
   - Escalate to security incident response team
   - Provide collected evidence
   - Follow security incident procedures

### Recovery

1. **Clean Systems**
   - Remove compromised components
   - Update passwords and keys
   - Patch known vulnerabilities

2. **Restore from Backup**
   - Use clean backup for data restoration
   - Verify backup integrity before restore

3. **Monitoring**
   - Implement additional security monitoring
   - Review and update security policies
   - Conduct security assessment

### Post-Incident

- Document incident details
- Update security procedures
- Implement preventive measures
- Conduct lessons learned session

---

## Runbook: Capacity Planning

### Scenario
Planning for increased load or scaling requirements.

### Current Capacity Assessment

1. **Resource Usage Analysis**
   ```bash
   # Current CPU usage
   docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

   # Memory trends
   curl -s "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"}/jvm_memory_max_bytes{area=\"heap\"}" | jq '.data.result[0].value[1]'

   # Database size
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT pg_size_pretty(pg_database_size('mydatabase'));"
   ```

2. **Performance Metrics Review**
   ```bash
   # Peak request rates
   curl -s "http://localhost:9090/api/v1/query?query=max_over_time(sum(rate(http_server_requests_seconds_count[5m]))[7d])" | jq '.data.result[0].value[1]'

   # Peak response times
   curl -s "http://localhost:9090/api/v1/query?query=max_over_time(histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))[7d])" | jq '.data.result[0].value[1]'
   ```

3. **Storage Growth**
   ```bash
   # Database growth rate
   docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) FROM pg_tables ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 10;"

   # Observability data growth
   du -sh observability/*/data/
   ```

### Scaling Recommendations

1. **Vertical Scaling** (increase resources)
   ```yaml
   # docker-compose.yml updates
   services:
     regtech-app:
       deploy:
         resources:
           limits:
             memory: 4G
             cpus: '2.0'
           reservations:
             memory: 2G
             cpus: '1.0'
   ```

2. **Horizontal Scaling** (multiple instances)
   ```bash
   # Scale application instances
   docker-compose up -d --scale regtech-app=3

   # Add load balancer configuration
   ```

3. **Database Scaling**
   - Connection pool tuning
   - Read replicas for read-heavy workloads
   - Database sharding for large datasets

### Monitoring After Scaling

- Monitor resource usage post-scaling
- Verify performance improvements
- Check for new bottlenecks
- Update capacity planning baselines

---

## Emergency Contacts

### Development Team
- **Lead Developer**: [Name] - [Phone] - [Email]
- **DevOps Engineer**: [Name] - [Phone] - [Email]
- **Database Administrator**: [Name] - [Phone] - [Email]

### Infrastructure Team
- **System Administrator**: [Name] - [Phone] - [Email]
- **Network Engineer**: [Name] - [Phone] - [Email]

### Business Stakeholders
- **Product Owner**: [Name] - [Phone] - [Email]
- **Business Analyst**: [Name] - [Phone] - [Email]

### External Support
- **Cloud Provider Support**: [Support Phone] - [Support Portal]
- **Third-party Services**: [Vendor Support Contacts]

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-10 | 1.0 | Initial runbook creation |
| | | Added deployment, restart, and incident response procedures |
| | | Included monitoring and troubleshooting guides |