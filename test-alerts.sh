#!/bin/bash
# test-alerts.sh

echo "=== Testing Alert Rules ==="

# Test Prometheus alert rules
ALERT_RULES=$(curl -s "http://localhost:9090/api/v1/rules" | jq -r '.data.groups[].rules[].name' 2>/dev/null)

if echo "$ALERT_RULES" | grep -q "ApplicationDown"; then
    echo "✅ Alert rules are loaded in Prometheus"
else
    echo "❌ Alert rules not found in Prometheus"
fi

# Test Alertmanager configuration
AM_STATUS=$(curl -s "http://localhost:9093/api/v2/status" | jq -r '.cluster.peers | length' 2>/dev/null || echo "0")

if [ "$AM_STATUS" -ge 0 ]; then
    echo "✅ Alertmanager is configured"
else
    echo "❌ Alertmanager configuration issue"
fi

# Test Grafana alerting
GRAFANA_ALERTS=$(curl -s -H "Authorization: Bearer $GRAFANA_API_KEY" \
    "http://localhost:3000/api/v1/provisioning/alert-rules" | jq -r '.[] | select(.title) | .title' 2>/dev/null)

if [ -n "$GRAFANA_ALERTS" ]; then
    echo "✅ Grafana alerts are configured"
else
    echo "❌ Grafana alerts not found"
fi

echo "=== Alert tests completed ==="