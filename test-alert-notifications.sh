#!/bin/bash
# test-alert-notifications.sh

echo "=== Testing Alert Notifications ==="

# Send test alert to Alertmanager
curl -X POST http://localhost:9093/api/v2/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "TestAlert",
        "severity": "warning",
        "service": "test"
      },
      "annotations": {
        "summary": "Test alert for smoke testing",
        "description": "This is a test alert to validate notification channels"
      }
    }
  ]' && echo "✅ Test alert sent to Alertmanager"

# Check active alerts
sleep 5
ACTIVE_ALERTS=$(curl -s "http://localhost:9093/api/v2/alerts" | jq -r '.[] | select(.labels.alertname=="TestAlert") | .labels.alertname' 2>/dev/null)

if [ "$ACTIVE_ALERTS" = "TestAlert" ]; then
    echo "✅ Test alert is active in Alertmanager"
else
    echo "❌ Test alert not found in Alertmanager"
fi

# Test Grafana alert state (requires manual verification)
echo "📋 Manual verification needed:"
echo "  1. Check Grafana UI: http://localhost:3000/alerting/list"
echo "  2. Verify alert rules are in 'Normal' state"
echo "  3. Check notification channels are configured"

echo "=== Alert notification tests completed ==="