package com.mailscale.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByOrganizationId(Long organizationId);

    Optional<ApiKey> findByIdAndOrganizationId(Long id, Long organizationId);

    void deleteByIdAndOrganizationId(Long id, Long organizationId);

    default void markUsed(ApiKey key, Instant now) {
        key.markUsed(now);
        save(key);
    }
}
