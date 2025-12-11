@echo off
REM Batch script to apply V55 migration
REM Adds version column for optimistic locking to portfolio_analysis table

echo ========================================
echo Portfolio Analysis Versioning
echo Migration V55: Add version to portfolio_analysis
echo ========================================
echo.

echo This migration will:
echo   1. Add version BIGINT column to riskcalculation.portfolio_analysis table
echo   2. Set default value to 0 for existing rows
echo.

echo Prerequisites:
echo   - PostgreSQL database is running
echo   - Database connection configured in application.yml
echo   - Flyway is configured and working
echo.

set /p confirm="Do you want to proceed with the migration? (y/N): "
if /i not "%confirm%"=="y" goto :exit

echo.
echo Applying migration V55...
echo.

cd /d "%~dp0"
mvn flyway:migrate -Dflyway.schemas=riskcalculation

if %errorlevel% neq 0 (
    echo.
    echo Migration failed! Check the output above for errors.
    echo.
    pause
    exit /b 1
)

echo.
echo Migration V55 applied successfully!
echo The version column has been added to the portfolio_analysis table.
echo.

pause
:exit