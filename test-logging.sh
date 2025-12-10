#!/bin/bash
# test-logging.sh

echo "=== Testing Log Collection ==="

# Start application
cd regtech-app
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
sleep 30

# Generate some logs
curl -s http://localhost:8080/actuator/health > /dev/null
curl -s http://localhost:8080/api/v1/status > /dev/null 2>&1 || true

# Query Loki for logs
LOG_COUNT=$(curl -s -G "http://localhost:3100/loki/api/v1/query_range" \
    --data-urlencode "query={job=\"regtech-app\"}" \
    --data-urlencode "start=$(date -d '5 minutes ago' +%s)" \
    --data-urlencode "end=$(date +%s)" | jq -r '.data.result[0].values | length' 2>/dev/null || echo "0")

if [ "$LOG_COUNT" -gt 0 ]; then
    echo "✅ Logs are being collected ($LOG_COUNT log entries found)"
else
    echo "❌ No logs found in Loki"
fi

# Test structured logging
STRUCTURED_LOGS=$(curl -s -G "http://localhost:3100/loki/api/v1/query" \
    --data-urlencode "query={job=\"regtech-app\"} | json" | jq -r '.data.result[0].stream' 2>/dev/null || echo "{}")

if echo "$STRUCTURED_LOGS" | grep -q "level"; then
    echo "✅ Structured logging is working"
else
    echo "❌ Structured logging not detected"
fi

# Cleanup
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "=== Logging tests completed ==="