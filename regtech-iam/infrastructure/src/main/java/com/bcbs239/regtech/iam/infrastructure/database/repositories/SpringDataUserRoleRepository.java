package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataUserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findByUserIdAndActiveTrue(UUID userId);
    List<UserRoleEntity> findByUserIdAndOrganizationIdAndActiveTrue(UUID userId, String organizationId);
    boolean existsByUserIdAndRoleIdAndActiveTrue(UUID userId, String roleId);
}