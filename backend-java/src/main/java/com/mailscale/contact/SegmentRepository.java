package com.mailscale.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    List<Segment> findByOrganizationId(Long orgId);

    Optional<Segment> findByOrganizationIdAndName(Long orgId, String name);

    Optional<Segment> findByIdAndOrganizationId(Long id, Long orgId);
}
