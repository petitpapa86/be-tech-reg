# RegTech Observability Smoke Tests - PowerShell Version
# Run this script to validate the complete observability implementation

param(
    [switch]$Verbose,
    [switch]$SkipApplicationTests
)

Write-Host "🧪 Starting RegTech Observability Smoke Tests..." -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# Test counters
$TotalTests = 0
$PassedTests = 0
$FailedTests = 0

# Function to print test results
function Write-TestResult {
    param(
        [string]$TestName,
        [bool]$Result
    )

    $script:TotalTests++
    if ($Result) {
        Write-Host "✅ $TestName PASSED" -ForegroundColor Green
        $script:PassedTests++
    } else {
        Write-Host "❌ $TestName FAILED" -ForegroundColor Red
        $script:FailedTests++
    }
}

# Function to test HTTP endpoint
function Test-HttpEndpoint {
    param(
        [string]$Url,
        [string]$ExpectedContent = "",
        [int]$Timeout = 10
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec $Timeout -UseBasicParsing
        if ($ExpectedContent -and $response.Content -notmatch $ExpectedContent) {
            return $false
        }
        return $true
    } catch {
        if ($Verbose) {
            Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        return $false
    }
}

# Function to test JSON endpoint
function Test-JsonEndpoint {
    param(
        [string]$Url,
        [string]$JsonPath,
        [string]$ExpectedValue,
        [int]$Timeout = 10
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec $Timeout -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json

        # Simple JSON path evaluation (basic implementation)
        $value = $json
        $pathParts = $JsonPath -split '\.'
        foreach ($part in $pathParts) {
            if ($part -match '(\w+)\[(\d+)\]') {
                $property = $matches[1]
                $index = [int]$matches[2]
                $value = $value.$property[$index]
            } else {
                $value = $value.$part
            }
        }

        return ($value -eq $ExpectedValue)
    } catch {
        if ($Verbose) {
            Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        return $false
    }
}

# Function to test array length
function Test-ArrayLength {
    param(
        [string]$Url,
        [string]$JsonPath,
        [int]$MinLength,
        [int]$Timeout = 10
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec $Timeout -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json

        # Simple JSON path evaluation for array length
        $value = $json
        $pathParts = $JsonPath -split '\.'
        foreach ($part in $pathParts) {
            if ($part -match '(\w+)\[(\d+)\]') {
                $property = $matches[1]
                $index = [int]$matches[2]
                $value = $value.$property[$index]
            } else {
                $value = $value.$part
            }
        }

        return ($value.Count -ge $MinLength)
    } catch {
        if ($Verbose) {
            Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        return $false
    }
}

Write-Host "🔍 Testing Infrastructure Health..." -ForegroundColor Yellow
Write-Host "-----------------------------------" -ForegroundColor Yellow

# Test 1.1: Prometheus Health
$result = Test-HttpEndpoint -Url "http://localhost:9090/-/healthy" -ExpectedContent "Prometheus is Healthy"
Write-TestResult -TestName "Prometheus Health Check" -Result $result

# Test 1.2: Grafana Health
$result = Test-JsonEndpoint -Url "http://localhost:3000/api/health" -JsonPath "database" -ExpectedValue "ok"
Write-TestResult -TestName "Grafana Health Check" -Result $result

# Test 1.3: Alertmanager Health
$result = Test-HttpEndpoint -Url "http://localhost:9093/-/healthy" -ExpectedContent "OK"
Write-TestResult -TestName "Alertmanager Health Check" -Result $result

# Test 1.4: Loki Health
$result = Test-HttpEndpoint -Url "http://localhost:3100/ready" -ExpectedContent "ready"
Write-TestResult -TestName "Loki Health Check" -Result $result

# Test 1.5: Tempo Health
$result = Test-HttpEndpoint -Url "http://localhost:3200/ready" -ExpectedContent "ready"
Write-TestResult -TestName "Tempo Health Check" -Result $result

Write-Host ""
Write-Host "📊 Testing Metrics Collection..." -ForegroundColor Yellow
Write-Host "-------------------------------" -ForegroundColor Yellow

if (-not $SkipApplicationTests) {
    # Test 2.1: Application Metrics
    $result = Test-ArrayLength -Url "http://localhost:9090/api/v1/query?query=up{job='regtech-app'}" -JsonPath "data.result" -MinLength 1
    Write-TestResult -TestName "Application Metrics Collection" -Result $result

    # Test 2.2: JVM Metrics
    $result = Test-ArrayLength -Url "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes" -JsonPath "data.result" -MinLength 1
    Write-TestResult -TestName "JVM Metrics Collection" -Result $result

    # Test 2.3: HTTP Metrics
    $result = Test-ArrayLength -Url "http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count" -JsonPath "data.result" -MinLength 1
    Write-TestResult -TestName "HTTP Metrics Collection" -Result $result
} else {
    Write-Host "⚠️  Skipping application metrics tests (-SkipApplicationTests)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🚨 Testing Alerting Configuration..." -ForegroundColor Yellow
Write-Host "-----------------------------------" -ForegroundColor Yellow

# Test 3.1: Alert Rules Loaded
$result = Test-ArrayLength -Url "http://localhost:9090/api/v1/rules" -JsonPath "data.groups" -MinLength 1
Write-TestResult -TestName "Alert Rules Loaded" -Result $result

# Test 3.2: Specific Alert Rule (ApplicationDown)
try {
    $response = Invoke-WebRequest -Uri "http://localhost:9090/api/v1/rules" -TimeoutSec 10 -UseBasicParsing
    $json = $response.Content | ConvertFrom-Json
    $alertFound = $false
    foreach ($group in $json.data.groups) {
        foreach ($rule in $group.rules) {
            if ($rule.name -eq "ApplicationDown") {
                $alertFound = $true
                break
            }
        }
        if ($alertFound) { break }
    }
    Write-TestResult -TestName "ApplicationDown Alert Rule" -Result $alertFound
} catch {
    Write-TestResult -TestName "ApplicationDown Alert Rule" -Result $false
}

# Test 3.3: Alertmanager Configuration
try {
    $response = Invoke-WebRequest -Uri "http://localhost:9093/api/v2/status" -TimeoutSec 10 -UseBasicParsing
    $config = $response.Content | ConvertFrom-Json
    $smtpConfigured = $config.config.original -match "smtp_smarthost"
    Write-TestResult -TestName "Alertmanager SMTP Configuration" -Result $smtpConfigured
} catch {
    Write-TestResult -TestName "Alertmanager SMTP Configuration" -Result $false
}

Write-Host ""
Write-Host "📈 Testing Grafana Integration..." -ForegroundColor Yellow
Write-Host "---------------------------------" -ForegroundColor Yellow

# Test 4.1: Grafana API Access
$result = Test-ArrayLength -Url "http://localhost:3000/api/dashboards" -JsonPath "." -MinLength 1
Write-TestResult -TestName "Grafana API Access" -Result $result

# Test 4.2: RegTech Dashboards
try {
    $pair = "admin:admin"
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
    $base64 = [System.Convert]::ToBase64String($bytes)
    $headers = @{ Authorization = "Basic $base64" }
    $response = Invoke-WebRequest -Uri "http://localhost:3000/api/search?query=RegTech" -Headers $headers -TimeoutSec 10 -UseBasicParsing
    $json = $response.Content | ConvertFrom-Json
    $result = $json.Count -gt 0
    Write-TestResult -TestName "RegTech Dashboards Available" -Result $result
} catch {
    Write-TestResult -TestName "RegTech Dashboards Available" -Result $false
}

# Test 4.3: Grafana Alert Rules
try {
    $pair = "admin:admin"
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
    $base64 = [System.Convert]::ToBase64String($bytes)
    $headers = @{ Authorization = "Basic $base64" }
    $result = Test-ArrayLength -Url "http://localhost:3000/api/v1/provisioning/alert-rules" -JsonPath "." -MinLength 1
    Write-TestResult -TestName "Grafana Alert Rules Configured" -Result $result
} catch {
    Write-TestResult -TestName "Grafana Alert Rules Configured" -Result $false
}

# Test 4.4: Contact Points
try {
    $pair = "admin:admin"
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
    $base64 = [System.Convert]::ToBase64String($bytes)
    $headers = @{ Authorization = "Basic $base64" }
    $result = Test-ArrayLength -Url "http://localhost:3000/api/v1/provisioning/contact-points" -JsonPath "." -MinLength 1
    Write-TestResult -TestName "Contact Points Configured" -Result $result
} catch {
    Write-TestResult -TestName "Contact Points Configured" -Result $false
}

Write-Host ""
Write-Host "⚡ Testing Performance..." -ForegroundColor Yellow
Write-Host "-----------------------" -ForegroundColor Yellow

# Test 7.1: Prometheus Query Performance
$startTime = Get-Date
try {
    Invoke-WebRequest -Uri "http://localhost:9090/api/v1/query?query=up" -TimeoutSec 10 -UseBasicParsing | Out-Null
    $endTime = Get-Date
    $queryTime = ($endTime - $startTime).TotalMilliseconds
    $result = $queryTime -lt 2000
    Write-TestResult -TestName "Prometheus Query Performance (< 2s)" -Result $result
} catch {
    Write-TestResult -TestName "Prometheus Query Performance (< 2s)" -Result $false
}

Write-Host ""
Write-Host "📊 Test Summary" -ForegroundColor Blue
Write-Host "==============" -ForegroundColor Blue
Write-Host "Total Tests: $TotalTests" -ForegroundColor Blue
Write-Host "Passed: $PassedTests" -ForegroundColor Green
Write-Host "Failed: $FailedTests" -ForegroundColor Red

if ($FailedTests -eq 0) {
    Write-Host ""
    Write-Host "🎉 All automated tests PASSED!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Next Steps:" -ForegroundColor Cyan
    Write-Host "1. Start the RegTech application to generate real metrics/traces/logs" -ForegroundColor White
    Write-Host "2. Run manual tests for dashboard visualization" -ForegroundColor White
    Write-Host "3. Test alert notifications with real credentials" -ForegroundColor White
    Write-Host "4. Perform load testing to verify performance under stress" -ForegroundColor White
    Write-Host ""
    Write-Host "✅ Observability implementation is fully functional!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "❌ Some tests failed. Check the observability stack configuration." -ForegroundColor Red
    Write-Host ""
    Write-Host "🔧 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "1. Ensure all Docker services are running: docker-compose -f docker-compose-observability.yml ps" -ForegroundColor White
    Write-Host "2. Check service logs: docker-compose -f docker-compose-observability.yml logs <service>" -ForegroundColor White
    Write-Host "3. Verify network connectivity between services" -ForegroundColor White
    Write-Host "4. Check configuration files for syntax errors" -ForegroundColor White
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "🧪 Observability Smoke Tests Completed!" -ForegroundColor Cyan