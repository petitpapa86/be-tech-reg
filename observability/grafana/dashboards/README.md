# Grafana Dashboards

This directory contains Grafana dashboard JSON files that are automatically provisioned when the observability stack starts.

## Directory Structure

```
dashboards/
├── system/          # Infrastructure and observability stack dashboards
├── business/        # Business metrics and domain-specific dashboards
└── sla/            # SLA monitoring and compliance dashboards
```

## Dashboard Categories

### System Dashboards (`system/`)

Infrastructure and observability stack monitoring:

- **Observability Stack Health**: Health and performance of Tempo, Loki, Prometheus, Grafana
- **Application Infrastructure**: JVM metrics, thread pools, connection pools
- **Database Performance**: Query performance, connection usage, transaction metrics
- **Network and I/O**: Network traffic, disk I/O, file system usage

### Business Dashboards (`business/`)

Domain-specific business metrics and KPIs:

- **IAM Module**: Authentication rates, user registrations, session management
- **Billing Module**: Payment processing, subscription metrics, revenue tracking
- **Ingestion Module**: Batch processing rates, file uploads, data validation
- **Data Quality Module**: Quality scores, rule violations, validation metrics
- **Risk Calculation Module**: Calculation throughput, exposure metrics, concentration indices
- **Report Generation Module**: Report generation rates, template usage, delivery metrics

### SLA Dashboards (`sla/`)

Service level agreement tracking and compliance:

- **API Performance SLA**: Response times, throughput, error rates by endpoint
- **Batch Processing SLA**: Completion times, processing windows, SLA compliance
- **Service Availability**: Uptime percentages, downtime incidents, availability trends
- **Capacity Planning**: Resource utilization, bottleneck identification, growth trends

## Creating Dashboards

### Method 1: Create in Grafana UI

1. Open Grafana (http://localhost:3000)
2. Create a new dashboard
3. Add panels with queries
4. Save the dashboard
5. Export as JSON (Share → Export → Save to file)
6. Copy JSON file to appropriate directory

### Method 2: Use Dashboard Templates

See the `templates/` directory for example dashboard JSON files that can be customized.

## Dashboard Best Practices

### 1. Use Variables

Add template variables for dynamic filtering:

```json
{
  "templating": {
    "list": [
      {
        "name": "environment",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(deployment_environment)"
      },
      {
        "name": "module",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(module)"
      }
    ]
  }
}
```

### 2. Add Documentation Panels

Include text panels with:
- Dashboard purpose and scope
- Key metrics explanations
- Links to runbooks and documentation
- Alert thresholds and SLA targets

### 3. Set Appropriate Refresh Intervals

- Real-time monitoring: 5-10 seconds
- Operational dashboards: 30-60 seconds
- Historical analysis: 5-15 minutes

### 4. Use Consistent Naming

- Prefix with category: `[System]`, `[Business]`, `[SLA]`
- Include module name: `[Business] Risk Calculation Metrics`
- Use descriptive titles: `API Response Time by Endpoint`

### 5. Link Related Dashboards

Add dashboard links for easy navigation:

```json
{
  "links": [
    {
      "title": "System Health",
      "url": "/d/system-health",
      "type": "dashboards"
    }
  ]
}
```

## Example Dashboard Queries

### Prometheus Queries

**Request rate by module:**
```promql
sum(rate(http_server_requests_seconds_count{module="$module"}[5m])) by (module, uri)
```

**Error rate percentage:**
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
/ 
sum(rate(http_server_requests_seconds_count[5m])) * 100
```

**P95 response time:**
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)
```

### Tempo Queries

**Trace search by service:**
```
{service.name="bcbs239-platform"}
```

**Trace search by duration:**
```
{duration>1s}
```

### Loki Queries

**Error logs:**
```logql
{service_name="bcbs239-platform"} |= "ERROR"
```

**Logs with trace context:**
```logql
{service_name="bcbs239-platform"} | json | traceId != ""
```

## Maintenance

### Updating Dashboards

1. Edit dashboard in Grafana UI
2. Export updated JSON
3. Replace file in appropriate directory
4. Grafana will reload automatically (within 10 seconds)

### Version Control

- Commit dashboard JSON files to version control
- Include meaningful commit messages describing changes
- Review dashboard changes in pull requests

### Backup

Dashboard JSON files are automatically backed up with the Grafana data volume. For additional safety:

1. Commit to version control
2. Export regularly to external storage
3. Include in Hetzner Object Storage backups

## Troubleshooting

### Dashboard Not Loading

1. Check JSON syntax: `jq . dashboard.json`
2. Verify file permissions: `chmod 644 dashboard.json`
3. Check Grafana logs: `docker logs grafana`
4. Verify provisioning configuration in `provisioning/dashboards/dashboards.yaml`

### Queries Not Working

1. Verify data source connection in Grafana UI
2. Test query in Explore view
3. Check metric names and labels
4. Verify time range and refresh interval

### Performance Issues

1. Reduce query complexity
2. Increase refresh interval
3. Limit time range
4. Use recording rules in Prometheus for expensive queries

## Resources

- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Prometheus Query Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Loki Query Language](https://grafana.com/docs/loki/latest/logql/)
- [Tempo Query Language](https://grafana.com/docs/tempo/latest/traceql/)
