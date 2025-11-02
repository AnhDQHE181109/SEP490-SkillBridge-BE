package com.skillbridge.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Password Encryption Service
 * 
 * This service demonstrates how BCrypt password hashing works.
 * BCrypt is a one-way hashing function, not encryption/decryption.
 * 
 * Key concepts:
 * - Hashing: One-way transformation (irreversible)
 * - Salt: Random data added to password before hashing
 * - Verification: Compare plain text with hash using matches()
 */
@Service
public class PasswordEncryptionService {

    private final PasswordEncoder passwordEncoder;

    public PasswordEncryptionService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hash a plain text password using BCrypt
     * 
     * Example:
     * Input: "myPassword123"
     * Output: "$2a$12$8k4J5J5J5J5J5J5J5J5J5ePwQrXtYyGlK3pLmN9OqRsT8uVwX2yZ6"
     * 
     * @param plainPassword The plain text password
     * @return BCrypt hashed password
     */
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify a plain text password against a BCrypt hash
     * 
     * This is how login verification works - we don't decrypt the stored hash,
     * instead we hash the provided password and compare the results.
     * 
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The stored BCrypt hash
     * @return true if passwords match, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Demonstrate password hashing with examples
     * This method shows how the same password produces different hashes
     * due to the random salt in BCrypt
     */
    public void demonstrateHashing() {
        String password = "admin123";
        
        // Hash the same password multiple times - each result will be different!
        String hash1 = hashPassword(password);
        String hash2 = hashPassword(password);
        String hash3 = hashPassword(password);
        
        System.out.println("=== BCrypt Password Hashing Demonstration ===");
        System.out.println("Original password: " + password);
        System.out.println("Hash #1: " + hash1);
        System.out.println("Hash #2: " + hash2);
        System.out.println("Hash #3: " + hash3);
        System.out.println();
        
        // Verify that all hashes match the original password
        System.out.println("=== Password Verification ===");
        System.out.println("Hash #1 matches password: " + verifyPassword(password, hash1));
        System.out.println("Hash #2 matches password: " + verifyPassword(password, hash2));
        System.out.println("Hash #3 matches password: " + verifyPassword(password, hash3));
        System.out.println("Wrong password matches: " + verifyPassword("wrongpass", hash1));
        System.out.println();
    }
}