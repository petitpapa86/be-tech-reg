package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.domain.users.Bcbs239Role;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataUserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findByUserIdAndActiveTrue(String userId);
    List<UserRoleEntity> findByUserIdAndOrganizationIdAndActiveTrue(String userId, String organizationId);
    boolean existsByUserIdAndRoleAndActiveTrue(String userId, Bcbs239Role role);
}