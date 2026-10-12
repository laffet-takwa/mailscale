package com.mailscale.email;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    Optional<EmailMessage> findById(UUID id);

    @Query("select m from EmailMessage m where m.organization.id = :orgId order by m.createdAt desc")
    Page<EmailMessage> findByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("select m from EmailMessage m where m.organization.id = :orgId"
            + " and m.status = :status order by m.createdAt desc")
    Page<EmailMessage> findByOrganizationIdAndStatus(@Param("orgId") Long orgId,
                                                       @Param("status") MessageStatus status,
                                                       Pageable pageable);

    @Query("select m from EmailMessage m where m.organization.id = :orgId"
            + " and m.status = :status"
            + " and (:type is null or m.messageType = :type)"
            + " order by m.createdAt desc")
    Page<EmailMessage> findByOrganizationIdAndStatus(
            @Param("orgId") Long orgId, @Param("status") MessageStatus status,
            @Param("type") MessageType type, Pageable pageable);

    @Query("select m from EmailMessage m where m.organization.id = :orgId"
            + " and m.messageType = :type order by m.createdAt desc")
    Page<EmailMessage> findByOrganizationIdAndType(@Param("orgId") Long orgId,
                                                     @Param("type") MessageType type,
                                                     Pageable pageable);

    @Query("select m from EmailMessage m where m.correlationId = :correlationId")
    Optional<EmailMessage> findByCorrelationId(@Param("correlationId") String correlationId);

    @Query("select m from EmailMessage m where m.organization.id = :orgId"
            + " and m.idempotencyKey = :key")
    Optional<EmailMessage> findByOrganizationAndIdempotencyKey(
            @Param("orgId") Long orgId, @Param("key") String key);

    @Query("select count(m) from EmailMessage m where m.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") Long orgId);

    @Query("select count(m) from EmailMessage m where m.organization.id = :orgId"
            + " and m.createdAt >= :since")
    long countByOrganizationIdAndCreatedAtAfter(@Param("orgId") Long orgId,
                                                  @Param("since") Instant since);

    @Query("select count(m) from EmailMessage m where m.organization.id = :orgId"
            + " and m.createdAt between :start and :end")
    long countByOrganizationIdAndCreatedAtBetween(
            @Param("orgId") Long orgId, @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("select count(m) from EmailMessage m where m.organization.id = :orgId"
            + " and m.status in :statuses and m.createdAt >= :since")
    long countByOrganizationIdAndStatusInAndCreatedAtAfter(
            @Param("orgId") Long orgId, @Param("statuses") List<MessageStatus> statuses,
            @Param("since") Instant since);

    List<EmailMessage> findByStatusInAndCreatedAtBefore(List<MessageStatus> statuses, Instant before);
}
