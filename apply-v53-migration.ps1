# PowerShell script to apply V53 migration
# Adds calculation_results_uri column to batches table

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Risk Calculation Storage Refactoring" -ForegroundColor Cyan
Write-Host "Migration V53: Add calculation_results_uri" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This migration will:" -ForegroundColor Yellow
Write-Host "  1. Add calculation_results_uri VARCHAR(500) column to riskcalculation.batches table" -ForegroundColor White
Write-Host "  2. Create index idx_batches_results_uri for URI lookups" -ForegroundColor White
Write-Host ""

Write-Host "Prerequisites:" -ForegroundColor Yellow
Write-Host "  - PostgreSQL database is running" -ForegroundColor White
Write-Host "  - Database connection configured in application.yml" -ForegroundColor White
Write-Host "  - Flyway is configured and working" -ForegroundColor White
Write-Host ""

$confirmation = Read-Host "Do you want to proceed with the migration? (yes/no)"

if ($confirmation -ne "yes") {
    Write-Host "Migration cancelled." -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "Running Flyway migration..." -ForegroundColor Green

# Run Flyway migration via Maven
mvn flyway:migrate -Dflyway.schemas=riskcalculation

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Migration V53 completed successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Verify the column was added: SELECT calculation_results_uri FROM riskcalculation.batches LIMIT 1;" -ForegroundColor White
    Write-Host "  2. Proceed with implementing task 2: Enhance BatchRepository with URI management methods" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Migration V53 failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check the error messages above and:" -ForegroundColor Yellow
    Write-Host "  1. Verify database connection" -ForegroundColor White
    Write-Host "  2. Check Flyway configuration" -ForegroundColor White
    Write-Host "  3. Review migration file: regtech-app/src/main/resources/db/migration/riskcalculation/V53__Add_calculation_results_uri_to_batches.sql" -ForegroundColor White
}
