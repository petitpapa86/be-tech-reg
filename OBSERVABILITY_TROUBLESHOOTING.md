# RegTech Platform Troubleshooting Guide

## Quick Reference

This guide provides quick solutions for common issues encountered in the RegTech platform.

## Application Issues

### Application Won't Start

**Symptoms**: Container exits immediately, logs show startup failures

**Quick Checks**:
```bash
# Check application logs
docker-compose logs regtech-app

# Verify Java version
java -version

# Check environment variables
docker-compose exec regtech-app env | grep -E "(DB_|SPRING_)"

# Verify database connectivity
docker-compose exec regtech-app nc -zv postgres 5432
```

**Common Solutions**:
1. **Database Connection**: Ensure PostgreSQL is running and accessible
2. **Environment Variables**: Check `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE`
3. **Port Conflicts**: Verify port 8080 is available
4. **Memory Issues**: Increase Docker memory limits

### High Memory Usage

**Symptoms**: Application consuming excessive memory, frequent GC

**Diagnosis**:
```bash
# Check JVM memory
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq '.measurements'

# Check GC activity
curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause | jq '.measurements'

# Container memory usage
docker stats regtech-app
```

**Solutions**:
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Enable GC tuning
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Restart application
docker-compose restart regtech-app
```

### Slow API Responses

**Symptoms**: P95 response time > 1 second

**Quick Diagnosis**:
```bash
# Find slowest endpoints
curl -s "http://localhost:9090/api/v1/query?query=topk(5, histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)))" | jq '.data.result[].metric.uri'

# Check database query performance
docker-compose exec postgres psql -U myuser -d mydatabase -c "SELECT pid, now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC LIMIT 5;"

# Check thread pool
curl -s http://localhost:8080/actuator/metrics/jvm.threads.live | jq '.measurements[0].value'
```

**Quick Fixes**:
1. **Database**: Kill long-running queries
2. **Restart**: Application restart to clear thread pools
3. **Scale**: Increase container resources

## Database Issues

### Connection Pool Exhausted

**Symptoms**: "Connection pool exhausted" errors

**Check Pool Status**:
```bash
# Active connections
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements[0].value'

# Pool configuration
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections | jq '.available[0].measurements'
```

**Solutions**:
```bash
# Increase pool size (application.yml)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

# Restart application
docker-compose restart regtech-app
```

### Slow Queries

**Symptoms**: Database queries taking > 1 second

**Identify Slow Queries**:
```bash
# Find slow queries
docker-compose exec postgres psql -U myuser -d mydatabase -c "
SELECT
  pid,
  now() - pg_stat_activity.query_start AS duration,
  query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - pg_stat_activity.query_start > interval '1 second'
ORDER BY duration DESC;"

# Check query plan
docker-compose exec postgres psql -U myuser -d mydatabase -c "EXPLAIN ANALYZE SELECT * FROM large_table WHERE condition = 'value';"
```

**Quick Fixes**:
1. **Indexes**: Add missing indexes on frequently queried columns
2. **VACUUM**: Run `VACUUM ANALYZE` on tables
3. **Kill Query**: `SELECT pg_terminate_backend(pid);`

### Database Disk Full

**Symptoms**: Database operations fail, disk space errors

**Check Disk Usage**:
```bash
# Database disk usage
docker-compose exec postgres df -h /var/lib/postgresql/data

# Table sizes
docker-compose exec postgres psql -U myuser -d mydatabase -c "
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;"
```

**Solutions**:
```bash
# Clean up old data
docker-compose exec postgres psql -U myuser -d mydatabase -c "DELETE FROM old_table WHERE created_at < now() - interval '90 days';"

# VACUUM to reclaim space
docker-compose exec postgres psql -U myuser -d mydatabase -c "VACUUM FULL;"

# Resize volume if needed
```

## Observability Issues

### No Metrics in Grafana

**Symptoms**: Dashboards show "No data"

**Diagnosis**:
```bash
# Check Prometheus targets
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health != "up")'

# Check application metrics endpoint
curl -s http://localhost:8080/actuator/prometheus | head -10

# Check OpenTelemetry Collector
curl -s http://localhost:8888/metrics | grep otelcol
```

**Solutions**:
1. **Application**: Ensure `/actuator/prometheus` is accessible
2. **Collector**: Restart OpenTelemetry Collector
3. **Prometheus**: Check scrape configuration

### Grafana Dashboards Not Loading

**Symptoms**: Dashboards fail to load or show errors

**Check Grafana Status**:
```bash
# Grafana health
curl -s http://localhost:3000/api/health

# Dashboard list
curl -u admin:admin -s http://localhost:3000/api/search | jq 'length'

# Data source status
curl -u admin:admin -s http://localhost:3000/api/datasources | jq '.[].status'
```

**Solutions**:
```bash
# Restart Grafana
docker-compose -f docker-compose-observability.yml restart grafana

# Check provisioning
docker-compose -f docker-compose-observability.yml logs grafana | grep -i error

# Manual dashboard import if needed
```

### Logs Not Appearing

**Symptoms**: No logs in Loki or Grafana Explore

**Diagnosis**:
```bash
# Check Loki health
curl -s http://localhost:3100/ready

# Test log query
curl -s "http://localhost:3100/loki/api/v1/query?query={job=\"regtech-app\"}" | jq '.data.result | length'

# Check application logging configuration
docker-compose exec regtech-app cat application.yml | grep -A 10 logging
```

**Solutions**:
1. **Restart Loki**: `docker-compose restart loki`
2. **Check Labels**: Ensure log labels match Loki configuration
3. **Log Format**: Verify structured logging is enabled

## Network Issues

### Service Discovery Problems

**Symptoms**: Services can't communicate with each other

**Diagnosis**:
```bash
# Check Docker networks
docker network ls

