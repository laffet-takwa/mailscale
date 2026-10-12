package com.mailscale.email;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOrganizationIdAndIdemKey(Long orgId, String idemKey);

    @Modifying
    @Query("delete from IdempotencyRecord i where i.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
