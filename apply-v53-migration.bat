@echo off
REM Batch script to apply V53 migration
REM Adds calculation_results_uri column to batches table

echo ========================================
echo Risk Calculation Storage Refactoring
echo Migration V53: Add calculation_results_uri
echo ========================================
echo.

echo This migration will:
echo   1. Add calculation_results_uri VARCHAR(500) column to riskcalculation.batches table
echo   2. Create index idx_batches_results_uri for URI lookups
echo.

echo Prerequisites:
echo   - PostgreSQL database is running
echo   - Database connection configured in application.yml
echo   - Flyway is configured and working
echo.

set /p confirmation="Do you want to proceed with the migration? (yes/no): "

if /i not "%confirmation%"=="yes" (
    echo Migration cancelled.
    exit /b 0
)

echo.
echo Running Flyway migration...

REM Run Flyway migration via Maven
call mvn flyway:migrate -Dflyway.schemas=riskcalculation

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo Migration V53 completed successfully!
    echo ========================================
    echo.
    echo Next steps:
    echo   1. Verify the column was added: SELECT calculation_results_uri FROM riskcalculation.batches LIMIT 1;
    echo   2. Proceed with implementing task 2: Enhance BatchRepository with URI management methods
) else (
    echo.
    echo ========================================
    echo Migration V53 failed!
    echo ========================================
    echo.
    echo Please check the error messages above and:
    echo   1. Verify database connection
    echo   2. Check Flyway configuration
    echo   3. Review migration file: regtech-app/src/main/resources/db/migration/riskcalculation/V53__Add_calculation_results_uri_to_batches.sql
)
