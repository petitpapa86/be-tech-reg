# Apply V62 Migration - Add validation_category to business_rules
# This script adds validation category column to business_rules table
# and updates existing rules with appropriate categories

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "REGTECH - Apply V65 Migration" -ForegroundColor Cyan
Write-Host "Add Validation Category to Business Rules" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# Migration details
Write-Host "Migration Version: V65" -ForegroundColor Yellow
Write-Host "Description: Add validation_category column and populate categories" -ForegroundColor Yellow
Write-Host "File: V65__add_validation_category_to_business_rules.sql" -ForegroundColor Yellow
Write-Host ""

# Check if migration file exists
$migrationFile = "regtech-app\src\main\resources\db\migration\dataquality\V65__add_validation_category_to_business_rules.sql"
if (-not (Test-Path $migrationFile)) {
    Write-Host "ERROR: Migration file not found: $migrationFile" -ForegroundColor Red
    exit 1
}

Write-Host "Migration file found: $migrationFile" -ForegroundColor Green
Write-Host ""

# Prompt for confirmation
Write-Host "This migration will:" -ForegroundColor Yellow
Write-Host "  1. Add validation_category VARCHAR(50) column to business_rules" -ForegroundColor White
Write-Host "  2. Categorize existing rules based on rule_code patterns:" -ForegroundColor White
Write-Host "     - DATA_QUALITY: Completeness rules" -ForegroundColor White
Write-Host "     - NUMERIC_RANGES: Amount validations" -ForegroundColor White
Write-Host "     - CODE_VALIDATION: Currency, country, LEI, rating validations" -ForegroundColor White
Write-Host "     - TEMPORAL_COHERENCE: Date and time validations" -ForegroundColor White
Write-Host "     - DUPLICATE_DETECTION: Duplicate checks" -ForegroundColor White
Write-Host "     - CROSS_REFERENCE: Cross-reference validations" -ForegroundColor White
Write-Host "  3. Create index on validation_category column" -ForegroundColor White
Write-Host ""

$confirmation = Read-Host "Do you want to proceed with the migration? (yes/no)"
if ($confirmation -ne "yes") {
    Write-Host "Migration cancelled by user." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Starting migration..." -ForegroundColor Cyan
Write-Host ""

# Navigate to regtech-app directory
Push-Location regtech-app

try {
    # Run Flyway migration
    Write-Host "Running Flyway migrate..." -ForegroundColor Cyan
    & ..\mvnw.cmd flyway:migrate -Dflyway.outOfOrder=true
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=================================================" -ForegroundColor Green
        Write-Host "SUCCESS: Migration V65 applied successfully!" -ForegroundColor Green
        Write-Host "=================================================" -ForegroundColor Green
        Write-Host ""
        
        # Verify migration
        Write-Host "Verifying migration..." -ForegroundColor Cyan
        & ..\mvnw.cmd flyway:info
        
        Write-Host ""
        Write-Host "Next Steps:" -ForegroundColor Yellow
        Write-Host "1. Verify the validation_category column was added:" -ForegroundColor White
        Write-Host "   docker exec regtech-postgres-1 psql -U myuser -d regtech -c '\d dataquality.business_rules'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "2. Check category assignments:" -ForegroundColor White
        Write-Host "   docker exec regtech-postgres-1 psql -U myuser -d regtech -c 'SELECT validation_category, COUNT(*) FROM dataquality.business_rules GROUP BY validation_category;'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "3. View sample categorized rules:" -ForegroundColor White
        Write-Host "   docker exec regtech-postgres-1 psql -U myuser -d regtech -c 'SELECT rule_code, validation_category FROM dataquality.business_rules ORDER BY validation_category LIMIT 20;'" -ForegroundColor Gray
        Write-Host ""
        
    } else {
        Write-Host ""
        Write-Host "=================================================" -ForegroundColor Red
        Write-Host "ERROR: Migration V65 failed!" -ForegroundColor Red
        Write-Host "=================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Check the error messages above for details." -ForegroundColor Yellow
        Write-Host "You may need to run 'mvnw flyway:repair' if the migration is marked as failed." -ForegroundColor Yellow
        exit 1
    }
    
} finally {
    # Return to original directory
    Pop-Location
}

Write-Host "Migration process complete." -ForegroundColor Cyan
