#!/bin/bash
# test-performance.sh

echo "=== Testing Performance Under Load ==="

# Start application
cd regtech-app
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
sleep 30

# Generate load
echo "Generating load for 30 seconds..."
START_TIME=$(date +%s)
END_TIME=$((START_TIME + 30))

while [ $(date +%s) -lt $END_TIME ]; do
    curl -s http://localhost:8080/actuator/health > /dev/null &
    curl -s http://localhost:8080/api/v1/status > /dev/null 2>&1 &
    sleep 0.1
done

wait

# Check metrics after load
RESPONSE_TIME=$(curl -s "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_sum{job='regtech-app'}/http_server_requests_seconds_count{job='regtech-app'}" | jq -r '.data.result[0].value[1]' 2>/dev/null)

if [ -n "$RESPONSE_TIME" ] && [ "$(echo "$RESPONSE_TIME > 0" | bc -l)" -eq 1 ]; then
    echo "✅ Performance metrics collected (avg response time: ${RESPONSE_TIME}s)"
else
    echo "❌ Performance metrics not available"
fi

# Check for errors during load
ERROR_RATE=$(curl -s "http://localhost:9090/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~\"(4|5)..\",job='regtech-app'}[5m]))" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")

if [ "$(echo "$ERROR_RATE < 0.01" | bc -l)" -eq 1 ]; then
    echo "✅ Error rate acceptable (< 1%)"
else
    echo "❌ High error rate detected: ${ERROR_RATE}"
fi

# Cleanup
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "=== Performance tests completed ==="