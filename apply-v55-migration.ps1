# PowerShell script to apply V55 migration
# Adds version column for optimistic locking to portfolio_analysis table

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Portfolio Analysis Versioning" -ForegroundColor Cyan
Write-Host "Migration V55: Add version to portfolio_analysis" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This migration will:" -ForegroundColor Yellow
Write-Host "  1. Add version BIGINT column to riskcalculation.portfolio_analysis table" -ForegroundColor White
Write-Host "  2. Set default value to 0 for existing rows" -ForegroundColor White
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

try {
    # Run the migration
    & mvn flyway:migrate -Dflyway.schemas=riskcalculation

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Migration V55 applied successfully!" -ForegroundColor Green
        Write-Host "The version column has been added to the portfolio_analysis table." -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "Migration failed! Check the output above for errors." -ForegroundColor Red
        Write-Host ""
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "An error occurred during migration: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    exit 1
}