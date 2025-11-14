# RegTech Roles and Permissions Reference

This document contains all the roles and their permissions for the RegTech system. Use this to populate your database tables when migrating from the deprecated `Bcbs239Role` enum to database-driven role configuration.

## Database Schema Reminder

```sql
-- Roles table
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    level INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Role permissions table
CREATE TABLE role_permissions (
    id VARCHAR(36) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE(role_id, permission)
);
```

## Roles and Permissions

### 1. VIEWER
**Level:** 1  
**Display Name:** Basic Viewer  
**Description:** Can only view reports and data - read-only access for basic users

**Permissions:**
- BCBS239_VIEW_REPORTS
- BCBS239_VIEW_VIOLATIONS

---

### 2. DATA_ANALYST
**Level:** 2  
**Display Name:** Data Analyst  
**Description:** Can upload files and view reports - handles data processing and analysis

**Permissions:**
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_VIEW_REPORTS
- BCBS239_GENERATE_REPORTS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_VALIDATE_DATA

---

### 3. AUDITOR
**Level:** 3  
**Display Name:** Auditor  
**Description:** Read-only access with audit capabilities - monitors system and tracks submissions

**Permissions:**
- BCBS239_VIEW_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_VIEW_AUDIT_LOGS
- BCBS239_TRACK_SUBMISSIONS
- BCBS239_MONITOR_SYSTEM

---

### 4. RISK_MANAGER
**Level:** 3  
**Display Name:** Risk Manager  
**Description:** Can manage violations and generate reports - handles risk assessment and mitigation

**Permissions:**
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_DELETE_FILES
- BCBS239_GENERATE_REPORTS
- BCBS239_VIEW_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_SCHEDULE_REPORTS
- BCBS239_MANAGE_VIOLATIONS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_VALIDATE_DATA
- BCBS239_APPROVE_DATA
- BCBS239_REJECT_DATA
- BCBS239_MANAGE_TEMPLATES

---

### 5. COMPLIANCE_OFFICER
**Level:** 4  
**Display Name:** Compliance Officer  
**Description:** Full compliance management capabilities - oversees regulatory compliance and reporting

**Permissions:**
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_DELETE_FILES
- BCBS239_GENERATE_REPORTS
- BCBS239_VIEW_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_SCHEDULE_REPORTS
- BCBS239_CONFIGURE_PARAMETERS
- BCBS239_MANAGE_TEMPLATES
- BCBS239_CONFIGURE_WORKFLOWS
- BCBS239_MANAGE_VIOLATIONS
- BCBS239_APPROVE_VIOLATIONS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_VALIDATE_DATA
- BCBS239_APPROVE_DATA
- BCBS239_REJECT_DATA
- BCBS239_SUBMIT_REGULATORY_REPORTS
- BCBS239_REVIEW_SUBMISSIONS
- BCBS239_TRACK_SUBMISSIONS
- BCBS239_VIEW_AUDIT_LOGS

---

### 6. BANK_ADMIN
**Level:** 4  
**Display Name:** Bank Administrator  
**Description:** Manages bank-specific configurations - administers bank-level settings and users

**Permissions:**
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_DELETE_FILES
- BCBS239_GENERATE_REPORTS
- BCBS239_VIEW_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_CONFIGURE_PARAMETERS
- BCBS239_MANAGE_TEMPLATES
- BCBS239_MANAGE_VIOLATIONS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_ADMINISTER_USERS
- BCBS239_ASSIGN_ROLES
- BCBS239_MANAGE_BANK_CONFIG
- BCBS239_VIEW_AUDIT_LOGS

---

### 7. HOLDING_COMPANY_USER
**Level:** 4  
**Display Name:** Holding Company User  
**Description:** Can view across multiple banks - access to consolidated data and reports

**Permissions:**
- BCBS239_VIEW_REPORTS
- BCBS239_GENERATE_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_VIEW_CROSS_BANK_DATA
- BCBS239_CONSOLIDATE_REPORTS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_TRACK_SUBMISSIONS

---

### 8. SYSTEM_ADMIN
**Level:** 5  
**Display Name:** System Administrator  
**Description:** Full system access - complete administrative control over the entire system

