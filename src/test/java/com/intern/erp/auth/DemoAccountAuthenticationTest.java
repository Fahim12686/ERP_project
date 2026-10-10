package com.intern.erp.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.intern.erp.outbox.OutboxService;
import com.intern.erp.security.JwtUtil;
import com.intern.erp.users.model.UserAccount;
import com.intern.erp.users.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
public class DemoAccountAuthenticationTest {

    @Mock
    private UserAccountRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxService outboxService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private static final String DEMO_PASSWORD = "demo123";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @Test
    public void testAdminLoginWithCorrectCredentials() {
        // Arrange
        UserAccount adminUser = createDemoUser("admin", "admin@company.com", "ADMIN");
        when(userRepo.findByUsername("admin@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("admin@company.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(DEMO_PASSWORD, adminUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("test-jwt-token");

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("admin@company.com", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthController.LoginResponse loginResponse = (AuthController.LoginResponse) response.getBody();
        assertNotNull(loginResponse);
        assertTrue(loginResponse.success());
        assertEquals("admin", loginResponse.username());
        assertTrue(loginResponse.roles().contains("ROLE_ADMIN"));
        assertNotNull(loginResponse.token());
    }

    @Test
    public void testHRLoginWithCorrectCredentials() {
        // Arrange
        UserAccount hrUser = createDemoUser("hr", "hr@company.com", "HR");
        when(userRepo.findByUsername("hr@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("hr@company.com")).thenReturn(Optional.of(hrUser));
        when(passwordEncoder.matches(DEMO_PASSWORD, hrUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("test-jwt-token");

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("hr@company.com", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthController.LoginResponse loginResponse = (AuthController.LoginResponse) response.getBody();
        assertNotNull(loginResponse);
        assertTrue(loginResponse.success());
        assertEquals("hr", loginResponse.username());
        assertTrue(loginResponse.roles().contains("ROLE_HR"));
    }

    @Test
    public void testFinanceLoginWithCorrectCredentials() {
        // Arrange
        UserAccount financeUser = createDemoUser("finance", "finance@company.com", "FINANCE");
        when(userRepo.findByUsername("finance@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("finance@company.com")).thenReturn(Optional.of(financeUser));
        when(passwordEncoder.matches(DEMO_PASSWORD, financeUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("test-jwt-token");

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("finance@company.com", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthController.LoginResponse loginResponse = (AuthController.LoginResponse) response.getBody();
        assertNotNull(loginResponse);
        assertTrue(loginResponse.success());
        assertEquals("finance", loginResponse.username());
        assertTrue(loginResponse.roles().contains("ROLE_FINANCE"));
    }

    @Test
    public void testLoginWithWrongPassword() {
        // Arrange
        UserAccount adminUser = createDemoUser("admin", "admin@company.com", "ADMIN");
        when(userRepo.findByUsername("admin@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("admin@company.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(WRONG_PASSWORD, adminUser.getPassword())).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("admin@company.com", WRONG_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testLoginWithNonExistentUser() {
        // Arrange
        when(userRepo.findByUsername("nonexistent@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("nonexistent@company.com")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("nonexistent@company.com", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testLoginWithDisabledUser() {
        // Arrange
        UserAccount disabledUser = createDemoUser("admin", "admin@company.com", "ADMIN");
        disabledUser.setEnabled(false);
        when(userRepo.findByUsername("admin@company.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("admin@company.com")).thenReturn(Optional.of(disabledUser));

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("admin@company.com", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testLoginByUsernameInsteadOfEmail() {
        // Arrange
        UserAccount adminUser = createDemoUser("admin", "admin@company.com", "ADMIN");
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(DEMO_PASSWORD, adminUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("test-jwt-token");

        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest("admin", DEMO_PASSWORD)
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthController.LoginResponse loginResponse = (AuthController.LoginResponse) response.getBody();
        assertNotNull(loginResponse);
        assertTrue(loginResponse.success());
    }

    @Test
    public void testLoginWithMissingCredentials() {
        // Act
        ResponseEntity<?> response = authController.login(
            new AuthController.LoginRequest(null, null)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private UserAccount createDemoUser(String username, String email, String role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashed_password_placeholder");
        user.setRoles(List.of(role));
        user.setEnabled(true);
        return user;
    }
}
