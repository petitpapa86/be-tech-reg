#!/usr/bin/env pwsh
# Apply V45 migration to fix quality_grade column length

Write-Host "Applying V45 migration: increase quality_grade column length..." -ForegroundColor Cyan
Write-Host ""

# Load environment variables from .env file
if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

# Set default values if not in .env
$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "regtech_db" }
$DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "regtech_user" }
$DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "regtech_password" }

Write-Host "Database: $DB_HOST:$DB_PORT/$DB_NAME"
Write-Host "User: $DB_USER"
Write-Host ""

# Set password environment variable for psql
$env:PGPASSWORD = $DB_PASSWORD

# Execute migration
$migrationFile = "regtech-app/src/main/resources/db/migration/dataquality/V45__increase_quality_grade_length.sql"

try {
    psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $migrationFile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ Migration V45 applied successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "The quality_grade column has been increased to VARCHAR(20)."
    } else {
        Write-Host ""
        Write-Host "✗ Migration V45 failed!" -ForegroundColor Red
        Write-Host "Please check the error messages above."
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "✗ Error executing migration: $_" -ForegroundColor Red
    exit 1
} finally {
    # Clear password from environment
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
