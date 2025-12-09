package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt-based implementation of PasswordHasher
 * 
 * Uses BCrypt with work factor 12 for secure password hashing.
 * BCrypt provides:
 * - Automatic salt generation
 * - Constant-time comparison to prevent timing attacks
 * - Configurable work factor for future-proofing against hardware improvements
 * 
 * Requirements: 5.3, 8.5, 12.1, 12.2, 12.3
 */
@Component
public class PasswordHasherImpl implements PasswordHasher {
    
    /**
     * BCrypt work factor of 12 provides a good balance between security and performance.
     * This results in 2^12 = 4096 iterations of the BCrypt algorithm.
     */
    private static final int BCRYPT_WORK_FACTOR = 12;
    
    private final BCryptPasswordEncoder encoder;
    
    public PasswordHasherImpl() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_WORK_FACTOR);
    }
    
    /**
     * Hashes a plaintext password using BCrypt with work factor 12.
     * 
     * BCrypt automatically generates a unique salt for each password,
     * which is embedded in the resulting hash string.
     * 
     * @param plaintext The plaintext password to hash
     * @return BCrypt hash string (includes salt and work factor)
     */
    @Override
    public String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return encoder.encode(plaintext);
    }
    
    /**
     * Verifies a plaintext password against a BCrypt hash.
     * 
     * Uses constant-time comparison to prevent timing attacks.
     * BCrypt's matches() method internally uses a constant-time comparison
     * to ensure that the verification time does not leak information about
     * the password.
     * 
     * @param plaintext The plaintext password to verify
     * @param hash The BCrypt hash to verify against
     * @return true if the password matches the hash, false otherwise
     */
    @Override
    public boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        
        try {
            // BCrypt.matches() uses constant-time comparison internally
            return encoder.matches(plaintext, hash);
        } catch (Exception e) {
            // Invalid hash format or other BCrypt errors
            return false;
        }
    }
}
