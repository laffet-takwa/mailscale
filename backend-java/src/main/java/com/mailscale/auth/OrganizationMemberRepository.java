package com.mailscale.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    List<OrganizationMember> findByUserId(Long userId);

    @Query("select om from OrganizationMember om where om.organization.id = :orgId and om.user.id = :userId")
    Optional<OrganizationMember> findRoleInOrganization(
            @Param("orgId") Long orgId, @Param("userId") Long userId);

    boolean existsByOrganizationIdAndUserIdAndRoleIn(
            Long organizationId, Long userId, List<Role> roles);
}
