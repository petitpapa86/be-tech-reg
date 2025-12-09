package com.bcbs239.regtech.iam.domain.authentication;

/**
 * PasswordHasher interface - defines password hashing operations
 * Implementation will be provided by infrastructure layer
 */
public interface PasswordHasher {
    /**
     * Hashes a plaintext password
     */
    String hash(String plaintext);

    /**
     * Verifies a plaintext password against a hash
     */
    boolean verify(String plaintext, String hash);
}
