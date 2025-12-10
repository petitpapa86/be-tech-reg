#!/bin/bash
# test-configs.sh

echo "=== Validating Configuration Files ==="

# Validate YAML syntax
configs=("prometheus.yml" "alertmanager.yml" "alert_rules.yml" "docker-compose-observability.yml")
for config in "${configs[@]}"; do
    if [ -f "observability/$config" ]; then
        python3 -c "import yaml; yaml.safe_load(open('observability/$config'))" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "✅ $config syntax is valid"
        else
            echo "❌ $config has invalid syntax"
            exit 1
        fi
    else
        echo "❌ $config not found"
        exit 1
    fi
done

# Validate Grafana provisioning
grafana_configs=("datasources.yml" "dashboards.yml" "alerting/contact-points.yml" "alerting/notification-policies.yml")
for config in "${grafana_configs[@]}"; do
    if [ -f "observability/grafana/provisioning/$config" ]; then
        python3 -c "import yaml; yaml.safe_load(open('observability/grafana/provisioning/$config'))" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "✅ Grafana $config syntax is valid"
        else
            echo "❌ Grafana $config has invalid syntax"
            exit 1
        fi
    else
        echo "❌ Grafana $config not found"
        exit 1
    fi
done

echo "=== Configuration validation completed ==="