-- V67__update_report_configuration_enums_to_italian.sql

-- Drop existing constraints
ALTER TABLE report_configuration DROP CONSTRAINT IF EXISTS report_configuration_language_check;
ALTER TABLE report_configuration DROP CONSTRAINT IF EXISTS report_configuration_frequency_check;
ALTER TABLE report_configuration DROP CONSTRAINT IF EXISTS report_configuration_schedule_day_check;

-- Update data to new Italian enum values
UPDATE report_configuration SET language = 'ITALIAN' WHERE language = 'ITALIAN'; -- No change in enum name if we use ITALIAN in code but IT in DTO?
-- Wait, I renamed them in code to match requested DTO?
-- Actually, I only renamed ReportFrequency and ScheduleDay.
-- ReportLanguage stayed as ITALIAN, ENGLISH, BILINGUAL.

-- Update Frequency
UPDATE report_configuration SET frequency = 'MENSILE' WHERE frequency = 'MONTHLY';
UPDATE report_configuration SET frequency = 'TRIMESTRALE' WHERE frequency = 'QUARTERLY';
UPDATE report_configuration SET frequency = 'SEMESTRALE' WHERE frequency = 'SEMI_ANNUAL';
UPDATE report_configuration SET frequency = 'ANNUALE' WHERE frequency = 'ANNUAL';

-- Update Schedule Day
UPDATE report_configuration SET schedule_day = 'LUNEDI' WHERE schedule_day = 'MONDAY';
UPDATE report_configuration SET schedule_day = 'MARTEDI' WHERE schedule_day = 'TUESDAY';
UPDATE report_configuration SET schedule_day = 'MERCOLEDI' WHERE schedule_day = 'WEDNESDAY';
UPDATE report_configuration SET schedule_day = 'GIOVEDI' WHERE schedule_day = 'THURSDAY';
UPDATE report_configuration SET schedule_day = 'VENERDI' WHERE schedule_day = 'FRIDAY';

-- Add new constraints
ALTER TABLE report_configuration ADD CONSTRAINT report_configuration_frequency_check 
    CHECK (frequency IN ('MENSILE', 'TRIMESTRALE', 'SEMESTRALE', 'ANNUALE'));

ALTER TABLE report_configuration ADD CONSTRAINT report_configuration_schedule_day_check 
    CHECK (schedule_day IN ('LUNEDI', 'MARTEDI', 'MERCOLEDI', 'GIOVEDI', 'VENERDI', 'SABATO', 'DOMENICA'));

-- Add missing languages if needed (though already covered by previous constraint)
ALTER TABLE report_configuration ADD CONSTRAINT report_configuration_language_check 
    CHECK (language IN ('ITALIAN', 'ENGLISH', 'BILINGUAL'));
