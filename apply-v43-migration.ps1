#!/usr/bin/env pwsh
# PowerShell script to apply V43 migration for quality reports tables

Write-Host "========================================"
Write-Host "Applying V43 Migration"
Write-Host "Creating quality_reports and quality_error_summaries tables"
Write-Host "========================================"
Write-Host ""

# Check if Maven is available
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Maven is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Maven and try again"
    exit 1
}

Write-Host "Running Flyway migration..."
Write-Host ""

Push-Location regtech-app
mvn flyway:migrate
Pop-Location

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Migration V43 applied successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "ERROR: Migration failed!" -ForegroundColor Red
    Write-Host "Please check the error messages above" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}
