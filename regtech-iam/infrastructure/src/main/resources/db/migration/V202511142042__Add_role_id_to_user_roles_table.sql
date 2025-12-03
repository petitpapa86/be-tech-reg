-- V202511142042__Add_role_id_to_user_roles_table.sql
-- Add role_id column to user_roles table for database-driven role management

-- Add role_id column to user_roles table
ALTER TABLE iam.user_roles ADD COLUMN IF NOT EXISTS role_id VARCHAR(36);

-- Create index for the new role_id column
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON iam.user_roles (role_id);

-- Add foreign key constraint (optional - can be added later if roles table exists)
-- ALTER TABLE iam.user_roles ADD CONSTRAINT fk_user_roles_role_id
-- FOREIGN KEY (role_id) REFERENCES iam.roles (id);

-- Update existing records to populate role_id based on role_name
-- This maps the role names to the corresponding role IDs
UPDATE iam.user_roles
SET role_id = CASE
    WHEN role_name = 'VIEWER' THEN 'role-viewer'
    WHEN role_name = 'DATA_ANALYST' THEN 'role-data-analyst'
    WHEN role_name = 'AUDITOR' THEN 'role-auditor'
    WHEN role_name = 'RISK_MANAGER' THEN 'role-risk-manager'
    WHEN role_name = 'COMPLIANCE_OFFICER' THEN 'role-compliance-officer'
    WHEN role_name = 'BANK_ADMIN' THEN 'role-bank-admin'
    WHEN role_name = 'HOLDING_COMPANY_USER' THEN 'role-holding-company'
    WHEN role_name = 'SYSTEM_ADMIN' THEN 'role-system-admin'
    ELSE NULL
END
WHERE role_id IS NULL;