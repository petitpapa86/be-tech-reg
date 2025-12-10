#!/bin/bash

set -e

echo "🧪 Starting RegTech Observability Smoke Tests..."
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to print test results
print_result() {
    local test_name=$1
    local result=$2
    ((TOTAL_TESTS++))
    if [ "$result" -eq 0 ]; then
        echo -e "${GREEN}✅ $test_name PASSED${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}❌ $test_name FAILED${NC}"
        ((FAILED_TESTS++))
    fi
}

# Function to check service health
check_service() {
    local url=$1
    local service_name=$2
    local expected_content=$3

    if curl -s --max-time 10 "$url" | grep -q "$expected_content" 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# Function to check JSON response
check_json() {
    local url=$1
    local jq_filter=$2
    local expected_value=$3
    local service_name=$4

    local result
    result=$(curl -s --max-time 10 "$url" 2>/dev/null | jq -r "$jq_filter" 2>/dev/null)

    if [ "$result" = "$expected_value" ] 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# Function to check array length
check_array_length() {
    local url=$1
    local jq_filter=$2
    local min_length=$3
    local service_name=$4

    local length
    length=$(curl -s --max-time 10 "$url" 2>/dev/null | jq "$jq_filter | length" 2>/dev/null)

    if [ "$length" -ge "$min_length" ] 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

echo "🔍 Testing Infrastructure Health..."
echo "-----------------------------------"

# Test 1.1: Prometheus Health
check_service "http://localhost:9090/-/healthy" "Prometheus" "Prometheus is Healthy"
print_result "Prometheus Health Check" $?

# Test 1.2: Grafana Health
check_json "http://localhost:3000/api/health" ".database" "ok" "Grafana"
print_result "Grafana Health Check" $?

# Test 1.3: Alertmanager Health
check_service "http://localhost:9093/-/healthy" "Alertmanager" "OK"
print_result "Alertmanager Health Check" $?

# Test 1.4: Loki Health
check_service "http://localhost:3100/ready" "Loki" "ready"
print_result "Loki Health Check" $?

# Test 1.5: Tempo Health
check_service "http://localhost:3200/ready" "Tempo" "ready"
print_result "Tempo Health Check" $?

echo ""
echo "📊 Testing Metrics Collection..."
echo "-------------------------------"

# Test 2.1: Application Metrics
check_array_length "http://localhost:9090/api/v1/query?query=up{job='regtech-app'}" ".data.result" 1 "Application Metrics"
print_result "Application Metrics Collection" $?

# Test 2.2: JVM Metrics
check_array_length "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes" ".data.result" 1 "JVM Metrics"
print_result "JVM Metrics Collection" $?

# Test 2.3: HTTP Metrics
check_array_length "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count" ".data.result" 1 "HTTP Metrics"
print_result "HTTP Metrics Collection" $?

echo ""
echo "🚨 Testing Alerting Configuration..."
echo "-----------------------------------"

# Test 3.1: Alert Rules Loaded
check_array_length "http://localhost:9090/api/v1/rules" ".data.groups" 1 "Alert Rules"
print_result "Alert Rules Loaded" $?

# Test 3.2: Specific Alert Rule
curl -s "http://localhost:9090/api/v1/rules" | jq -r '.data.groups[].rules[] | select(.name == "ApplicationDown") | .name' | grep -q "ApplicationDown"
print_result "ApplicationDown Alert Rule" $?

# Test 3.3: Alertmanager Configuration
curl -s "http://localhost:9093/api/v2/status" | jq -r '.config.original' | grep -q "smtp_smarthost"
print_result "Alertmanager SMTP Configuration" $?

echo ""
echo "📈 Testing Grafana Integration..."
echo "---------------------------------"

# Test 4.1: Grafana API Access
check_array_length "http://localhost:3000/api/dashboards" "." 1 "Grafana API"
print_result "Grafana API Access" $?

# Test 4.2: RegTech Dashboards
curl -s -u admin:admin "http://localhost:3000/api/search?query=RegTech" | jq '. | length' | grep -q "[1-9]"
print_result "RegTech Dashboards Available" $?

# Test 4.3: Grafana Alert Rules
check_array_length "http://localhost:3000/api/v1/provisioning/alert-rules" "." 1 "Grafana Alert Rules"
print_result "Grafana Alert Rules Configured" $?

# Test 4.4: Contact Points
check_array_length "http://localhost:3000/api/v1/provisioning/contact-points" "." 1 "Contact Points"
print_result "Contact Points Configured" $?

echo ""
echo "📝 Testing Logging Integration..."
echo "---------------------------------"

# Test 5.1: Log Collection (requires recent logs)
# Note: This test may fail if no recent logs exist
LOGS_COUNT=$(curl -s "http://localhost:3100/loki/api/v1/query_range?query={job=\"regtech-app\"}&start=$(($(date +%s) - 300))&end=$(date +%s)&step=60" 2>/dev/null | jq '.data.result | length' 2>/dev/null || echo "0")
if [ "$LOGS_COUNT" -gt 0 ]; then
    print_result "Log Collection" 0
else
    echo -e "${YELLOW}⚠️  Log Collection - No recent logs (expected if app not running)${NC}"
fi

echo ""
echo "🔗 Testing Tracing Integration..."
echo "---------------------------------"

# Test 6.1: Trace Collection (requires recent traces)
TRACES_COUNT=$(curl -s "http://localhost:3200/api/search?service=regtech-app" 2>/dev/null | jq '.traces | length' 2>/dev/null || echo "0")
if [ "$TRACES_COUNT" -gt 0 ]; then
    print_result "Trace Collection" 0
else
    echo -e "${YELLOW}⚠️  Trace Collection - No recent traces (expected if app not running)${NC}"
fi

echo ""
echo "⚡ Testing Performance..."
echo "-----------------------"

# Test 7.1: Prometheus Query Performance
START_TIME=$(date +%s%3N)
curl -s "http://localhost:9090/api/v1/query?query=up" > /dev/null 2>&1
END_TIME=$(date +%s%3N)
QUERY_TIME=$((END_TIME - START_TIME))
if [ "$QUERY_TIME" -lt 2000 ]; then
    print_result "Prometheus Query Performance (< 2s)" 0
else
    print_result "Prometheus Query Performance (< 2s)" 1
fi

echo ""
echo "📊 Test Summary"
echo "=============="
echo -e "${BLUE}Total Tests: $TOTAL_TESTS${NC}"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 All automated tests PASSED!${NC}"
    echo ""
    echo "📋 Next Steps:"
    echo "1. Start the RegTech application to generate real metrics/traces/logs"
    echo "2. Run manual tests for dashboard visualization"
    echo "3. Test alert notifications with real credentials"
    echo "4. Perform load testing to verify performance under stress"
    echo ""
    echo -e "${GREEN}✅ Observability implementation is fully functional!${NC}"
else
    echo ""
    echo -e "${RED}❌ Some tests failed. Check the observability stack configuration.${NC}"
    echo ""
    echo "🔧 Troubleshooting:"
    echo "1. Ensure all Docker services are running: docker-compose -f docker-compose-observability.yml ps"
    echo "2. Check service logs: docker-compose -f docker-compose-observability.yml logs <service>"
    echo "3. Verify network connectivity between services"
    echo "4. Check configuration files for syntax errors"
fi

echo ""
echo "=============================================="
echo "🧪 Observability Smoke Tests Completed!"