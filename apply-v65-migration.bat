@echo off
REM Apply V65 Migration - Add validation_category to business_rules
REM This script adds validation category column to business_rules table

echo =================================================
echo REGTECH - Apply V65 Migration
echo Add Validation Category to Business Rules
echo =================================================
echo.

REM Migration details
echo Migration Version: V65
echo Description: Add validation_category column and populate categories
echo File: V65__add_validation_category_to_business_rules.sql
echo.

REM Check if migration file exists
if not exist "regtech-app\src\main\resources\db\migration\dataquality\V65__add_validation_category_to_business_rules.sql" (
    echo ERROR: Migration file not found
    exit /b 1
)

echo Migration file found
echo.

REM Prompt for confirmation
echo This migration will:
echo   1. Add validation_category VARCHAR(50) column to business_rules
echo   2. Categorize existing rules based on rule_code patterns
echo   3. Create index on validation_category column
echo.

set /p confirmation="Do you want to proceed with the migration? (yes/no): "
if /i not "%confirmation%"=="yes" (
    echo Migration cancelled by user.
    exit /b 0
)

echo.
echo Starting migration...
echo.

REM Navigate to regtech-app directory
cd regtech-app

REM Run Flyway migration
echo Running Flyway migrate...
call ..\mvnw.cmd flyway:migrate -Dflyway.outOfOrder=true

if %ERRORLEVEL% equ 0 (
    echo.
    echo =================================================
    echo SUCCESS: Migration V65 applied successfully!
    echo =================================================
    echo.
    
    REM Verify migration
    echo Verifying migration...
    call ..\mvnw.cmd flyway:info
    
    echo.
    echo Next Steps:
    echo 1. Verify the validation_category column was added
    echo 2. Check category assignments
    echo 3. View sample categorized rules
    echo.
) else (
    echo.
    echo =================================================
    echo ERROR: Migration V65 failed!
    echo =================================================
    echo.
    echo Check the error messages above for details.
    exit /b 1
)

REM Return to original directory
cd ..

echo Migration process complete.
