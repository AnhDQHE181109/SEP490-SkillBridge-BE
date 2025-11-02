package com.skillbridge.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Password Service
 * Handles password generation and hashing
 */
@Service
public class PasswordService {

    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*";
    private static final String ALL_CHARS = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS;
    private static final int PASSWORD_LENGTH = 12;

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a random secure password
     * @return Generated plain text password
     */
    public String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        
        // Ensure at least one character from each character set
        password.append(getRandomChar(UPPER_CASE));
        password.append(getRandomChar(LOWER_CASE));
        password.append(getRandomChar(DIGITS));
        password.append(getRandomChar(SPECIAL_CHARS));
        
        // Fill the rest with random characters from all sets
        for (int i = password.length(); i < PASSWORD_LENGTH; i++) {
            password.append(getRandomChar(ALL_CHARS));
        }
        
        // Shuffle the password to avoid predictable pattern
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    /**
     * Hash a password using BCrypt
     * @param plainPassword Plain text password
     * @return Hashed password
     */
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify if a password matches the hashed password
     * @param plainPassword Plain text password
     * @param hashedPassword Hashed password
     * @return true if matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Get a random character from the given character set
     * @param charSet Character set to choose from
     * @return Random character
     */
    private char getRandomChar(String charSet) {
        return charSet.charAt(secureRandom.nextInt(charSet.length()));
    }
}

