@echo off
REM Apply V21 migration to fix subscriptions table schema
REM This script applies the V21 migration SQL directly to PostgreSQL

echo ========================================
echo V21 Subscriptions Schema Migration
echo ========================================
echo.
echo This will:
echo 1. Drop and recreate billing.subscriptions table
echo 2. Update Flyway history to record V21
echo.
echo WARNING: This will DELETE all existing subscription data!
echo.

REM Load database connection from .env file
if exist .env (
    for /f "tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="DB_HOST" set DB_HOST=%%b
        if "%%a"=="DB_PORT" set DB_PORT=%%b
        if "%%a"=="DB_NAME" set DB_NAME=%%b
        if "%%a"=="DB_USER" set DB_USER=%%b
        if "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
    )
)

REM Set defaults if not found in .env
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_NAME set DB_NAME=regtech_db
if not defined DB_USER set DB_USER=postgres
if not defined DB_PASSWORD set DB_PASSWORD=postgres

echo Database: %DB_NAME% on %DB_HOST%:%DB_PORT%
echo User: %DB_USER%
echo.

set /p CONFIRM="Type 'yes' to continue: "
if /i not "%CONFIRM%"=="yes" (
    echo Migration cancelled.
    exit /b 1
)

echo.
echo Applying V21 migration...
echo.

REM Set PGPASSWORD environment variable for psql
set PGPASSWORD=%DB_PASSWORD%

REM Apply the migration
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Migration failed!
    echo.
    echo Make sure:
    echo 1. PostgreSQL is running
    echo 2. Database credentials are correct in .env file
    echo 3. psql command is available in PATH
    echo.
    exit /b 1
)

echo.
echo Updating Flyway history...
echo.

REM Update Flyway history
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), '21', 'Update subscriptions schema', 'SQL', 'V21__Update_subscriptions_schema.sql', NULL, CURRENT_USER, CURRENT_TIMESTAMP, 0, true);"

if %ERRORLEVEL% neq 0 (
    echo.
    echo WARNING: Failed to update Flyway history!
    echo The migration was applied but Flyway doesn't know about it.
    echo You may need to manually update flyway_schema_history table.
    echo.
    exit /b 1
)

echo.
echo ========================================
echo Migration V21 applied successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Restart your application
echo 2. Test user registration
echo 3. Apply V6 migration for outbox_messages (see APPLY_MIGRATION_NOW.md)
echo.

REM Clear password from environment
set PGPASSWORD=

pause
