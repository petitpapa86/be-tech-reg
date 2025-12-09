@echo off
REM Windows batch script to apply V43 migration for quality reports tables

echo ========================================
echo Applying V43 Migration
echo Creating quality_reports and quality_error_summaries tables
echo ========================================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and try again
    pause
    exit /b 1
)

echo Running Flyway migration...
echo.

cd regtech-app
mvn flyway:migrate
cd ..

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration V43 applied successfully!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR: Migration failed!
    echo Please check the error messages above
    echo ========================================
)

echo.
pause
