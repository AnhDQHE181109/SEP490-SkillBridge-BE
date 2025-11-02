package com.skillbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * BCrypt Password Encoder Bean
     * 
     * BCrypt is a password hashing function designed by Niels Provos and David Mazières.
     * It incorporates a salt to protect against rainbow table attacks.
     * 
     * Features:
     * - One-way hashing (irreversible)
     * - Automatic salt generation
     * - Configurable work factor (strength = 12 here)
     * - Time-tested security standard
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 = 2^12 = 4096 rounds (good balance of security vs performance)
        return new BCryptPasswordEncoder(12);
    }
}