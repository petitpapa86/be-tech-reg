-- V202512031000__Convert_users_id_to_uuid.sql
-- Convert users.id from VARCHAR to UUID

-- Drop all foreign key constraints that reference users.id from any table
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT con.conname, rel.relname as table_name
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE con.contype = 'f'
          AND con.confrelid = (SELECT oid FROM pg_class WHERE relname = 'users')
          AND con.confkey[1] = (SELECT attnum FROM pg_attribute
                               WHERE attrelid = (SELECT oid FROM pg_class WHERE relname = 'users')
                               AND attname = 'id')
          AND rel.relname IN (SELECT tablename FROM pg_tables WHERE schemaname = 'iam')
    LOOP
        RAISE NOTICE 'Dropping constraint % from table %', constraint_record.conname, constraint_record.table_name;
        EXECUTE 'ALTER TABLE iam.' || constraint_record.table_name || ' DROP CONSTRAINT ' || constraint_record.conname;
    END LOOP;
END $$;

-- Convert the users.id column to UUID
ALTER TABLE iam.users ALTER COLUMN id TYPE UUID USING id::uuid;

-- Convert the refresh_tokens.user_id column to UUID
ALTER TABLE iam.refresh_tokens ALTER COLUMN user_id TYPE UUID USING user_id::uuid;

-- Recreate the foreign key constraint
ALTER TABLE iam.refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user_id
    FOREIGN KEY (user_id) REFERENCES iam.users(id);