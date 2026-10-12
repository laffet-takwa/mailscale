package com.mailscale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailscale.auth.AuthService;
import com.mailscale.auth.OrganizationRepository;
import com.mailscale.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void registrationCreatesUserOrganizationAndMembership() {
        var response = authService.register(
                new AuthService.RegisterRequest(
                        "newuser@example.com", "password123",
                        "New User", "New Org", "new-org"));

        assertNotNull(response.token());
        assertEquals("new-org", response.organizationSlug());
        assertEquals("OWNER", response.role());

        assertTrue(userRepository
                .existsByEmail("newuser@example.com"));
        assertTrue(organizationRepository
                .findBySlug("new-org").isPresent());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        authService.register(
                new AuthService.RegisterRequest(
                        "dup@example.com", "password123",
                        "Dup User", "Dup Org", "dup-org"));
        org.junit.jupiter.api.Assertions.assertThrows(
                com.mailscale.common.ConflictException.class,
                () -> authService.register(
                        new AuthService.RegisterRequest(
                                "dup@example.com", "password123",
                                "Dup User", "Dup Org", "dup-org")));
    }

    @Test
    void loginReturnsToken() {
        authService.register(
                new AuthService.RegisterRequest(
                        "login@example.com", "password123",
                        "Login User", "Login Org", "login-org"));
        var response = authService.login(
                new AuthService.LoginRequest(
                        "login@example.com", "password123"));
        assertNotNull(response.token());
    }

    @Test
    void loginWithWrongPasswordFails() {
        authService.register(
                new AuthService.RegisterRequest(
                        "wrong@example.com", "password123",
                        "Wrong User", "Wrong Org", "wrong-org"));
        org.junit.jupiter.api.Assertions.assertThrows(
                com.mailscale.common.UnauthorizedException.class,
                () -> authService.login(
                        new AuthService.LoginRequest(
                                "wrong@example.com", "wrongpass")));
    }
}
