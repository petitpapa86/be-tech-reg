-- Verification script for quality_grade column before migration V45

-- Check current column definition
SELECT 
    column_name, 
    data_type, 
    character_maximum_length,
    is_nullable
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
  AND table_name = 'quality_reports' 
  AND column_name = 'quality_grade';

-- Check if there are any existing records
SELECT COUNT(*) as total_records
FROM dataquality.quality_reports;

-- Check existing quality_grade values (if any)
SELECT 
    quality_grade,
    LENGTH(quality_grade) as grade_length,
    COUNT(*) as count
FROM dataquality.quality_reports
WHERE quality_grade IS NOT NULL
GROUP BY quality_grade, LENGTH(quality_grade)
ORDER BY grade_length DESC;

-- Check for any records that might have issues
SELECT 
    report_id,
    batch_id,
    quality_grade,
    LENGTH(quality_grade) as grade_length
FROM dataquality.quality_reports
WHERE quality_grade IS NOT NULL
  AND LENGTH(quality_grade) > 2
LIMIT 10;
