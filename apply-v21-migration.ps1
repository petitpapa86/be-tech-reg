# Apply V21 migration to fix subscriptions table schema
# This script applies the V21 migration SQL directly to PostgreSQL

Write-Host "========================================"
Write-Host "V21 Subscriptions Schema Migration"
Write-Host "========================================"
Write-Host ""
Write-Host "This will:"
Write-Host "1. Drop and recreate billing.subscriptions table"
Write-Host "2. Update Flyway history to record V21"
Write-Host ""
Write-Host "WARNING: This will DELETE all existing subscription data!" -ForegroundColor Red
Write-Host ""

# Load database connection from .env file
$envFile = ".env"
$dbConfig = @{
    DB_HOST = "localhost"
    DB_PORT = "5432"
    DB_NAME = "regtech_db"
    DB_USER = "postgres"
    DB_PASSWORD = "postgres"
}

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^([^=]+)=(.*)$") {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            if ($dbConfig.ContainsKey($key)) {
                $dbConfig[$key] = $value
            }
        }
    }
}

Write-Host "Database: $($dbConfig.DB_NAME) on $($dbConfig.DB_HOST):$($dbConfig.DB_PORT)"
Write-Host "User: $($dbConfig.DB_USER)"
Write-Host ""

$confirm = Read-Host "Type 'yes' to continue"
if ($confirm -ne "yes") {
    Write-Host "Migration cancelled."
    exit 1
}

Write-Host ""
Write-Host "Applying V21 migration..."
Write-Host ""

# Set PGPASSWORD environment variable for psql
$env:PGPASSWORD = $dbConfig.DB_PASSWORD

# Apply the migration
$migrationFile = "regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql"
$psqlArgs = @(
    "-h", $dbConfig.DB_HOST,
    "-p", $dbConfig.DB_PORT,
    "-U", $dbConfig.DB_USER,
    "-d", $dbConfig.DB_NAME,
    "-f", $migrationFile
)

& psql @psqlArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Migration failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure:"
    Write-Host "1. PostgreSQL is running"
    Write-Host "2. Database credentials are correct in .env file"
    Write-Host "3. psql command is available in PATH"
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Updating Flyway history..."
Write-Host ""

# Update Flyway history
$flywayUpdate = "INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), '21', 'Update subscriptions schema', 'SQL', 'V21__Update_subscriptions_schema.sql', NULL, CURRENT_USER, CURRENT_TIMESTAMP, 0, true);"

$psqlArgs = @(
    "-h", $dbConfig.DB_HOST,
    "-p", $dbConfig.DB_PORT,
    "-U", $dbConfig.DB_USER,
    "-d", $dbConfig.DB_NAME,
    "-c", $flywayUpdate
)

& psql @psqlArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "WARNING: Failed to update Flyway history!" -ForegroundColor Yellow
    Write-Host "The migration was applied but Flyway doesn't know about it."
    Write-Host "You may need to manually update flyway_schema_history table."
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "Migration V21 applied successfully!" -ForegroundColor Green
Write-Host "========================================"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Restart your application"
Write-Host "2. Test user registration"
Write-Host "3. Apply V6 migration for outbox_messages (see APPLY_MIGRATION_NOW.md)"
Write-Host ""

# Clear password from environment
Remove-Item Env:\PGPASSWORD

Read-Host "Press Enter to continue"
