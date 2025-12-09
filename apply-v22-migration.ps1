# PowerShell script to apply V22 migration for invoices schema update
# This updates the invoices table to match the current domain model

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Applying V22 Migration - Invoices Schema Update" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if .env file exists
if (-Not (Test-Path ".env")) {
    Write-Host "ERROR: .env file not found!" -ForegroundColor Red
    Write-Host "Please create a .env file with your database credentials." -ForegroundColor Yellow
    exit 1
}

# Load environment variables from .env file
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*?)\s*=\s*(.*?)\s*$') {
        $name = $matches[1]
        $value = $matches[2]
        Set-Item -Path "env:$name" -Value $value
    }
}

# Database connection parameters
$DB_HOST = $env:DB_HOST
$DB_PORT = $env:DB_PORT
$DB_NAME = $env:DB_NAME
$DB_USER = $env:DB_USER
$DB_PASSWORD = $env:DB_PASSWORD

Write-Host "Database Configuration:" -ForegroundColor Yellow
Write-Host "  Host: $DB_HOST" -ForegroundColor Gray
Write-Host "  Port: $DB_PORT" -ForegroundColor Gray
Write-Host "  Database: $DB_NAME" -ForegroundColor Gray
Write-Host "  User: $DB_USER" -ForegroundColor Gray
Write-Host ""

# Set PGPASSWORD environment variable for psql
$env:PGPASSWORD = $DB_PASSWORD

# Migration file path
$MIGRATION_FILE = "regtech-app/src/main/resources/db/migration/billing/V22__Update_invoices_schema.sql"

Write-Host "Checking migration file..." -ForegroundColor Yellow
if (-Not (Test-Path $MIGRATION_FILE)) {
    Write-Host "ERROR: Migration file not found: $MIGRATION_FILE" -ForegroundColor Red
    exit 1
}
Write-Host "Migration file found: $MIGRATION_FILE" -ForegroundColor Green
Write-Host ""

# Display migration preview
Write-Host "Migration Preview:" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow
Get-Content $MIGRATION_FILE | Select-Object -First 20
Write-Host "..." -ForegroundColor Gray
Write-Host ""

# Confirm before proceeding
$confirmation = Read-Host "Do you want to apply this migration? (yes/no)"
if ($confirmation -ne "yes") {
    Write-Host "Migration cancelled." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Applying migration..." -ForegroundColor Yellow

# Apply migration using psql
$psqlCommand = "psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $MIGRATION_FILE"
Invoke-Expression $psqlCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Migration Applied Successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "The invoices table schema has been updated to match the current domain model." -ForegroundColor Green
    Write-Host ""
    Write-Host "Changes applied:" -ForegroundColor Yellow
    Write-Host "  Invoices table:" -ForegroundColor Cyan
    Write-Host "    - Removed: amount_due, currency, subscription_id columns" -ForegroundColor Gray
    Write-Host "    - Added: subscription_amount_value, subscription_amount_currency" -ForegroundColor Gray
    Write-Host "    - Added: overage_amount_value, overage_amount_currency" -ForegroundColor Gray
    Write-Host "    - Added: total_amount_value, total_amount_currency" -ForegroundColor Gray
    Write-Host "    - Added: billing_period_start_date, billing_period_end_date" -ForegroundColor Gray
    Write-Host "    - Added: invoice_number, issue_date, sent_at" -ForegroundColor Gray
    Write-Host "  Invoice Line Items table:" -ForegroundColor Cyan
    Write-Host "    - Removed: amount, currency columns" -ForegroundColor Gray
    Write-Host "    - Added: unit_amount_value, unit_amount_currency" -ForegroundColor Gray
    Write-Host "    - Added: total_amount_value, total_amount_currency" -ForegroundColor Gray
    Write-Host ""
    Write-Host "You can now restart your application." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Migration Failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check the error messages above and fix any issues." -ForegroundColor Yellow
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  - Database connection problems" -ForegroundColor Gray
    Write-Host "  - Insufficient permissions" -ForegroundColor Gray
    Write-Host "  - Migration already applied" -ForegroundColor Gray
    exit 1
}

# Clear password from environment
Remove-Item env:PGPASSWORD
