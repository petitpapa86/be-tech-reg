@echo off
REM Batch script to apply V54 migration
REM Adds version column for optimistic locking to batches table

echo ========================================
echo Batch Entity Versioning
echo Migration V54: Add version to batches
echo ========================================
echo.

echo This migration will:
echo   1. Add version BIGINT column to riskcalculation.batches table
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
echo Applying migration V54...
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
echo Migration V54 applied successfully!
echo The version column has been added to the batches table.
echo.

pause
:exit