**Permissions:**
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_DELETE_FILES
- BCBS239_GENERATE_REPORTS
- BCBS239_VIEW_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_SCHEDULE_REPORTS
- BCBS239_CONFIGURE_PARAMETERS
- BCBS239_MANAGE_TEMPLATES
- BCBS239_CONFIGURE_WORKFLOWS
- BCBS239_MANAGE_VIOLATIONS
- BCBS239_APPROVE_VIOLATIONS
- BCBS239_VIEW_VIOLATIONS
- BCBS239_ADMINISTER_USERS
- BCBS239_ASSIGN_ROLES
- BCBS239_VIEW_AUDIT_LOGS
- BCBS239_VALIDATE_DATA
- BCBS239_APPROVE_DATA
- BCBS239_REJECT_DATA
- BCBS239_MANAGE_SYSTEM_CONFIG
- BCBS239_MONITOR_SYSTEM
- BCBS239_BACKUP_RESTORE
- BCBS239_SUBMIT_REGULATORY_REPORTS
- BCBS239_REVIEW_SUBMISSIONS
- BCBS239_TRACK_SUBMISSIONS
- BCBS239_MANAGE_BANK_CONFIG
- BCBS239_VIEW_CROSS_BANK_DATA
- BCBS239_CONSOLIDATE_REPORTS

## SQL Insert Statements

Use these INSERT statements to populate your database:

```sql
-- Insert roles
INSERT INTO roles (id, name, display_name, description, level) VALUES
('role-viewer', 'VIEWER', 'Basic Viewer', 'Can only view reports and data - read-only access for basic users', 1),
('role-data-analyst', 'DATA_ANALYST', 'Data Analyst', 'Can upload files and view reports - handles data processing and analysis', 2),
('role-auditor', 'AUDITOR', 'Auditor', 'Read-only access with audit capabilities - monitors system and tracks submissions', 3),
('role-risk-manager', 'RISK_MANAGER', 'Risk Manager', 'Can manage violations and generate reports - handles risk assessment and mitigation', 3),
('role-compliance-officer', 'COMPLIANCE_OFFICER', 'Compliance Officer', 'Full compliance management capabilities - oversees regulatory compliance and reporting', 4),
('role-bank-admin', 'BANK_ADMIN', 'Bank Administrator', 'Manages bank-specific configurations - administers bank-level settings and users', 4),
('role-holding-company', 'HOLDING_COMPANY_USER', 'Holding Company User', 'Can view across multiple banks - access to consolidated data and reports', 4),
('role-system-admin', 'SYSTEM_ADMIN', 'System Administrator', 'Full system access - complete administrative control over the entire system', 5);

-- Insert permissions for VIEWER
INSERT INTO role_permissions (id, role_id, permission) VALUES
('perm-viewer-1', 'role-viewer', 'BCBS239_VIEW_REPORTS'),
('perm-viewer-2', 'role-viewer', 'BCBS239_VIEW_VIOLATIONS');

-- Insert permissions for DATA_ANALYST
INSERT INTO role_permissions (id, role_id, permission) VALUES
('perm-analyst-1', 'role-data-analyst', 'BCBS239_UPLOAD_FILES'),
('perm-analyst-2', 'role-data-analyst', 'BCBS239_DOWNLOAD_FILES'),
('perm-analyst-3', 'role-data-analyst', 'BCBS239_VIEW_REPORTS'),
('perm-analyst-4', 'role-data-analyst', 'BCBS239_GENERATE_REPORTS'),
('perm-analyst-5', 'role-data-analyst', 'BCBS239_VIEW_VIOLATIONS'),
('perm-analyst-6', 'role-data-analyst', 'BCBS239_VALIDATE_DATA');

-- Insert permissions for AUDITOR
INSERT INTO role_permissions (id, role_id, permission) VALUES
('perm-auditor-1', 'role-auditor', 'BCBS239_VIEW_REPORTS'),
('perm-auditor-2', 'role-auditor', 'BCBS239_EXPORT_REPORTS'),
('perm-auditor-3', 'role-auditor', 'BCBS239_VIEW_VIOLATIONS'),
('perm-auditor-4', 'role-auditor', 'BCBS239_VIEW_AUDIT_LOGS'),
('perm-auditor-5', 'role-auditor', 'BCBS239_TRACK_SUBMISSIONS'),
('perm-auditor-6', 'role-auditor', 'BCBS239_MONITOR_SYSTEM');

-- Insert permissions for RISK_MANAGER
INSERT INTO role_permissions (id, role_id, permission) VALUES
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
('perm-risk-13', 'role-risk-manager', 'BCBS239_MANAGE_TEMPLATES');

-- Insert permissions for COMPLIANCE_OFFICER
INSERT INTO role_permissions (id, role_id, permission) VALUES
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
('perm-compliance-20', 'role-compliance-officer', 'BCBS239_VIEW_AUDIT_LOGS');

-- Insert permissions for BANK_ADMIN
INSERT INTO role_permissions (id, role_id, permission) VALUES
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
('perm-bank-admin-14', 'role-bank-admin', 'BCBS239_VIEW_AUDIT_LOGS');

-- Insert permissions for HOLDING_COMPANY_USER
INSERT INTO role_permissions (id, role_id, permission) VALUES
('perm-holding-1', 'role-holding-company', 'BCBS239_VIEW_REPORTS'),
('perm-holding-2', 'role-holding-company', 'BCBS239_GENERATE_REPORTS'),
('perm-holding-3', 'role-holding-company', 'BCBS239_EXPORT_REPORTS'),
('perm-holding-4', 'role-holding-company', 'BCBS239_VIEW_CROSS_BANK_DATA'),
('perm-holding-5', 'role-holding-company', 'BCBS239_CONSOLIDATE_REPORTS'),
('perm-holding-6', 'role-holding-company', 'BCBS239_VIEW_VIOLATIONS'),
('perm-holding-7', 'role-holding-company', 'BCBS239_TRACK_SUBMISSIONS');

-- Insert permissions for SYSTEM_ADMIN
INSERT INTO role_permissions (id, role_id, permission) VALUES
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
('perm-sys-admin-28', 'role-system-admin', 'BCBS239_CONSOLIDATE_REPORTS');
```

