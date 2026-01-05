-- Add Italian translations to existing roles and permissions system

-- 1. Add Italian columns to roles table
ALTER TABLE iam.roles 
ADD COLUMN IF NOT EXISTS display_name_it VARCHAR(100),
ADD COLUMN IF NOT EXISTS description_it TEXT;

-- 2. Update existing roles with Italian translations
UPDATE iam.roles SET 
    display_name_it = 'Visualizzatore Base',
    description_it = 'Può solo visualizzare report e dati - accesso in sola lettura per utenti di base'
WHERE name = 'VIEWER';

UPDATE iam.roles SET 
    display_name_it = 'Analista Dati',
    description_it = 'Può caricare file e visualizzare report - gestisce elaborazione e analisi dati'
WHERE name = 'DATA_ANALYST';

UPDATE iam.roles SET 
    display_name_it = 'Revisore',
    description_it = 'Accesso in sola lettura con capacità di audit - monitora sistema e traccia sottomissioni'
WHERE name = 'AUDITOR';

UPDATE iam.roles SET 
    display_name_it = 'Responsabile Rischi',
    description_it = 'Può gestire violazioni e generare report - gestisce valutazione e mitigazione rischi'
WHERE name = 'RISK_MANAGER';

UPDATE iam.roles SET 
    display_name_it = 'Responsabile Compliance',
    description_it = 'Capacità complete di gestione compliance - supervisiona conformità normativa e reporting'
WHERE name = 'COMPLIANCE_OFFICER';

UPDATE iam.roles SET 
    display_name_it = 'Amministratore Banca',
    description_it = 'Gestisce configurazioni specifiche della banca - amministra impostazioni e utenti a livello banca'
WHERE name = 'BANK_ADMIN';

UPDATE iam.roles SET 
    display_name_it = 'Utente Holding',
    description_it = 'Può visualizzare attraverso più banche - accesso a dati consolidati e report'
WHERE name = 'HOLDING_COMPANY_USER';

UPDATE iam.roles SET 
    display_name_it = 'Amministratore Sistema',
    description_it = 'Accesso completo al sistema - controllo amministrativo completo su tutto il sistema'
WHERE name = 'SYSTEM_ADMIN';

-- 3. Create permission translations table
CREATE TABLE IF NOT EXISTS iam.permission_translations (
    permission_code VARCHAR(100) PRIMARY KEY,
    display_name_en VARCHAR(200) NOT NULL,
    display_name_it VARCHAR(200) NOT NULL,
    description_it TEXT,
    category VARCHAR(50) NOT NULL
);

-- 4. Insert permission translations (only Italian needed - English from permission code)
INSERT INTO iam.permission_translations (permission_code, display_name_en, display_name_it, description_it, category) VALUES
-- File Operations
('BCBS239_UPLOAD_FILES', 'Upload Files', 'Carica File', 'Carica file dati per elaborazione', 'FILES'),
('BCBS239_DOWNLOAD_FILES', 'Download Files', 'Scarica File', 'Scarica file dati', 'FILES'),
('BCBS239_DELETE_FILES', 'Delete Files', 'Elimina File', 'Elimina file dati', 'FILES'),

-- Report Operations
('BCBS239_VIEW_REPORTS', 'View Reports', 'Visualizza Report', 'Visualizza report generati', 'REPORTS'),
('BCBS239_GENERATE_REPORTS', 'Generate Reports', 'Genera Report', 'Genera nuovi report', 'REPORTS'),
('BCBS239_EXPORT_REPORTS', 'Export Reports', 'Esporta Report', 'Esporta report in formati esterni', 'REPORTS'),
('BCBS239_SCHEDULE_REPORTS', 'Schedule Reports', 'Pianifica Report', 'Pianifica generazione automatica report', 'REPORTS'),

-- Configuration
('BCBS239_CONFIGURE_PARAMETERS', 'Configure Parameters', 'Configura Parametri', 'Configura parametri di sistema', 'CONFIG'),
('BCBS239_MANAGE_TEMPLATES', 'Manage Templates', 'Gestisci Template', 'Gestisci template report', 'CONFIG'),
('BCBS239_CONFIGURE_WORKFLOWS', 'Configure Workflows', 'Configura Flussi', 'Configura flussi di lavoro', 'CONFIG'),
('BCBS239_MANAGE_BANK_CONFIG', 'Manage Bank Config', 'Gestisci Config Banca', 'Gestisci configurazione banca', 'CONFIG'),
('BCBS239_MANAGE_SYSTEM_CONFIG', 'Manage System Config', 'Gestisci Config Sistema', 'Gestisci configurazione sistema', 'CONFIG'),

-- Violations
('BCBS239_VIEW_VIOLATIONS', 'View Violations', 'Visualizza Violazioni', 'Visualizza violazioni conformità', 'VIOLATIONS'),
('BCBS239_MANAGE_VIOLATIONS', 'Manage Violations', 'Gestisci Violazioni', 'Gestisci violazioni conformità', 'VIOLATIONS'),
('BCBS239_APPROVE_VIOLATIONS', 'Approve Violations', 'Approva Violazioni', 'Approva risoluzioni violazioni', 'VIOLATIONS'),

-- Data Management
('BCBS239_VALIDATE_DATA', 'Validate Data', 'Valida Dati', 'Valida qualità dati', 'DATA'),
('BCBS239_APPROVE_DATA', 'Approve Data', 'Approva Dati', 'Approva dati per sottomissione', 'DATA'),
('BCBS239_REJECT_DATA', 'Reject Data', 'Rifiuta Dati', 'Rifiuta sottomissioni dati', 'DATA'),

-- User Administration
('BCBS239_ADMINISTER_USERS', 'Administer Users', 'Amministra Utenti', 'Gestisci account utenti', 'USERS'),
('BCBS239_ASSIGN_ROLES', 'Assign Roles', 'Assegna Ruoli', 'Assegna ruoli agli utenti', 'USERS'),

-- Audit & Monitoring
('BCBS239_VIEW_AUDIT_LOGS', 'View Audit Logs', 'Visualizza Log Audit', 'Visualizza log audit sistema', 'AUDIT'),
('BCBS239_MONITOR_SYSTEM', 'Monitor System', 'Monitora Sistema', 'Monitora salute e prestazioni sistema', 'AUDIT'),
('BCBS239_TRACK_SUBMISSIONS', 'Track Submissions', 'Traccia Sottomissioni', 'Traccia sottomissioni normative', 'AUDIT'),

-- Regulatory
('BCBS239_SUBMIT_REGULATORY_REPORTS', 'Submit Reports', 'Sottometti Report', 'Sottometti report alle autorità', 'REGULATORY'),
('BCBS239_REVIEW_SUBMISSIONS', 'Review Submissions', 'Rivedi Sottomissioni', 'Rivedi sottomissioni normative', 'REGULATORY'),

-- Cross-Bank (Holding)
('BCBS239_VIEW_CROSS_BANK_DATA', 'View Cross-Bank Data', 'Visualizza Dati Multi-Banca', 'Visualizza dati su più banche', 'HOLDING'),
('BCBS239_CONSOLIDATE_REPORTS', 'Consolidate Reports', 'Consolida Report', 'Consolida report da più banche', 'HOLDING'),

-- System Administration
('BCBS239_BACKUP_RESTORE', 'Backup & Restore', 'Backup e Ripristino', 'Esegui backup e ripristino sistema', 'SYSTEM')
ON CONFLICT (permission_code) DO NOTHING;
