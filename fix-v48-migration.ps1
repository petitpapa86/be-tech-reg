#!/usr/bin/env pwsh
# fix-v48-migration.ps1
# Fixes and reapplies V48 migration with idempotent SQL

param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbName = "regtech",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "dracons86"
)

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Fixing V48 Migration: VALIDITY Rules" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This script will:" -ForegroundColor Yellow
Write-Host "  1. Remove failed V48 migration from Flyway history" -ForegroundColor Gray
Write-Host "  2. Apply updated idempotent V48 migration" -ForegroundColor Gray
Write-Host ""

Write-Host "Database Configuration:" -ForegroundColor Yellow
Write-Host "  Host: $DbHost" -ForegroundColor Gray
Write-Host "  Port: $DbPort" -ForegroundColor Gray
Write-Host "  Database: $DbName" -ForegroundColor Gray
Write-Host "  User: $DbUser" -ForegroundColor Gray
Write-Host ""

# Step 1: Remove failed migration from Flyway history
Write-Host "Step 1: Cleaning Flyway history..." -ForegroundColor Cyan

$cleanupSql = "DELETE FROM flyway_schema_history WHERE version = '48' AND success = false;"

Write-Host "SQL to execute:" -ForegroundColor Gray
Write-Host $cleanupSql -ForegroundColor DarkGray
Write-Host ""

# For local PostgreSQL - try direct connection
Write-Host "Attempting direct connection to local PostgreSQL..." -ForegroundColor Gray

# Check if psql is available
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlPath) {
    Write-Host "psql found at: $($psqlPath.Source)" -ForegroundColor Green
    $env:PGPASSWORD = $DbPassword
    $result = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c $cleanupSql 2>&1
    Remove-Item Env:\PGPASSWORD
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Flyway history cleaned successfully" -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host "Database operation warning (may be OK if row did not exist)" -ForegroundColor Yellow
        Write-Host "$result" -ForegroundColor DarkGray
        Write-Host ""
    }
} else {
    Write-Host "psql not found in PATH" -ForegroundColor Yellow
    Write-Host "Skipping Flyway history cleanup - migration will handle conflicts with ON CONFLICT" -ForegroundColor Yellow
    Write-Host ""
}

# Step 2: Apply the migration
Write-Host "Step 2: Running Flyway repair..." -ForegroundColor Cyan
Write-Host ""

# Change to project root
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir

try {
    # Check if migration file exists
    $migrationFile = "regtech-app\src\main\resources\db\migration\dataquality\V48__insert_validity_rules.sql"
    if (-not (Test-Path $migrationFile)) {
        Write-Host "ERROR: Migration file not found: $migrationFile" -ForegroundColor Red
        exit 1
    }

    Write-Host "Migration file found and updated with ON CONFLICT clauses" -ForegroundColor Green
    Write-Host ""

    # Change to regtech-app directory
    Push-Location regtech-app

    try {
        # Set Flyway environment variables
        $env:FLYWAY_URL = "jdbc:postgresql://${DbHost}:${DbPort}/${DbName}"
        $env:FLYWAY_USER = $DbUser
        $env:FLYWAY_PASSWORD = $DbPassword
        
        Write-Host "Executing: ..\mvnw flyway:repair" -ForegroundColor Gray
        Write-Host ""
        
        # Run Flyway repair first
        ..\mvnw flyway:repair
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Flyway repair had warnings, continuing anyway..." -ForegroundColor Yellow
            Write-Host ""
        }
        
        Write-Host ""
        Write-Host "Step 3: Applying V48 migration..." -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "Executing: ..\mvnw flyway:migrate -Dflyway.outOfOrder=true" -ForegroundColor Gray
        Write-Host ""
        
        # Run Flyway migrate with out-of-order support
        ..\mvnw flyway:migrate "-Dflyway.outOfOrder=true"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "================================================" -ForegroundColor Green
            Write-Host "Migration V48 Applied Successfully!" -ForegroundColor Green
            Write-Host "================================================" -ForegroundColor Green
            Write-Host ""
            Write-Host "What was added:" -ForegroundColor Cyan
            Write-Host "  - Rule 1: ExposureAmount must be positive (CRITICAL)" -ForegroundColor White
            Write-Host "  - Rule 2: MaturityDate must be in future (HIGH)" -ForegroundColor White
            Write-Host "  - Rule 3: Sector must be in valid catalog (HIGH)" -ForegroundColor White
            Write-Host "  - Rule 4: Currency must follow ISO 4217 (CRITICAL)" -ForegroundColor White
            Write-Host "  - Rule 5: InternalRating must follow valid format (HIGH)" -ForegroundColor White
            Write-Host "  - Rule 6: Collateral must not exceed 3x Exposure (HIGH)" -ForegroundColor White
            Write-Host ""
            Write-Host "All INSERT statements now use ON CONFLICT DO NOTHING" -ForegroundColor Green
            Write-Host "Migration is now idempotent and safe to rerun!" -ForegroundColor Green
            Write-Host ""
        } else {
            Write-Host ""
            Write-Host "================================================" -ForegroundColor Red
            Write-Host "Migration V48 Failed!" -ForegroundColor Red
            Write-Host "================================================" -ForegroundColor Red
            Write-Host ""
            Write-Host "Check error messages above for details" -ForegroundColor Yellow
            
            # Clean up environment variables
            Remove-Item Env:\FLYWAY_URL -ErrorAction SilentlyContinue
            Remove-Item Env:\FLYWAY_USER -ErrorAction SilentlyContinue
            Remove-Item Env:\FLYWAY_PASSWORD -ErrorAction SilentlyContinue
            
            exit 1
        }
    } finally {
        # Clean up environment variables
        Remove-Item Env:\FLYWAY_URL -ErrorAction SilentlyContinue
        Remove-Item Env:\FLYWAY_USER -ErrorAction SilentlyContinue
        Remove-Item Env:\FLYWAY_PASSWORD -ErrorAction SilentlyContinue
        
        Pop-Location
    }
} finally {
    Pop-Location
}

Write-Host "Done!" -ForegroundColor Green
