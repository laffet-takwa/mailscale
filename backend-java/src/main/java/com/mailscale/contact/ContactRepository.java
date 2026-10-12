package com.mailscale.contact;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("select c from Contact c where c.organization.id = :orgId")
    Page<Contact> findAllByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("select c from Contact c where c.organization.id = :orgId"
            + " and (lower(c.email) like lower(concat('%', :query, '%'))"
            + " or lower(c.firstName) like lower(concat('%', :query, '%'))"
            + " or lower(c.lastName) like lower(concat('%', :query, '%')))")
    Page<Contact> search(@Param("orgId") Long orgId, @Param("query") String query, Pageable pageable);

    @Query("select c from Contact c where c.organization.id = :orgId and c.status = :status")
    Page<Contact> findByStatus(@Param("orgId") Long orgId, @Param("status") ContactStatus status,
                                 Pageable pageable);

    Optional<Contact> findByOrganizationIdAndEmail(Long orgId, String email);

    Optional<Contact> findByIdAndOrganizationId(Long id, Long orgId);

    @Query("select count(c) from Contact c where c.organization.id = :orgId and c.status = 'ACTIVE'")
    long countActiveByOrganizationId(@Param("orgId") Long orgId);

    @Query("select c from Contact c where c.organization.id = :orgId and c.status = 'ACTIVE'"
            + " and c.consentStatus = 'GRANTED' and c.unsubscribedAt is null")
    Page<Contact> findDeliverable(@Param("orgId") Long orgId, Pageable pageable);
}
