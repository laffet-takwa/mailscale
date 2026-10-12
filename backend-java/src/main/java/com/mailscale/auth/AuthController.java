package com.mailscale.auth;

import com.mailscale.auth.ApiKeyService.CreatedApiKey;
import com.mailscale.common.OrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final ApiKeyService apiKeyService;

    public AuthController(AuthService authService, ApiKeyService apiKeyService) {
        this.authService = authService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user and organization")
    public ResponseEntity<AuthService.RegisterResponse> register(
            @Valid @RequestBody AuthService.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT")
    public ResponseEntity<AuthService.LoginResponse> login(
            @Valid @RequestBody AuthService.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user and organization")
    public ResponseEntity<Map<String, Object>> me() {
        var ctx = OrganizationContext.requireUser();
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(Map.of(
                "user", Map.of("id", ctx.getId(), "email", ctx.getEmail(),
                        "name", ctx.getFullName()),
                "organization", Map.of("id", org.getId(), "slug", org.getSlug(),
                        "name", org.getName())
        ));
    }

    @PostMapping("/api-keys")
    @Operation(summary = "Create an API key for the current organization")
    public ResponseEntity<CreatedApiKey> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        var org = OrganizationContext.requireOrganization();
        Instant expiresAt = request.expiresInDays() != null
                ? Instant.now().plus(java.time.Duration.ofDays(request.expiresInDays()))
                : null;
        return ResponseEntity.ok(apiKeyService.create(org, request.name(),
                request.scopes() != null ? request.scopes() : List.of("email:send"),
                expiresAt));
    }

    @GetMapping("/api-keys")
    @Operation(summary = "List API keys for the current organization")
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        var org = OrganizationContext.requireOrganization();
        return ResponseEntity.ok(apiKeyService.list(org));
    }

    public record CreateApiKeyRequest(
            @NotBlank String name,
            List<String> scopes,
            Integer expiresInDays) {
    }
}
