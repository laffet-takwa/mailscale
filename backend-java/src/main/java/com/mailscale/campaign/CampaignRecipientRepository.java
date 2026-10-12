package com.mailscale.campaign;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {

    List<CampaignRecipient> findByCampaignId(Long campaignId);

    Optional<CampaignRecipient> findByCampaignIdAndContactId(Long campaignId, Long contactId);

    @Query("select cr from CampaignRecipient cr where cr.campaign.id = :campaignId and cr.status = :status")
    List<CampaignRecipient> findByCampaignIdAndStatus(@Param("campaignId") Long campaignId,
                                                        @Param("status") RecipientStatus status);

    @Query("select count(cr) from CampaignRecipient cr where cr.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") Long campaignId);

    @Query("select new map(cr.status as status, count(cr) as count) from CampaignRecipient cr"
            + " where cr.campaign.id = :campaignId group by cr.status")
    List<?> countByStatusGrouped(@Param("campaignId") Long campaignId);
}
