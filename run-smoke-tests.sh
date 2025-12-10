#!/bin/bash
# run-smoke-tests.sh

echo "🚀 Starting RegTech Observability Smoke Tests"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

run_test() {
    local test_name=$1
    local test_script=$2

    echo -e "\n${YELLOW}Running: $test_name${NC}"
    ((TOTAL_TESTS++))

    if bash "$test_script"; then
        echo -e "${GREEN}✅ PASSED: $test_name${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}❌ FAILED: $test_name${NC}"
        ((FAILED_TESTS++))
    fi
}

# Run all tests
run_test "Infrastructure Health Check" "test-infrastructure.sh"
run_test "Configuration Validation" "test-configs.sh"
run_test "Metrics Collection" "test-metrics-collection.sh"
run_test "OpenTelemetry Traces" "test-traces.sh"
run_test "Logging Collection" "test-logging.sh"
run_test "Dashboard Accessibility" "test-dashboards.sh"
run_test "Alert Rules" "test-alerts.sh"
run_test "Alert Notifications" "test-alert-notifications.sh"
run_test "Performance Under Load" "test-performance.sh"
run_test "End-to-End Integration" "test-integration.sh"

# Summary
echo -e "\n=============================================="
echo -e "🧪 Smoke Test Results Summary"
echo -e "=============================================="
echo -e "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All smoke tests PASSED! Observability implementation is ready for production.${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️  Some tests FAILED. Please review the issues above before deploying to production.${NC}"
    exit 1
fi