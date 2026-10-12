package com.mailscale.auth;

import com.mailscale.auth.ApiKey;
import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "msk_live_";
    private static final int KEY_BYTES = 24;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public CreatedApiKey create(Organization organization, String name,
                                  List<String> scopes, Instant expiresAt) {
        byte[] random = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(random);
        String secret = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(random);
        String hash = sha256(secret);
        String prefix = secret.substring(0, Math.min(12, secret.length()));
        ApiKey apiKey = new ApiKey(organization, name, hash, prefix,
                String.join(",", scopes), expiresAt);
        apiKeyRepository.save(apiKey);
        return new CreatedApiKey(apiKey.getId(), name, secret, scopes, expiresAt);
    }

    @Transactional(readOnly = true)
    public ApiKey verify(String secret) {
        String hash = sha256(secret);
        ApiKey key = apiKeyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new com.mailscale.common.UnauthorizedException(
                        "Invalid API key"));
        if (!key.isActive(Instant.now())) {
            throw new com.mailscale.common.UnauthorizedException("API key is revoked or expired");
        }
        return key;
    }

    @Transactional
    public void revoke(Organization organization, Long id) {
        ApiKey key = apiKeyRepository.findByIdAndOrganizationId(id, organization.getId())
                .orElseThrow(() -> new NotFoundException("API key not found"));
        key.revoke(Instant.now());
        apiKeyRepository.save(key);
    }

    public List<ApiKey> list(Organization organization) {
        return apiKeyRepository.findByOrganizationId(organization.getId());
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record CreatedApiKey(Long id, String name, String secret,
                                   List<String> scopes, Instant expiresAt) {
    }
}
