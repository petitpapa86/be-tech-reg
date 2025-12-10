#!/bin/bash
# test-integration.sh

echo "=== Running End-to-End Integration Test ==="

# Start all services
echo "Starting observability stack..."
docker-compose -f docker-compose-observability.yml up -d
sleep 30

# Start application
cd regtech-app
OTEL_SERVICE_NAME=regtech-app \
OTEL_TRACES_EXPORTER=otlp \
OTEL_METRICS_EXPORTER=otlp \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
sleep 60  # Wait for full initialization

# Generate comprehensive test data
echo "Generating test data..."

# Health checks
curl -s http://localhost:8080/actuator/health > /dev/null
echo "Health check completed"

# API calls
for i in {1..10}; do
    curl -s "http://localhost:8080/api/v1/status" > /dev/null 2>&1 || true
    sleep 0.5
done
echo "API calls completed"

# Simulate some errors (if endpoint exists)
curl -s "http://localhost:8080/api/v1/error-test" > /dev/null 2>&1 || true

# Wait for metrics to be processed
sleep 30

# Validate complete pipeline
echo "Validating observability pipeline..."

# 1. Metrics in Prometheus
METRICS_COUNT=$(curl -s "http://localhost:9090/api/v1/query?query=up{job='regtech-app'}" | jq -r '.data.result | length' 2>/dev/null || echo "0")
[ "$METRICS_COUNT" -gt 0 ] && echo "✅ Metrics pipeline working" || echo "❌ Metrics pipeline failed"

# 2. Traces in Tempo
TRACE_COUNT=$(curl -s "http://localhost:3200/api/search?tags=service.name%3Dregtech-app" | jq -r '.traces | length' 2>/dev/null || echo "0")
[ "$TRACE_COUNT" -gt 0 ] && echo "✅ Tracing pipeline working" || echo "❌ Tracing pipeline failed"

# 3. Logs in Loki
LOG_COUNT=$(curl -s -G "http://localhost:3100/loki/api/v1/query_range" \
    --data-urlencode "query={job=\"regtech-app\"}" \
    --data-urlencode "start=$(date -d '5 minutes ago' +%s)" \
    --data-urlencode "end=$(date +%s)" | jq -r '.data.result[0].values | length' 2>/dev/null || echo "0")
[ "$LOG_COUNT" -gt 0 ] && echo "✅ Logging pipeline working" || echo "❌ Logging pipeline failed"

# 4. Dashboards accessible
DASHBOARD_CHECK=$(curl -s "http://localhost:3000/api/health" | jq -r '.database // empty' 2>/dev/null || echo "failed")
[ "$DASHBOARD_CHECK" = "ok" ] && echo "✅ Dashboard pipeline working" || echo "❌ Dashboard pipeline failed"

# 5. Alert rules loaded
ALERT_COUNT=$(curl -s "http://localhost:9090/api/v1/rules" | jq -r '.data.groups | length' 2>/dev/null || echo "0")
[ "$ALERT_COUNT" -gt 0 ] && echo "✅ Alerting pipeline working" || echo "❌ Alerting pipeline failed"

# Cleanup
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null
docker-compose -f docker-compose-observability.yml down

echo "=== End-to-end integration test completed ==="