#!/bin/bash
# test-traces.sh

echo "=== Testing OpenTelemetry Traces ==="

# Start application with tracing
cd regtech-app
OTEL_SERVICE_NAME=regtech-app \
OTEL_TRACES_EXPORTER=otlp \
OTEL_METRICS_EXPORTER=otlp \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
sleep 30

# Generate some traffic
curl -s http://localhost:8080/actuator/health > /dev/null
curl -s http://localhost:8080/api/v1/status > /dev/null 2>&1 || true

# Query Tempo for traces
TRACE_COUNT=$(curl -s "http://localhost:3200/api/search?tags=service.name%3Dregtech-app" | jq -r '.traces | length' 2>/dev/null || echo "0")

if [ "$TRACE_COUNT" -gt 0 ]; then
    echo "✅ Traces are being collected ($TRACE_COUNT traces found)"
else
    echo "❌ No traces found in Tempo"
fi

# Cleanup
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "=== Trace tests completed ==="