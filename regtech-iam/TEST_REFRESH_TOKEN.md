# Testing the Refresh Token Fix

## The Problem
You're getting "REFRESH_TOKEN_NOT_FOUND" because you're trying to use an OLD refresh token that was created with BCrypt hashing.

## Why It's Not Working
1. **Old tokens** (before the fix): Created with BCrypt → Can't be looked up
2. **New tokens** (after the fix): Created with SHA-256 → Can be looked up
3. **Your current token**: Was created BEFORE the fix, so it uses BCrypt

## Solution: Get a New Token

### Step 1: Restart the Application
The database migration needs to run to clear old tokens:
```bash
# Stop the application if running
# Then start it again
mvn spring-boot:run -pl regtech-app
```

### Step 2: Login Again
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

Save the `refreshToken` from the response. This new token will use SHA-256.

### Step 3: Test Refresh
```bash
POST http://localhost:8080/api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<NEW_TOKEN_FROM_STEP_2>"
}
```

This should now work!

## Verification

### Check the token hash in database
After login, you can verify the token is stored with SHA-256:

```sql
SELECT 
    id,
    user_id,
    LEFT(token_hash, 20) as hash_preview,
    LENGTH(token_hash) as hash_length,
    created_at,
    expires_at,
    revoked
FROM refresh_tokens
ORDER BY created_at DESC
LIMIT 5;
```

SHA-256 Base64-encoded hashes are 44 characters long.
BCrypt hashes start with `$2a$` and are 60 characters long.

## What Changed

### Before (BCrypt)
```
Token: "abc123..."
Hash:  "$2a$12$randomsalt..." (different every time)
Lookup: Hash again → "$2a$12$differentsalt..." → NOT FOUND ❌
```

### After (SHA-256)
```
Token: "abc123..."
Hash:  "XXLxFfx7bm..." (same every time)
Lookup: Hash again → "XXLxFfx7bm..." → FOUND ✅
```

## Common Issues

### "Still not working after login"
- Make sure you're using the NEW refresh token from the login response
- Don't use old tokens from before the fix

### "Migration didn't run"
- Check application logs for Flyway migration messages
- Verify the migration file exists: `V202511261744__Clear_refresh_tokens_for_hash_change.sql`

### "Database still has old tokens"
- The migration should have cleared them
- You can manually clear: `DELETE FROM refresh_tokens;`
