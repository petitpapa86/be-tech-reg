@echo off
REM Batch script to apply V22 migration for invoices schema update
REM This updates the invoices table to match the current domain model

echo ========================================
echo Applying V22 Migration - Invoices Schema Update
echo ========================================
echo.

REM Check if .env file exists
if not exist ".env" (
    echo ERROR: .env file not found!
    echo Please create a .env file with your database credentials.
    exit /b 1
)

REM Load environment variables from .env file
for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
    set %%a=%%b
)

echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.

REM Set PGPASSWORD environment variable for psql
set PGPASSWORD=%DB_PASSWORD%

REM Migration file path
set MIGRATION_FILE=regtech-app\src\main\resources\db\migration\billing\V22__Update_invoices_schema.sql

echo Checking migration file...
if not exist "%MIGRATION_FILE%" (
    echo ERROR: Migration file not found: %MIGRATION_FILE%
    exit /b 1
)
echo Migration file found: %MIGRATION_FILE%
echo.

REM Confirm before proceeding
set /p confirmation="Do you want to apply this migration? (yes/no): "
if not "%confirmation%"=="yes" (
    echo Migration cancelled.
    exit /b 0
)

echo.
echo Applying migration...

REM Apply migration using psql
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%MIGRATION_FILE%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo Migration Applied Successfully!
    echo ========================================
    echo.
    echo The invoices table schema has been updated to match the current domain model.
    echo.
    echo Changes applied:
    echo   Invoices table:
    echo     - Removed: amount_due, currency, subscription_id columns
    echo     - Added: subscription_amount_value, subscription_amount_currency
    echo     - Added: overage_amount_value, overage_amount_currency
    echo     - Added: total_amount_value, total_amount_currency
    echo     - Added: billing_period_start_date, billing_period_end_date
    echo     - Added: invoice_number, issue_date, sent_at
    echo   Invoice Line Items table:
    echo     - Removed: amount, currency columns
    echo     - Added: unit_amount_value, unit_amount_currency
    echo     - Added: total_amount_value, total_amount_currency
    echo.
    echo You can now restart your application.
) else (
    echo.
    echo ========================================
    echo Migration Failed!
    echo ========================================
    echo.
    echo Please check the error messages above and fix any issues.
    echo Common issues:
    echo   - Database connection problems
    echo   - Insufficient permissions
    echo   - Migration already applied
    exit /b 1
)

REM Clear password from environment
set PGPASSWORD=
