-- Clean up problematic migration records from Flyway schema history
-- This migration removes migrations that were causing issues or were outdated

DELETE FROM flyway_schema_history
WHERE version = '202511142042'
  AND description = 'Add role id to user roles table';