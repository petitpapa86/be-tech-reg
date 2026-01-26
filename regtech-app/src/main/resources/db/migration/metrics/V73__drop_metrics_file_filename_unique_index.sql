-- Drop unique index on metrics_file.filename; filenames are not unique

DROP INDEX IF EXISTS metrics.metrics_file_filename_uq;

-- In case a constraint was created instead of an index
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
        WHERE tc.constraint_type = 'UNIQUE'
          AND tc.table_schema = 'metrics'
          AND tc.table_name = 'metrics_file'
          AND ccu.column_name = 'filename'
    ) THEN
        EXECUTE (
            SELECT 'ALTER TABLE metrics.metrics_file DROP CONSTRAINT ' || tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
            WHERE tc.constraint_type = 'UNIQUE'
              AND tc.table_schema = 'metrics'
              AND tc.table_name = 'metrics_file'
              AND ccu.column_name = 'filename'
            LIMIT 1
        );
    END IF;
END $$;
