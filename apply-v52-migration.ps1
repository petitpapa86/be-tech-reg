#!/usr/bin/env pwsh
# PowerShell script to apply V52 migration

Write-Host "Applying V52 migration: Increase counterparty_lei length" -ForegroundColor Cyan

# Load environment variables
if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

$DB_HOST = $env:DB_HOST
$DB_PORT = $env:DB_PORT
$DB_NAME = $env:DB_NAME
$DB_USER = $env:DB_USER
$DB_PASSWORD = $env:DB_PASSWORD

Write-Host "Database: $DB_NAME at $DB_HOST:$DB_PORT" -ForegroundColor Yellow

# Set PGPASSWORD for psql
$env:PGPASSWORD = $DB_PASSWORD

# Apply migration
Write-Host "Executing migration..." -ForegroundColor Green
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "regtech-app/src/main/resources/db/migration/riskcalculation/V52__Increase_counterparty_lei_length.sql"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Migration V52 applied successfully!" -ForegroundColor Green
} else {
    Write-Host "Migration V52 failed!" -ForegroundColor Red
    exit 1
}
