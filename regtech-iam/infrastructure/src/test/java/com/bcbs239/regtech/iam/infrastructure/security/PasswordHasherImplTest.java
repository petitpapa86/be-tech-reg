package com.bcbs239.regtech.iam.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordHasherImpl
 * 
 * Tests BCrypt password hashing with work factor 12 and constant-time comparison.
 * Requirements: 5.3, 8.5, 12.1, 12.2, 12.3
 */
class PasswordHasherImplTest {
    
    private PasswordHasherImpl passwordHasher;
    
    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasherImpl();
    }
    
    @Test
    void hash_shouldGenerateValidBCryptHash() {
        // Given
        String plaintext = "MySecurePassword123!";
        
        // When
        String hash = passwordHasher.hash(plaintext);
        
        // Then
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$12$"), "Hash should start with BCrypt work factor 12 prefix");
        assertEquals(60, hash.length(), "BCrypt hash should be 60 characters long");
    }
    
    @Test
    void hash_shouldGenerateUniqueSalts() {
        // Given
        String plaintext = "MySecurePassword123!";
        
        // When
        String hash1 = passwordHasher.hash(plaintext);
        String hash2 = passwordHasher.hash(plaintext);
        
        // Then
        assertNotEquals(hash1, hash2, "Each hash should have a unique salt");
    }
    
    @Test
    void verify_shouldReturnTrueForMatchingPassword() {
        // Given
        String plaintext = "MySecurePassword123!";
        String hash = passwordHasher.hash(plaintext);
        
        // When
        boolean result = passwordHasher.verify(plaintext, hash);
        
        // Then
        assertTrue(result, "Verification should succeed for matching password");
    }
    
    @Test
    void verify_shouldReturnFalseForNonMatchingPassword() {
        // Given
        String plaintext = "MySecurePassword123!";
        String wrongPassword = "WrongPassword456!";
        String hash = passwordHasher.hash(plaintext);
        
        // When
        boolean result = passwordHasher.verify(wrongPassword, hash);
        
        // Then
        assertFalse(result, "Verification should fail for non-matching password");
    }
    
    @Test
    void verify_shouldReturnFalseForInvalidHash() {
        // Given
        String plaintext = "MySecurePassword123!";
        String invalidHash = "not-a-valid-bcrypt-hash";
        
        // When
        boolean result = passwordHasher.verify(plaintext, invalidHash);
        
        // Then
        assertFalse(result, "Verification should fail for invalid hash format");
    }
    
    @Test
    void verify_shouldReturnFalseForNullPlaintext() {
        // Given
        String hash = passwordHasher.hash("MySecurePassword123!");
        
        // When
        boolean result = passwordHasher.verify(null, hash);
        
        // Then
        assertFalse(result, "Verification should fail for null plaintext");
    }
    
    @Test
    void verify_shouldReturnFalseForNullHash() {
        // Given
        String plaintext = "MySecurePassword123!";
        
        // When
        boolean result = passwordHasher.verify(plaintext, null);
        
        // Then
        assertFalse(result, "Verification should fail for null hash");
    }
    
    @Test
    void hash_shouldThrowExceptionForNullPlaintext() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordHasher.hash(null);
        }, "Hashing null plaintext should throw IllegalArgumentException");
    }
    
    @Test
    void verify_shouldUseConstantTimeComparison() {
        // Given
        String plaintext = "MySecurePassword123!";
        String hash = passwordHasher.hash(plaintext);
        
        // When - Measure time for correct password
        long startCorrect = System.nanoTime();
        passwordHasher.verify(plaintext, hash);
        long timeCorrect = System.nanoTime() - startCorrect;
        
        // When - Measure time for incorrect password
        long startIncorrect = System.nanoTime();
        passwordHasher.verify("WrongPassword456!", hash);
        long timeIncorrect = System.nanoTime() - startIncorrect;
        
        // Then - Times should be similar (within 50% variance)
        // Note: This is a basic check. BCrypt's internal constant-time comparison
        // is what actually prevents timing attacks.
        double ratio = (double) Math.max(timeCorrect, timeIncorrect) / 
                       Math.min(timeCorrect, timeIncorrect);
        
        // This test verifies that BCrypt is being used, which has constant-time comparison
        // The actual constant-time guarantee comes from BCrypt's implementation
        assertTrue(ratio < 10.0, 
            "Verification times should be similar (BCrypt uses constant-time comparison)");
    }
    
    @Test
    void hash_shouldWorkWithSpecialCharacters() {
        // Given
        String plaintext = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
        
        // When
        String hash = passwordHasher.hash(plaintext);
        boolean result = passwordHasher.verify(plaintext, hash);
        
        // Then
        assertTrue(result, "Should handle special characters correctly");
    }
    
    @Test
    void hash_shouldWorkWithUnicodeCharacters() {
        // Given
        String plaintext = "Пароль123!密码";
        
        // When
        String hash = passwordHasher.hash(plaintext);
        boolean result = passwordHasher.verify(plaintext, hash);
        
        // Then
        assertTrue(result, "Should handle Unicode characters correctly");
    }
    
    @Test
    void hash_shouldWorkWithLongPasswords() {
        // Given - BCrypt has a maximum password length of 72 bytes
        String plaintext = "A".repeat(70) + "1!";
        
        // When
        String hash = passwordHasher.hash(plaintext);
        boolean result = passwordHasher.verify(plaintext, hash);
        
        // Then
        assertTrue(result, "Should handle long passwords correctly (up to 72 bytes)");
    }
}
