package com.mailscale.campaign;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    Optional<Campaign> findByIdAndOrganizationId(Long id, Long orgId);

    @Query("select c from Campaign c where c.organization.id = :orgId and c.status = 'SCHEDULED'"
            + " and c.scheduledAt <= :now")
    List<Campaign> findDueForDispatch(@Param("orgId") Long orgId, @Param("now") Instant now);

    @Query("select c from Campaign c where c.organization.id = :orgId and c.status = 'PROCESSING'")
    List<Campaign> findProcessing(@Param("orgId") Long orgId);
}