## Permission Summary by Category

### File Operations
- BCBS239_UPLOAD_FILES
- BCBS239_DOWNLOAD_FILES
- BCBS239_DELETE_FILES

### Reporting
- BCBS239_VIEW_REPORTS
- BCBS239_GENERATE_REPORTS
- BCBS239_EXPORT_REPORTS
- BCBS239_SCHEDULE_REPORTS
- BCBS239_CONSOLIDATE_REPORTS

### Data Management
- BCBS239_VALIDATE_DATA
- BCBS239_APPROVE_DATA
- BCBS239_REJECT_DATA

### Compliance & Violations
- BCBS239_VIEW_VIOLATIONS
- BCBS239_MANAGE_VIOLATIONS
- BCBS239_APPROVE_VIOLATIONS

### System Administration
- BCBS239_ADMINISTER_USERS
- BCBS239_ASSIGN_ROLES
- BCBS239_MANAGE_SYSTEM_CONFIG
- BCBS239_MANAGE_BANK_CONFIG
- BCBS239_MONITOR_SYSTEM
- BCBS239_BACKUP_RESTORE

### Regulatory Reporting
- BCBS239_SUBMIT_REGULATORY_REPORTS
- BCBS239_REVIEW_SUBMISSIONS
- BCBS239_TRACK_SUBMISSIONS

### Configuration
- BCBS239_CONFIGURE_PARAMETERS
- BCBS239_MANAGE_TEMPLATES
- BCBS239_CONFIGURE_WORKFLOWS

### Audit & Monitoring
- BCBS239_VIEW_AUDIT_LOGS
- BCBS239_VIEW_CROSS_BANK_DATA

## Migration Notes

When migrating from the enum-based system:

1. Run the INSERT statements above to populate your roles and permissions tables
2. Update existing user bank assignments to reference role IDs instead of role names
3. Test that all permissions are correctly loaded by the RolePermissionService
4. Gradually phase out the Bcbs239Role enum dependency
5. Update any hardcoded role references in your application code

This reference provides everything you need to set up a fully database-driven role and permission system!</content>
<parameter name="filePath">C:\Users\alseny\Desktop\react projects\regtech\ROLES_REFERENCE.md