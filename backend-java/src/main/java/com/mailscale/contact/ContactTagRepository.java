package com.mailscale.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactTagRepository extends JpaRepository<ContactTag, Long> {

    List<ContactTag> findByOrganizationId(Long orgId);

    Optional<ContactTag> findByOrganizationIdAndName(Long orgId, String name);

    Optional<ContactTag> findByIdAndOrganizationId(Long id, Long orgId);
}
