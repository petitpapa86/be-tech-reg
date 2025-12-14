-- Align IAM schema with JPA entities expecting UUID identifiers
-- Fixes runtime failure: operator does not exist: character varying = uuid

-- NOTE: Avoid requiring pgcrypto/uuid-ossp (often needs elevated privileges).
-- We'll generate UUIDs via md5+regexp_replace when we need a fallback.

-- 1) Prepare UUID mapping for users (keep the original string id in id_old)
ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS id_old VARCHAR(36),
    ADD COLUMN IF NOT EXISTS id_uuid UUID;

UPDATE iam.users
SET id_old = id::text
WHERE id_old IS NULL;

-- UUID regex (case-insensitive)
UPDATE iam.users
SET id_uuid = CASE
    WHEN id_old ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN id_old::uuid
    ELSE (regexp_replace(md5(random()::text || clock_timestamp()::text), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\\1-\\2-\\3-\\4-\\5'))::uuid
END
WHERE id_uuid IS NULL;

-- 2) Convert refresh_tokens.id + refresh_tokens.user_id to UUID (without re-adding FKs yet)
ALTER TABLE iam.refresh_tokens
    ADD COLUMN IF NOT EXISTS id_uuid UUID,
    ADD COLUMN IF NOT EXISTS user_id_uuid UUID;

UPDATE iam.refresh_tokens
SET id_uuid = CASE
    WHEN id::text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN (id::text)::uuid
    ELSE (regexp_replace(md5(random()::text || clock_timestamp()::text), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\\1-\\2-\\3-\\4-\\5'))::uuid
END
WHERE id_uuid IS NULL;

UPDATE iam.refresh_tokens rt
SET user_id_uuid = u.id_uuid
FROM iam.users u
WHERE rt.user_id_uuid IS NULL
    AND rt.user_id::text = u.id_old;

-- If any tokens reference missing users, drop them (otherwise NOT NULL + FK will fail)
DELETE FROM iam.refresh_tokens
WHERE user_id_uuid IS NULL;

-- Drop existing FK before swapping column types
ALTER TABLE iam.refresh_tokens DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user_id;

ALTER TABLE iam.refresh_tokens ALTER COLUMN id_uuid SET NOT NULL;
ALTER TABLE iam.refresh_tokens ALTER COLUMN user_id_uuid SET NOT NULL;

ALTER TABLE iam.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;
ALTER TABLE iam.refresh_tokens DROP COLUMN id;
ALTER TABLE iam.refresh_tokens RENAME COLUMN id_uuid TO id;
ALTER TABLE iam.refresh_tokens ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE iam.refresh_tokens DROP COLUMN user_id;
ALTER TABLE iam.refresh_tokens RENAME COLUMN user_id_uuid TO user_id;

-- 3) Convert user_roles.user_id to UUID and add missing columns expected by UserRoleEntity
ALTER TABLE iam.user_roles
    ADD COLUMN IF NOT EXISTS user_id_uuid UUID;

UPDATE iam.user_roles ur
SET user_id_uuid = u.id_uuid
FROM iam.users u
WHERE ur.user_id_uuid IS NULL
    AND ur.user_id::text = u.id_old;

-- If any assignments reference missing users, drop them (otherwise NOT NULL + FK will fail)
DELETE FROM iam.user_roles
WHERE user_id_uuid IS NULL;

ALTER TABLE iam.user_roles DROP CONSTRAINT IF EXISTS user_roles_user_id_fkey;

ALTER TABLE iam.user_roles ALTER COLUMN user_id_uuid SET NOT NULL;

ALTER TABLE iam.user_roles DROP COLUMN user_id;
ALTER TABLE iam.user_roles RENAME COLUMN user_id_uuid TO user_id;

-- Add new columns to support the newer entity mapping (keep legacy columns too)
ALTER TABLE iam.user_roles
    ADD COLUMN IF NOT EXISTS role_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255) DEFAULT 'default-org',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Ensure every referenced role_name exists in iam.roles so role_id can be backfilled
INSERT INTO iam.roles (id, name, display_name, description, level)
SELECT
    ('role-' || lower(replace(ur.role_name, '_', '-'))) AS id,
    ur.role_name AS name,
    ur.role_name AS display_name,
    'Auto-created role (migration backfill)' AS description,
    1 AS level
FROM iam.user_roles ur
LEFT JOIN iam.roles r ON r.name = ur.role_name
WHERE r.id IS NULL
GROUP BY ur.role_name
ON CONFLICT (name) DO NOTHING;

-- Backfill role_id based on role_name when possible
UPDATE iam.user_roles ur
SET role_id = r.id
FROM iam.roles r
WHERE ur.role_id IS NULL
  AND ur.role_name = r.name;

-- Fallback to VIEWER role if any role_id still missing
UPDATE iam.user_roles
SET role_id = 'role-viewer'
WHERE role_id IS NULL;

-- Ensure organization_id is non-null
UPDATE iam.user_roles
SET organization_id = 'default-org'
WHERE organization_id IS NULL;

ALTER TABLE iam.user_roles ALTER COLUMN organization_id SET NOT NULL;

-- 4) Switch primary key on users to UUID
ALTER TABLE iam.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE iam.users DROP COLUMN id;
ALTER TABLE iam.users RENAME COLUMN id_uuid TO id;
ALTER TABLE iam.users ADD CONSTRAINT users_pkey PRIMARY KEY (id);

-- 5) Re-create supporting indexes and foreign keys (now that users.id is UUID)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON iam.refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON iam.user_roles (user_id);

ALTER TABLE iam.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE;

ALTER TABLE iam.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE;

-- 6) Ensure the user_bank_assignments table exists (UserEntity has an eager OneToMany)
CREATE TABLE IF NOT EXISTS iam.user_bank_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    bank_id VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_bank_assignments_user_id
        FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_bank_assignments_user_id ON iam.user_bank_assignments (user_id);
CREATE INDEX IF NOT EXISTS idx_user_bank_assignments_bank_id ON iam.user_bank_assignments (bank_id);

-- Keep id_old for traceability; can be dropped later once stable.