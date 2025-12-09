-- V10__Create_roles_and_permissions_tables.sql
-- Create IAM tables including users, roles, and permissions
-- Originally: V202511142041__Create_roles_and_permissions_tables.sql

-- Create users table
CREATE TABLE IF NOT EXISTS iam.users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_username ON iam.users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON iam.users (email);

-- Create roles table
CREATE TABLE IF NOT EXISTS iam.roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for roles table
CREATE INDEX IF NOT EXISTS idx_roles_name ON iam.roles (name);
CREATE INDEX IF NOT EXISTS idx_roles_level ON iam.roles (level);

-- Create role_permissions table
CREATE TABLE IF NOT EXISTS iam.role_permissions (
    id VARCHAR(100) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES iam.roles (id) ON DELETE CASCADE,
    UNIQUE (role_id, permission)
);

-- Create indexes for role_permissions table
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON iam.role_permissions (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON iam.role_permissions (permission);

-- Insert roles
INSERT INTO iam.roles (id, name, display_name, description, level) VALUES
('role-viewer', 'VIEWER', 'Basic Viewer', 'Can only view reports and data - read-only access for basic users', 1),
('role-data-analyst', 'DATA_ANALYST', 'Data Analyst', 'Can upload files and view reports - handles data processing and analysis', 2),
('role-auditor', 'AUDITOR', 'Auditor', 'Read-only access with audit capabilities - monitors system and tracks submissions', 3),
('role-risk-manager', 'RISK_MANAGER', 'Risk Manager', 'Can manage violations and generate reports - handles risk assessment and mitigation', 3),
('role-compliance-officer', 'COMPLIANCE_OFFICER', 'Compliance Officer', 'Full compliance management capabilities - oversees regulatory compliance and reporting', 4),
('role-bank-admin', 'BANK_ADMIN', 'Bank Administrator', 'Manages bank-specific configurations - administers bank-level settings and users', 4),
('role-holding-company', 'HOLDING_COMPANY_USER', 'Holding Company User', 'Can view across multiple banks - access to consolidated data and reports', 4),
('role-system-admin', 'SYSTEM_ADMIN', 'System Administrator', 'Full system access - complete administrative control over the entire system', 5)
ON CONFLICT (id) DO NOTHING;

-- Insert permissions for VIEWER
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-viewer-1', 'role-viewer', 'BCBS239_VIEW_REPORTS'),
('perm-viewer-2', 'role-viewer', 'BCBS239_VIEW_VIOLATIONS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for DATA_ANALYST
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-analyst-1', 'role-data-analyst', 'BCBS239_UPLOAD_FILES'),
('perm-analyst-2', 'role-data-analyst', 'BCBS239_DOWNLOAD_FILES'),
('perm-analyst-3', 'role-data-analyst', 'BCBS239_VIEW_REPORTS'),
('perm-analyst-4', 'role-data-analyst', 'BCBS239_GENERATE_REPORTS'),
('perm-analyst-5', 'role-data-analyst', 'BCBS239_VIEW_VIOLATIONS'),
('perm-analyst-6', 'role-data-analyst', 'BCBS239_VALIDATE_DATA')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for AUDITOR
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-auditor-1', 'role-auditor', 'BCBS239_VIEW_REPORTS'),
('perm-auditor-2', 'role-auditor', 'BCBS239_EXPORT_REPORTS'),
('perm-auditor-3', 'role-auditor', 'BCBS239_VIEW_VIOLATIONS'),
('perm-auditor-4', 'role-auditor', 'BCBS239_VIEW_AUDIT_LOGS'),
('perm-auditor-5', 'role-auditor', 'BCBS239_TRACK_SUBMISSIONS'),
('perm-auditor-6', 'role-auditor', 'BCBS239_MONITOR_SYSTEM')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for RISK_MANAGER
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-risk-1', 'role-risk-manager', 'BCBS239_UPLOAD_FILES'),
('perm-risk-2', 'role-risk-manager', 'BCBS239_DOWNLOAD_FILES'),
('perm-risk-3', 'role-risk-manager', 'BCBS239_DELETE_FILES'),
('perm-risk-4', 'role-risk-manager', 'BCBS239_GENERATE_REPORTS'),
('perm-risk-5', 'role-risk-manager', 'BCBS239_VIEW_REPORTS'),
('perm-risk-6', 'role-risk-manager', 'BCBS239_EXPORT_REPORTS'),
('perm-risk-7', 'role-risk-manager', 'BCBS239_SCHEDULE_REPORTS'),
('perm-risk-8', 'role-risk-manager', 'BCBS239_MANAGE_VIOLATIONS'),
('perm-risk-9', 'role-risk-manager', 'BCBS239_VIEW_VIOLATIONS'),
('perm-risk-10', 'role-risk-manager', 'BCBS239_VALIDATE_DATA'),
('perm-risk-11', 'role-risk-manager', 'BCBS239_APPROVE_DATA'),
('perm-risk-12', 'role-risk-manager', 'BCBS239_REJECT_DATA'),
('perm-risk-13', 'role-risk-manager', 'BCBS239_MANAGE_TEMPLATES')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for COMPLIANCE_OFFICER
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-compliance-1', 'role-compliance-officer', 'BCBS239_UPLOAD_FILES'),
('perm-compliance-2', 'role-compliance-officer', 'BCBS239_DOWNLOAD_FILES'),
('perm-compliance-3', 'role-compliance-officer', 'BCBS239_DELETE_FILES'),
('perm-compliance-4', 'role-compliance-officer', 'BCBS239_GENERATE_REPORTS'),
('perm-compliance-5', 'role-compliance-officer', 'BCBS239_VIEW_REPORTS'),
('perm-compliance-6', 'role-compliance-officer', 'BCBS239_EXPORT_REPORTS'),
('perm-compliance-7', 'role-compliance-officer', 'BCBS239_SCHEDULE_REPORTS'),
('perm-compliance-8', 'role-compliance-officer', 'BCBS239_CONFIGURE_PARAMETERS'),
('perm-compliance-9', 'role-compliance-officer', 'BCBS239_MANAGE_TEMPLATES'),
('perm-compliance-10', 'role-compliance-officer', 'BCBS239_CONFIGURE_WORKFLOWS'),
('perm-compliance-11', 'role-compliance-officer', 'BCBS239_MANAGE_VIOLATIONS'),
('perm-compliance-12', 'role-compliance-officer', 'BCBS239_APPROVE_VIOLATIONS'),
('perm-compliance-13', 'role-compliance-officer', 'BCBS239_VIEW_VIOLATIONS'),
('perm-compliance-14', 'role-compliance-officer', 'BCBS239_VALIDATE_DATA'),
('perm-compliance-15', 'role-compliance-officer', 'BCBS239_APPROVE_DATA'),
('perm-compliance-16', 'role-compliance-officer', 'BCBS239_REJECT_DATA'),
('perm-compliance-17', 'role-compliance-officer', 'BCBS239_SUBMIT_REGULATORY_REPORTS'),
('perm-compliance-18', 'role-compliance-officer', 'BCBS239_REVIEW_SUBMISSIONS'),
('perm-compliance-19', 'role-compliance-officer', 'BCBS239_TRACK_SUBMISSIONS'),
('perm-compliance-20', 'role-compliance-officer', 'BCBS239_VIEW_AUDIT_LOGS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for BANK_ADMIN
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-bank-admin-1', 'role-bank-admin', 'BCBS239_UPLOAD_FILES'),
('perm-bank-admin-2', 'role-bank-admin', 'BCBS239_DOWNLOAD_FILES'),
('perm-bank-admin-3', 'role-bank-admin', 'BCBS239_DELETE_FILES'),
('perm-bank-admin-4', 'role-bank-admin', 'BCBS239_GENERATE_REPORTS'),
('perm-bank-admin-5', 'role-bank-admin', 'BCBS239_VIEW_REPORTS'),
('perm-bank-admin-6', 'role-bank-admin', 'BCBS239_EXPORT_REPORTS'),
('perm-bank-admin-7', 'role-bank-admin', 'BCBS239_CONFIGURE_PARAMETERS'),
('perm-bank-admin-8', 'role-bank-admin', 'BCBS239_MANAGE_TEMPLATES'),
('perm-bank-admin-9', 'role-bank-admin', 'BCBS239_MANAGE_VIOLATIONS'),
('perm-bank-admin-10', 'role-bank-admin', 'BCBS239_VIEW_VIOLATIONS'),
('perm-bank-admin-11', 'role-bank-admin', 'BCBS239_ADMINISTER_USERS'),
('perm-bank-admin-12', 'role-bank-admin', 'BCBS239_ASSIGN_ROLES'),
('perm-bank-admin-13', 'role-bank-admin', 'BCBS239_MANAGE_BANK_CONFIG'),
('perm-bank-admin-14', 'role-bank-admin', 'BCBS239_VIEW_AUDIT_LOGS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for HOLDING_COMPANY_USER
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-holding-1', 'role-holding-company', 'BCBS239_VIEW_REPORTS'),
('perm-holding-2', 'role-holding-company', 'BCBS239_GENERATE_REPORTS'),
('perm-holding-3', 'role-holding-company', 'BCBS239_EXPORT_REPORTS'),
('perm-holding-4', 'role-holding-company', 'BCBS239_VIEW_CROSS_BANK_DATA'),
('perm-holding-5', 'role-holding-company', 'BCBS239_CONSOLIDATE_REPORTS'),
('perm-holding-6', 'role-holding-company', 'BCBS239_VIEW_VIOLATIONS'),
('perm-holding-7', 'role-holding-company', 'BCBS239_TRACK_SUBMISSIONS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Insert permissions for SYSTEM_ADMIN
INSERT INTO iam.role_permissions (id, role_id, permission) VALUES
('perm-sys-admin-1', 'role-system-admin', 'BCBS239_UPLOAD_FILES'),
('perm-sys-admin-2', 'role-system-admin', 'BCBS239_DOWNLOAD_FILES'),
('perm-sys-admin-3', 'role-system-admin', 'BCBS239_DELETE_FILES'),
('perm-sys-admin-4', 'role-system-admin', 'BCBS239_GENERATE_REPORTS'),
('perm-sys-admin-5', 'role-system-admin', 'BCBS239_VIEW_REPORTS'),
('perm-sys-admin-6', 'role-system-admin', 'BCBS239_EXPORT_REPORTS'),
('perm-sys-admin-7', 'role-system-admin', 'BCBS239_SCHEDULE_REPORTS'),
('perm-sys-admin-8', 'role-system-admin', 'BCBS239_CONFIGURE_PARAMETERS'),
('perm-sys-admin-9', 'role-system-admin', 'BCBS239_MANAGE_TEMPLATES'),
('perm-sys-admin-10', 'role-system-admin', 'BCBS239_CONFIGURE_WORKFLOWS'),
('perm-sys-admin-11', 'role-system-admin', 'BCBS239_MANAGE_VIOLATIONS'),
('perm-sys-admin-12', 'role-system-admin', 'BCBS239_APPROVE_VIOLATIONS'),
('perm-sys-admin-13', 'role-system-admin', 'BCBS239_VIEW_VIOLATIONS'),
('perm-sys-admin-14', 'role-system-admin', 'BCBS239_ADMINISTER_USERS'),
('perm-sys-admin-15', 'role-system-admin', 'BCBS239_ASSIGN_ROLES'),
('perm-sys-admin-16', 'role-system-admin', 'BCBS239_VIEW_AUDIT_LOGS'),
('perm-sys-admin-17', 'role-system-admin', 'BCBS239_VALIDATE_DATA'),
('perm-sys-admin-18', 'role-system-admin', 'BCBS239_APPROVE_DATA'),
('perm-sys-admin-19', 'role-system-admin', 'BCBS239_REJECT_DATA'),
('perm-sys-admin-20', 'role-system-admin', 'BCBS239_MANAGE_SYSTEM_CONFIG'),
('perm-sys-admin-21', 'role-system-admin', 'BCBS239_MONITOR_SYSTEM'),
('perm-sys-admin-22', 'role-system-admin', 'BCBS239_BACKUP_RESTORE'),
('perm-sys-admin-23', 'role-system-admin', 'BCBS239_SUBMIT_REGULATORY_REPORTS'),
('perm-sys-admin-24', 'role-system-admin', 'BCBS239_REVIEW_SUBMISSIONS'),
('perm-sys-admin-25', 'role-system-admin', 'BCBS239_TRACK_SUBMISSIONS'),
('perm-sys-admin-26', 'role-system-admin', 'BCBS239_MANAGE_BANK_CONFIG'),
('perm-sys-admin-27', 'role-system-admin', 'BCBS239_VIEW_CROSS_BANK_DATA'),
('perm-sys-admin-28', 'role-system-admin', 'BCBS239_CONSOLIDATE_REPORTS')
ON CONFLICT (role_id, permission) DO NOTHING;

-- Create user_roles table (junction table for many-to-many relationship)
CREATE TABLE IF NOT EXISTS iam.user_roles (
    id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES iam.users (id) ON DELETE CASCADE,
    UNIQUE (user_id, role_name)
);

-- Create indexes for user_roles table
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON iam.user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_name ON iam.user_roles (role_name);
