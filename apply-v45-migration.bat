@echo off
REM Apply V45 migration to fix quality_grade column length
echo Applying V45 migration: increase quality_grade column length...
echo.

REM Load environment variables
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        set "%%a=%%b"
    )
)

REM Set default values if not in .env
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_NAME set DB_NAME=regtech_db
if not defined DB_USER set DB_USER=regtech_user
if not defined DB_PASSWORD set DB_PASSWORD=regtech_password

echo Database: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo User: %DB_USER%
echo.

REM Execute migration
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f regtech-app/src/main/resources/db/migration/dataquality/V45__increase_quality_grade_length.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Migration V45 applied successfully!
    echo.
    echo The quality_grade column has been increased to VARCHAR(20).
) else (
    echo.
    echo ✗ Migration V45 failed!
    echo Please check the error messages above.
    exit /b 1
)

pause
