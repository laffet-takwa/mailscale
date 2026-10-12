package com.mailscale.suppression;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuppressionEntryRepository extends JpaRepository<SuppressionEntry, Long> {

    List<SuppressionEntry> findByOrganizationId(Long orgId);

    Optional<SuppressionEntry> findByOrganizationIdAndEmail(Long orgId, String email);

    Optional<SuppressionEntry> findByIdAndOrganizationId(Long id, Long orgId);

    boolean existsByOrganizationIdAndEmail(Long orgId, String email);
}
