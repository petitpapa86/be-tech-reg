#!/bin/bash
# test-dashboards.sh

echo "=== Testing Grafana Dashboards ==="

# Test Grafana API access
GRAFANA_API_KEY=${GRAFANA_API_KEY:-"your-api-key-here"}

# Get dashboard list
DASHBOARDS=$(curl -s -H "Authorization: Bearer $GRAFANA_API_KEY" \
    "http://localhost:3000/api/search?type=dash-db" | jq -r '.[].title' 2>/dev/null)

if echo "$DASHBOARDS" | grep -q "RegTech"; then
    echo "✅ RegTech dashboards are loaded"
    echo "Found dashboards:"
    echo "$DASHBOARDS" | grep "RegTech"
else
    echo "❌ RegTech dashboards not found"
fi

# Test specific dashboard panels
DASHBOARD_UID="regtech-overview"
PANELS=$(curl -s -H "Authorization: Bearer $GRAFANA_API_KEY" \
    "http://localhost:3000/api/dashboards/uid/$DASHBOARD_UID" | jq -r '.dashboard.panels[].title' 2>/dev/null)

if [ -n "$PANELS" ]; then
    echo "✅ Dashboard panels are configured"
    echo "Panel count: $(echo "$PANELS" | wc -l)"
else
    echo "❌ Dashboard panels not found"
fi

echo "=== Dashboard tests completed ==="