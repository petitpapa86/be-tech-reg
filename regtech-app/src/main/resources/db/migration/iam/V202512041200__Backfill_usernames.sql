-- Backfill usernames from email for existing users with null username
-- Sets username to the user's email if username is NULL, truncated to 100 characters to respect schema
UPDATE iam.users
SET username = LEFT(email, 100)
WHERE username IS NULL;

-- Optionally ensure column is NOT NULL (uncomment if you want to enforce after backfill)
-- ALTER TABLE iam.users ALTER COLUMN username SET NOT NULL;
