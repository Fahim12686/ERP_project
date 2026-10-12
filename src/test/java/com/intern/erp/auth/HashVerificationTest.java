package com.intern.erp.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class HashVerificationTest {

    @Test
    public void testExistingHashAgainstDemo123() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String demoPassword = "demo123";
        
        // This is the hash from the existing MongoDB record (finance user)
        String existingHash = "$2a$10$t2oB5JLZUiugGKMOugri6.Puex8E44zgWnyDwUuk/5i8EBEvZy.IS";
        
        System.out.println("Testing if existing hash matches 'demo123':");
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches: " + passwordEncoder.matches(demoPassword, existingHash));
        
        if (passwordEncoder.matches(demoPassword, existingHash)) {
            System.out.println("✅ The existing hash DOES match demo123");
        } else {
            System.out.println("❌ The existing hash DOES NOT match demo123");
            System.out.println("This means the password in the database is NOT demo123");
        }
    }

    @Test
    public void testCurrentDatabaseHashAgainstDemo123() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String demoPassword = "demo123";
        
        // This is the CURRENT hash from the deployed database (all three users have this same hash)
        String currentHash = "$2a$10$iJXpv1mSwCmZQc80lKBA3eTHC17Nl1Y6uvyX1fuE.TEuZaLM/ebii";
        
        System.out.println("Testing if CURRENT database hash matches 'demo123':");
        System.out.println("Hash: " + currentHash);
        System.out.println("Matches: " + passwordEncoder.matches(demoPassword, currentHash));
        
        if (passwordEncoder.matches(demoPassword, currentHash)) {
            System.out.println("✅ The current hash DOES match demo123");
        } else {
            System.out.println("❌ The current hash DOES NOT match demo123");
            System.out.println("This is the problem - the password in the database is NOT demo123");
        }
    }
}
