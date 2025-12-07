@echo off
REM Batch script to apply V52 migration

echo Applying V52 migration: Increase counterparty_lei length

REM Load environment variables from .env file
if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
        set %%a=%%b
    )
)

echo Database: %DB_NAME% at %DB_HOST%:%DB_PORT%

REM Set PGPASSWORD for psql
set PGPASSWORD=%DB_PASSWORD%

REM Apply migration
echo Executing migration...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "regtech-app/src/main/resources/db/migration/riskcalculation/V52__Increase_counterparty_lei_length.sql"

if %ERRORLEVEL% EQU 0 (
    echo Migration V52 applied successfully!
) else (
    echo Migration V52 failed!
    exit /b 1
)
