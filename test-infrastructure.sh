#!/bin/bash
# test-infrastructure.sh

echo "=== Testing Observability Infrastructure ==="

# Check if all services are running
services=("prometheus" "grafana" "tempo" "loki" "alertmanager" "otel-collector")
for service in "${services[@]}"; do
    if docker ps | grep -q "$service"; then
        echo "✅ $service is running"
    else
        echo "❌ $service is NOT running"
        exit 1
    fi
done

# Test service health endpoints
echo "Testing service endpoints..."
curl -f http://localhost:9090/-/healthy && echo "✅ Prometheus healthy" || echo "❌ Prometheus unhealthy"
curl -f http://localhost:3000/api/health && echo "✅ Grafana healthy" || echo "❌ Grafana unhealthy"
curl -f http://localhost:9093/-/healthy && echo "✅ Alertmanager healthy" || echo "❌ Alertmanager unhealthy"

echo "=== Infrastructure tests completed ==="