package com.mailscale.template;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {

    List<TemplateVersion> findByTemplateIdOrderByVersionDesc(Long templateId);

    Optional<TemplateVersion> findByTemplateIdAndVersion(Long templateId, int version);
}
