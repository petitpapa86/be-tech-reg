# PowerShell script to apply V44 migration
# Increases report_id column length in quality_reports table

Write-Host "Applying V44 migration: Increase report_id length" -ForegroundColor Cyan

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

if (-not $DB_HOST) { $DB_HOST = "localhost" }
if (-not $DB_PORT) { $DB_PORT = "5432" }
if (-not $DB_NAME) { $DB_NAME = "regtech_db" }
if (-not $DB_USER) { $DB_USER = "regtech_user" }

Write-Host "Database: $DB_NAME at $DB_HOST:$DB_PORT" -ForegroundColor Yellow

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $DB_PASSWORD

# Apply migration
Write-Host "Applying migration..." -ForegroundColor Green
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "regtech-app/src/main/resources/db/migration/dataquality/V44__increase_report_id_length.sql"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Migration applied successfully!" -ForegroundColor Green
} else {
    Write-Host "Migration failed!" -ForegroundColor Red
    exit 1
}

# Clear password
$env:PGPASSWORD = $null
