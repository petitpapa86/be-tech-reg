@echo off
REM Batch script to apply V44 migration
REM Increases report_id column length in quality_reports table

echo Applying V44 migration: Increase report_id length

REM Load environment variables from .env file
if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
        set %%a=%%b
    )
)

REM Set defaults if not provided
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_NAME set DB_NAME=regtech_db
if not defined DB_USER set DB_USER=regtech_user

echo Database: %DB_NAME% at %DB_HOST%:%DB_PORT%

REM Set PGPASSWORD environment variable
set PGPASSWORD=%DB_PASSWORD%

REM Apply migration
echo Applying migration...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "regtech-app/src/main/resources/db/migration/dataquality/V44__increase_report_id_length.sql"

if %ERRORLEVEL% EQU 0 (
    echo Migration applied successfully!
) else (
    echo Migration failed!
    exit /b 1
)

REM Clear password
set PGPASSWORD=