# Verify service connectivity
docker-compose exec regtech-app ping postgres

# Check DNS resolution
docker-compose exec regtech-app nslookup postgres
```

**Solutions**:
```bash
# Restart affected services
docker-compose restart regtech-app postgres

# Recreate networks
docker-compose down
docker-compose up -d

# Check firewall rules
```

### External Access Issues

**Symptoms**: Cannot access application from outside Docker

**Check Port Mapping**:
```bash
# Verify port mapping
docker-compose ps

# Test local connectivity
curl -f http://localhost:8080/actuator/health

# Check firewall
netstat -tlnp | grep :8080
```

**Solutions**:
1. **Port Conflicts**: Change host port in docker-compose.yml
2. **Firewall**: Open required ports
3. **Network Mode**: Verify bridge network configuration

## Performance Issues

### High CPU Usage

**Symptoms**: CPU usage > 80%

**Diagnosis**:
```bash
# Check container CPU
docker stats --format "table {{.Container}}\t{{.CPUPerc}}"

# Application thread dumps
curl -s http://localhost:8080/actuator/threaddump | head -50

# JVM CPU metrics
curl -s http://localhost:8080/actuator/metrics/jvm.cpu.recent | jq '.measurements'
```

**Solutions**:
1. **Thread Dumps**: Analyze for blocking threads
2. **Scale Up**: Increase CPU limits
3. **Optimize Code**: Profile and optimize hot paths

### Memory Leaks

**Symptoms**: Memory usage steadily increasing

**Diagnosis**:
```bash
# Memory trend
curl -s "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"}" | jq '.data.result[0].value[1]'

# Heap dump (if enabled)
jmap -dump:live,format=b,file=heap.hprof <pid>

# GC logs
docker-compose logs regtech-app | grep -i gc
```

**Solutions**:
1. **Restart**: Temporary fix with application restart
2. **Heap Dump Analysis**: Use tools like VisualVM or MAT
3. **Code Review**: Look for object retention issues

## Configuration Issues

### Environment Variables Not Set

**Symptoms**: Application fails with configuration errors

**Check Configuration**:
```bash
# List environment variables
docker-compose exec regtech-app env | grep -E "(DB_|SPRING_)" | sort

# Check application.yml
docker-compose exec regtech-app cat application.yml | grep -v "^#" | grep -v "^$"
```

**Solutions**:
```bash
# Update .env file
echo "DB_USERNAME=myuser" >> .env
echo "DB_PASSWORD=secret" >> .env

# Restart application
docker-compose up -d regtech-app
```

### Invalid Configuration

**Symptoms**: Application fails to start with config errors

**Validate Configuration**:
```bash
# Check YAML syntax
docker-compose exec regtech-app python3 -c "import yaml; yaml.safe_load(open('application.yml'))"

# Check required properties
docker-compose logs regtech-app | grep -i "required\|missing\|invalid"
```

**Solutions**:
1. **YAML Syntax**: Fix indentation and formatting
2. **Required Properties**: Add missing configuration values
3. **Validate**: Use Spring Boot configuration validation

## Log Analysis

### Common Error Patterns

**Database Connection Errors**:
```
Caused by: org.postgresql.util.PSQLException: Connection to postgres:5432 refused
```
**Solution**: Check PostgreSQL container status and network connectivity

**Memory Errors**:
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution**: Increase JVM heap size or fix memory leaks

**Timeout Errors**:
```
java.net.SocketTimeoutException: Read timed out
```
**Solution**: Check network connectivity and increase timeouts

### Log Levels

**Debug Logging**:
```yaml
logging:
  level:
    com.bcbs239.regtech: DEBUG
    org.springframework: DEBUG
```

**Production Logging**:
```yaml
logging:
  level:
    root: INFO
    com.bcbs239.regtech: INFO
```

## Emergency Commands

### Complete System Restart
```bash
# Stop everything
docker-compose down
docker-compose -f docker-compose-observability.yml down

# Clean up (use with caution)
docker system prune -f

# Start fresh
docker-compose -f docker-compose-observability.yml up -d
docker-compose up -d
```

### Quick Health Check
```bash
# Check all services
docker-compose ps && docker-compose -f docker-compose-observability.yml ps

# Health endpoints
curl -f http://localhost:8080/actuator/health && \
curl -f http://localhost:3000/api/health && \
curl -f http://localhost:9090/-/healthy
```

### Log Collection for Support
```bash
# Collect all logs
docker-compose logs > app_logs_$(date +%Y%m%d_%H%M%S).log
docker-compose -f docker-compose-observability.yml logs > observability_logs_$(date +%Y%m%d_%H%M%S).log

# System information
docker version && docker-compose version > system_info.txt
```

## Prevention Tips

1. **Regular Backups**: Database and configuration backups
2. **Monitoring Alerts**: Set up alerts for key metrics
3. **Resource Limits**: Configure appropriate Docker resource limits
4. **Log Rotation**: Implement log rotation to prevent disk fill
5. **Updates**: Keep Docker images and dependencies updated
6. **Documentation**: Maintain accurate configuration documentation

## Support Escalation

If issues persist after trying troubleshooting steps:

1. **Collect Information**:
   - Application and system logs
   - Docker container status
   - Configuration files
   - Error messages and stack traces

2. **Contact Support**:
   - Development team for application issues
   - DevOps team for infrastructure issues
   - Include collected information and steps attempted

3. **Severity Levels**:
   - **Critical**: System down, data loss
   - **High**: Major functionality broken
   - **Medium**: Performance degraded
   - **Low**: Minor issues, workarounds available