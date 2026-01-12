@echo off
REM apply-v48-migration.bat
REM Applies Flyway migration V48__insert_validity_rules.sql

setlocal

set DB_HOST=localhost
set DB_PORT=5433
set DB_NAME=regtech
set DB_USER=myuser
set DB_PASSWORD=secret

echo ================================================
echo Applying V48 Migration: VALIDITY Rules
echo ================================================
echo.

echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.

REM Check if migration file exists
set MIGRATION_FILE=regtech-app\src\main\resources\db\migration\dataquality\V48__insert_validity_rules.sql
if not exist "%MIGRATION_FILE%" (
    echo ERROR: Migration file not found: %MIGRATION_FILE%
    exit /b 1
)

echo Migration file found: %MIGRATION_FILE%
echo.

echo Migration Contents (first 20 lines):
echo ------------------------------------------------------------
type "%MIGRATION_FILE%" | more /e +0
echo ...
echo ------------------------------------------------------------
echo.

set /p CONFIRM="Apply this migration? (yes/no): "
if /i not "%CONFIRM%"=="yes" (
    echo Migration cancelled by user
    exit /b 0
)

echo.
echo Applying migration via Maven Flyway...

REM Change to regtech-app directory
cd regtech-app

REM Set Flyway environment variables
set FLYWAY_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
set FLYWAY_USER=%DB_USER%
set FLYWAY_PASSWORD=%DB_PASSWORD%

echo Executing: ..\mvnw flyway:migrate
echo.

call ..\mvnw flyway:migrate

if %ERRORLEVEL% equ 0 (
    echo.
    echo ================================================
    echo Migration V48 Applied Successfully!
    echo ================================================
    echo.
    echo What was added:
    echo   - Rule 1: ExposureAmount ^> 0 ^(CRITICAL^)
    echo   - Rule 2: MaturityDate ^> Today ^(HIGH^)
    echo   - Rule 3: Sector in Valid Catalog ^(HIGH^)
    echo   - Rule 4: Currency ISO 4217 ^(CRITICAL^)
    echo   - Rule 5: InternalRating Format ^(HIGH^)
    echo   - Rule 6: Collateral ≤ 3x Exposure ^(HIGH^)
    echo.
    echo Next steps:
    echo   1. Compile project: .\mvnw compile
    echo   2. Run application to test validity rules
    echo   3. Check validity score in quality reports
    echo.
) else (
    echo.
    echo ================================================
    echo Migration V48 Failed!
    echo ================================================
    echo.
    echo Check error messages above for details
    cd ..
    exit /b 1
)

cd ..
endlocal
