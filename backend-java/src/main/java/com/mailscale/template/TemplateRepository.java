package com.mailscale.template;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByOrganizationId(Long orgId);

    Optional<Template> findByOrganizationIdAndName(Long orgId, String name);

    Optional<Template> findByIdAndOrganizationId(Long id, Long orgId);
}
