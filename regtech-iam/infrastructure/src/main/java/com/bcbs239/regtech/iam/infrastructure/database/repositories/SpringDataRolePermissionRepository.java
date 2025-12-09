package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Spring Data JPA repository for RolePermission entities.
 */
@Repository
public interface SpringDataRolePermissionRepository extends JpaRepository<RolePermissionEntity, String> {

    /**
     * Find all permissions for a role by role ID.
     */
    @Query("SELECT rp.permission FROM RolePermissionEntity rp WHERE rp.role.id = :roleId")
    Set<String> findPermissionsByRoleId(@Param("roleId") String roleId);

    /**
     * Find all permissions for a role by role name.
     */
    @Query("SELECT rp.permission FROM RolePermissionEntity rp WHERE rp.role.name = :roleName")
    Set<String> findPermissionsByRoleName(@Param("roleName") String roleName);

    /**
     * Find all role permission entities for a role.
     */
    Set<RolePermissionEntity> findByRoleId(String roleId);

    /**
     * Delete all permissions for a role.
     */
    void deleteByRoleId(String roleId);

    /**
     * Check if a role has a specific permission.
     */
    boolean existsByRoleIdAndPermission(String roleId, String permission);
}