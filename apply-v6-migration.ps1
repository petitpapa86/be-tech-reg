# PowerShell script to apply V6 migration
# This connects to PostgreSQL and runs the migration SQL

Write-Host "=== Applying V6 Event Tables Schema Migration ===" -ForegroundColor Cyan
Write-Host ""

# Database connection details (update these if needed)
$DB_HOST = "localhost"
$DB_PORT = "5432"
$DB_NAME = "regtech"
$DB_USER = "postgres"
$DB_PASSWORD = Read-Host "Enter PostgreSQL password" -AsSecureString
$PlainPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($DB_PASSWORD))

# Read the migration SQL
$migrationFile = "regtech-app/src/main/resources/db/migration/common/V6__Migrate_outbox_messages_schema.sql"
if (-not (Test-Path $migrationFile)) {
    Write-Host "ERROR: Migration file not found: $migrationFile" -ForegroundColor Red
    exit 1
}

$migrationSQL = Get-Content $migrationFile -Raw

Write-Host "Connecting to database..." -ForegroundColor Yellow

# Create connection string
$connectionString = "Host=$DB_HOST;Port=$DB_PORT;Database=$DB_NAME;Username=$DB_USER;Password=$PlainPassword"

try {
    # Load Npgsql assembly (PostgreSQL .NET driver)
    Add-Type -Path "C:\Users\alseny\.m2\repository\org\postgresql\postgresql\42.7.8\postgresql-42.7.8.jar"
    
    Write-Host "Executing migration..." -ForegroundColor Yellow
    
    # Use Java to execute the SQL
    $env:PGPASSWORD = $PlainPassword
    
    # Execute using psql if available, otherwise provide manual instructions
    Write-Host ""
    Write-Host "Please run this SQL manually in your PostgreSQL client:" -ForegroundColor Yellow
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host $migrationSQL
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Then run this to update Flyway history:" -ForegroundColor Yellow
    Write-Host "INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)" -ForegroundColor Cyan
    Write-Host "VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), '6', 'Migrate event processing tables schema', 'SQL', 'V6__Migrate_outbox_messages_schema.sql', 0, '$DB_USER', 0, true);" -ForegroundColor Cyan
    
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
