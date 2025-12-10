#!/bin/bash
# test-metrics-collection.sh

echo "=== Testing Metrics Collection ==="

# Start the application (run in background)
echo "Starting RegTech application..."
cd regtech-app
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
sleep 30  # Wait for application to start

# Test application health
curl -f http://localhost:8080/actuator/health && echo "✅ Application is healthy" || echo "❌ Application is unhealthy"

# Test metrics endpoint
METRICS=$(curl -s http://localhost:8080/actuator/prometheus)
if echo "$METRICS" | grep -q "jvm_memory_used_bytes"; then
    echo "✅ JVM metrics are being collected"
else
    echo "❌ JVM metrics not found"
fi

if echo "$METRICS" | grep -q "http_server_requests_seconds"; then
    echo "✅ HTTP metrics are being collected"
else
    echo "❌ HTTP metrics not found"
fi

# Test Prometheus metrics ingestion
PROMETHEUS_METRICS=$(curl -s "http://localhost:9090/api/v1/query?query=up{job='regtech-app'}")
if echo "$PROMETHEUS_METRICS" | grep -q '"value"'; then
    echo "✅ Application metrics ingested by Prometheus"
else
    echo "❌ Application metrics not ingested by Prometheus"
fi

# Cleanup
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "=== Metrics collection tests completed ==="