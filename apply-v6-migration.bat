@echo off
REM Quick script to apply V6 migration manually on Windows
REM Usage: apply-v6-migration.bat

echo === Applying V6 Event Tables Schema Migration ===
echo.
echo This will:
echo   1. Drop old outbox_messages, inbox_messages, and sagas tables
echo   2. Recreate them with correct schema
echo.

set /p DB_USER="Enter PostgreSQL username: "
set /p DB_NAME="Enter PostgreSQL database name: "
set /p DB_PASS="Enter PostgreSQL password: "

set PGPASSWORD=%DB_PASS%

echo.
echo Applying migration...
psql -U %DB_USER% -d %DB_NAME% -f regtech-app/src/main/resources/db/migration/common/V6__Migrate_outbox_messages_schema.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Migration applied successfully!
    echo.
    echo Now update flyway_schema_history:
    psql -U %DB_USER% -d %DB_NAME% -c "INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history), '6', 'Migrate event processing tables schema', 'SQL', 'V6__Migrate_outbox_messages_schema.sql', 0, '%DB_USER%', 0, true);"
    
    echo.
    echo All done! You can now restart your application.
) else (
    echo.
    echo Migration failed. Check the error above.
    exit /b 1
)

set PGPASSWORD=
