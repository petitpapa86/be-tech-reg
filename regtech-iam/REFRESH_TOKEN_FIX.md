# Refresh Token Fix - Hash Algorithm Change

## Problem
The refresh token endpoint was returning 401 "REFRESH_TOKEN_NOT_FOUND" errors because of a fundamental flaw in the token storage and lookup mechanism.

### Root Cause
The code was using **BCrypt** to hash refresh tokens. BCrypt is a salted hashing algorithm, which means:
- Each time you hash the same input, you get a **different output**
- This is perfect for passwords (prevents rainbow table attacks)
- This is **terrible** for token lookup (you can't find the token by hashing it again)

### The Broken Flow
1. **Login**: Generate token → Hash with BCrypt → Store hash in database
2. **Refresh**: Receive token → Hash with BCrypt (produces NEW hash) → Try to find this NEW hash → **NOT FOUND**

## Solution
Changed from BCrypt (salted) to **SHA-256** (deterministic) for refresh token hashing.

### Why SHA-256?
- **Deterministic**: Same input always produces the same output
- **Allows lookup**: We can hash the incoming token and find it in the database
- **Secure**: SHA-256 is cryptographically secure for this use case
- **Fast**: Much faster than BCrypt for lookups

### Changes Made

#### 1. Updated `TokenPair.java`
- Changed token hashing from `passwordHasher.hash()` (BCrypt) to `createDeterministicHash()` (SHA-256)
- Added helper method `createDeterministicHash()` that uses SHA-256

#### 2. Updated `RefreshTokenCommandHandler.java`
- Changed lookup logic to use SHA-256 hash instead of BCrypt
- Added same `createDeterministicHash()` helper method

#### 3. Updated `IRefreshTokenRepository.java`
- Added `findValidTokensByUserId()` method for future optimization

#### 4. Updated `JpaRefreshTokenRepository.java`
- Implemented `findValidTokensByUserId()` method

#### 5. Created Database Migration
- `V202511261744__Clear_refresh_tokens_for_hash_change.sql`
- Clears all existing refresh tokens (they use BCrypt and can't be migrated)
- Users will need to log in again to get new tokens

## Security Considerations

### Is SHA-256 Secure Enough?
**Yes**, for these reasons:

1. **Tokens are cryptographically random**: Generated using `SecureRandom` with 32 bytes (256 bits)
2. **High entropy**: 2^256 possible values makes brute force impossible
3. **Short-lived**: Tokens expire after 7 days
4. **Token rotation**: Old tokens are revoked when refreshed
5. **Database protection**: Attacker needs database access to see hashes

### BCrypt vs SHA-256 for Tokens
- **BCrypt**: Best for passwords (user-chosen, low entropy, need slow hashing)
- **SHA-256**: Best for tokens (system-generated, high entropy, need fast lookup)

## Testing the Fix

### 1. Clear existing tokens
The migration will run automatically on next startup and clear all refresh tokens.

### 2. Test the flow
```bash
# 1. Login
POST /api/v1/auth/login
{
  "username": "user@example.com",
  "password": "password"
}
# Save the refreshToken from response

# 2. Refresh (should now work)
POST /api/v1/auth/refresh
{
  "refreshToken": "<token_from_login>"
}
# Should return new access and refresh tokens
```

## Impact
- **Breaking change**: All existing refresh tokens are invalidated
- **User impact**: Users must log in again
- **Benefit**: Refresh token endpoint now works correctly

## Future Improvements
1. Consider encoding userId in the refresh token itself (as JWT) for faster lookup
2. Add rate limiting on refresh endpoint
3. Consider adding refresh token families for better security
