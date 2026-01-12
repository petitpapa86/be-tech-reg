#!/usr/bin/env pwsh
# apply-v48-migration.ps1
# Applies Flyway migration V48__insert_validity_rules.sql

param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5433",
    [string]$DbName = "regtech",
    [string]$DbUser = "myuser",
    [string]$DbPassword = "secret"
)

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Applying V48 Migration: VALIDITY Rules" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Database Configuration:" -ForegroundColor Yellow
Write-Host "  Host: $DbHost" -ForegroundColor Gray
Write-Host "  Port: $DbPort" -ForegroundColor Gray
Write-Host "  Database: $DbName" -ForegroundColor Gray
Write-Host "  User: $DbUser" -ForegroundColor Gray
Write-Host ""

# Check if migration file exists
$migrationFile = "regtech-app\src\main\resources\db\migration\dataquality\V48__insert_validity_rules.sql"
if (-not (Test-Path $migrationFile)) {
    Write-Host "❌ ERROR: Migration file not found: $migrationFile" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Migration file found: $migrationFile" -ForegroundColor Green
Write-Host ""

# Show migration preview
Write-Host "📋 Migration Contents:" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
Get-Content $migrationFile -TotalCount 20
Write-Host "..." -ForegroundColor Gray
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
Write-Host ""

# Prompt for confirmation
$confirmation = Read-Host "Apply this migration? (yes/no)"
if ($confirmation -ne "yes") {
    Write-Host "❌ Migration cancelled by user" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "🔄 Applying migration via Maven Flyway..." -ForegroundColor Cyan

# Change to regtech-app directory
Push-Location regtech-app

try {
    # Run Flyway migrate
    $env:FLYWAY_URL = "jdbc:postgresql://${DbHost}:${DbPort}/${DbName}"
    $env:FLYWAY_USER = $DbUser
    $env:FLYWAY_PASSWORD = $DbPassword
    
    Write-Host "Executing: ..\mvnw flyway:migrate" -ForegroundColor Gray
    Write-Host ""
    
    ..\mvnw flyway:migrate
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "================================================" -ForegroundColor Green
        Write-Host "✅ Migration V48 Applied Successfully!" -ForegroundColor Green
        Write-Host "================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 What was added:" -ForegroundColor Cyan
        Write-Host "  • Rule 1: ExposureAmount > 0 (CRITICAL)" -ForegroundColor White
        Write-Host "  • Rule 2: MaturityDate > Today (HIGH)" -ForegroundColor White
        Write-Host "  • Rule 3: Sector in Valid Catalog (HIGH)" -ForegroundColor White
        Write-Host "  • Rule 4: Currency ISO 4217 (CRITICAL)" -ForegroundColor White
        Write-Host "  • Rule 5: InternalRating Format (HIGH)" -ForegroundColor White
        Write-Host "  • Rule 6: Collateral ≤ 3x Exposure (HIGH)" -ForegroundColor White
        Write-Host ""
        Write-Host "🎯 Next steps:" -ForegroundColor Yellow
        Write-Host "  1. Compile project: .\mvnw compile" -ForegroundColor Gray
        Write-Host "  2. Run application to test validity rules" -ForegroundColor Gray
        Write-Host "  3. Check validity score in quality reports" -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "================================================" -ForegroundColor Red
        Write-Host "❌ Migration V48 Failed!" -ForegroundColor Red
        Write-Host "================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Check error messages above for details" -ForegroundColor Yellow
        exit 1
    }
} finally {
    Pop-Location
}
