package com.intern.erp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderTest {

    @Test
    public void testBCryptEncodingAndMatching() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String demoPassword = "demo123";
        
        // Encode the password
        String encodedPassword = passwordEncoder.encode(demoPassword);
        
        // Verify the encoded password is not the same as plaintext
        assertNotEquals(demoPassword, encodedPassword);
        
        // Verify the encoded password matches the original
        assertTrue(passwordEncoder.matches(demoPassword, encodedPassword));
        
        // Verify wrong password does not match
        assertFalse(passwordEncoder.matches("wrongpassword", encodedPassword));
    }

    @Test
    public void testMultipleEncodingsProduceDifferentHashes() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String demoPassword = "demo123";
        
        String hash1 = passwordEncoder.encode(demoPassword);
        String hash2 = passwordEncoder.encode(demoPassword);
        
        // BCrypt uses a random salt, so hashes should be different
        assertNotEquals(hash1, hash2);
        
        // But both should match the original password
        assertTrue(passwordEncoder.matches(demoPassword, hash1));
        assertTrue(passwordEncoder.matches(demoPassword, hash2));
    }
}
