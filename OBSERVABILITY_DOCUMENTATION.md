# RegTech Platform Observability Documentation

## Overview

This documentation covers the comprehensive observability implementation for the RegTech platform, including metrics collection, distributed tracing, centralized logging, and visualization dashboards.

## Architecture

### Components

- **OpenTelemetry Collector**: Receives and processes telemetry data from applications
- **Prometheus**: Time-series metrics database and collection system
- **Tempo**: Distributed tracing backend for request correlation
- **Loki**: Log aggregation and querying system
- **Grafana**: Visualization and alerting platform

### Data Flow

```
Applications → OpenTelemetry Collector → [Prometheus, Tempo, Loki] → Grafana
```

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for applications)
- PostgreSQL database
- At least 8GB RAM, 4 CPU cores recommended

### Environment Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd regtech-platform
   ```

2. **Set environment variables**:
   ```bash
   export GRAFANA_ADMIN_USER=admin
   export GRAFANA_ADMIN_PASSWORD=secure-password-here
   export GRAFANA_ROOT_URL=http://localhost:3000
   ```

3. **Start the observability stack**:
   ```bash
   docker-compose -f docker-compose-observability.yml up -d
   ```

4. **Verify services are running**:
   ```bash
   docker-compose -f docker-compose-observability.yml ps
   ```

### Access URLs

- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Tempo**: http://localhost:3200
- **Loki**: http://localhost:3100
- **OpenTelemetry Collector**: http://localhost:8888/metrics

## Application Instrumentation

### Spring Boot Configuration

Applications are instrumented using Micrometer and OpenTelemetry:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus: true
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

### @Observed Annotations

All controller methods are instrumented with `@Observed`:

```java
@RestController
@RequestMapping("/api/v1/data-quality")
@Observed(name = "data-quality.api", contextualName = "data-quality-api")
public class DataQualityController {

    @GetMapping("/reports/{id}")
    @Observed(name = "data-quality.api.report.get",
              contextualName = "get-report",
              lowCardinalityKeyValues = {"operation", "get"})
    public ResponseEntity<Report> getReport(@PathVariable Long id) {
        // implementation
    }
}
```

## Dashboards

### Available Dashboards

1. **Operations Overview** - Primary dashboard for daily monitoring
2. **System Observability Health** - Infrastructure and stack monitoring
3. **Business API Performance** - API performance by module
4. **SLA API Performance** - Service level agreement compliance
5. **Business Metrics** - Key business KPIs and metrics

### Dashboard Navigation

- All dashboards are automatically loaded via provisioning
- Access via Grafana sidebar → Dashboards → RegTech folder
- Use time range picker to adjust analysis windows
- Filter by module using dashboard variables

## Monitoring Metrics

### HTTP Metrics

- `http_server_requests_seconds_count` - Total request count
- `http_server_requests_seconds_sum` - Total response time
- `http_server_requests_seconds_bucket` - Response time histogram buckets

### JVM Metrics

- `jvm_memory_used_bytes` - Memory usage by memory pool
- `jvm_gc_pause_seconds` - Garbage collection pause times
- `jvm_threads_live` - Active thread count

### Business Metrics

- `regtech_risk_assessments_total` - Risk assessment operations
- `regtech_reports_generated_total` - Report generation count
- `regtech_business_transactions_total` - Business transaction count

## Alerting

### Recommended Alerts

#### Critical (Immediate Response)
- API Availability < 99.9%
- Error Rate > 5%
- P95 Response Time > 2 seconds
- Service Down (any component)

#### Warning (Investigation Required)
- API Availability < 99.95%
- Error Rate > 1%
- P95 Response Time > 1 second
- Memory Usage > 85%

#### Info (Monitoring)
- Business transaction failures
- Data quality score degradation
- High-risk category detections

### Alert Configuration

Alerts can be configured in Grafana:
1. Navigate to Alerting → Alert Rules
2. Create new alert rule with Prometheus queries
3. Set thresholds and notification channels

## Troubleshooting

### Common Issues

#### Services Not Starting

**Symptoms**: Docker containers fail to start
**Solution**:
```bash
# Check container logs
docker-compose -f docker-compose-observability.yml logs <service-name>

# Restart specific service
docker-compose -f docker-compose-observability.yml restart <service-name>
```

#### No Metrics Appearing

**Symptoms**: Dashboards show no data
**Solution**:
1. Verify application is running with observability enabled
2. Check OpenTelemetry Collector logs
3. Verify Prometheus targets are healthy
4. Check network connectivity between services

#### High Memory Usage

**Symptoms**: Services consuming excessive memory
**Solution**:
1. Adjust JVM heap settings in application configuration
2. Increase Docker memory limits
3. Review and optimize application memory usage

### Log Analysis

#### Application Logs
```bash
# View application logs
docker-compose logs regtech-app

# Follow logs in real-time
docker-compose logs -f regtech-app
```

#### Observability Stack Logs
```bash
# View all observability logs
docker-compose -f docker-compose-observability.yml logs

# View specific service logs
docker-compose -f docker-compose-observability.yml logs grafana
```

## Maintenance

### Data Retention

- **Metrics**: 90 days (Prometheus)
- **Traces**: 7 days (Tempo)
- **Logs**: 30 days (Loki)

### Backup Strategy

1. **Database Backups**: Daily PostgreSQL dumps
2. **Metrics Data**: Prometheus data directory backup
3. **Configuration**: Git-based configuration backup

### Updates

#### Stack Updates
```bash
# Pull latest images
docker-compose -f docker-compose-observability.yml pull

# Restart with new versions
docker-compose -f docker-compose-observability.yml up -d
```

#### Application Updates
```bash
# Build new application version
./mvnw clean package -DskipTests

# Restart application
docker-compose restart regtech-app
```

## Performance Tuning

### OpenTelemetry Collector

```yaml
# otel-collector-config.yaml
processors:
  batch:
    timeout: 1s
    send_batch_size: 1024
  memory_limiter:
    limit_mib: 512
    spike_limit_mib: 128
```

### Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8888']
    scrape_interval: 15s
```

### JVM Tuning

```bash
# JVM options for production
-Xmx2g -Xms2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
```

## Security

### Network Security

- All services run on isolated Docker network
- External access only through published ports
- No direct database access from external networks

### Authentication

- Grafana secured with admin credentials
- Application endpoints protected by authentication
- Database access restricted to application services

### Data Protection

- Metrics data encrypted at rest
- Logs contain no sensitive information
- Audit trails maintained for configuration changes

## Support

### Getting Help

1. Check this documentation first
2. Review application and infrastructure logs
3. Check Grafana dashboards for system status
4. Contact development team for application-specific issues

### Escalation Paths

- **Application Issues**: Development team
- **Infrastructure Issues**: DevOps/SRE team
- **Performance Issues**: Performance engineering team
- **Security Issues**: Security team (immediate escalation)

## Appendices

### Configuration Files

- `docker-compose-observability.yml` - Observability stack definition
- `observability/otel-collector-config.yaml` - OpenTelemetry configuration
- `observability/prometheus.yml` - Prometheus configuration
- `observability/tempo.yaml` - Tempo configuration
- `observability/loki.yaml` - Loki configuration

### Metric Definitions

Complete list of all collected metrics with descriptions and units.

### Dashboard Templates

JSON templates for all Grafana dashboards with customization guidelines.