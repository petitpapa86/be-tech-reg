package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.iam.domain.users.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing user roles and permissions.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    
    /**
     * Find all active roles for a user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.active = true")
    List<UserRole> findActiveByUserId(@Param("userId") String userId);
    
    /**
     * Find roles for a user in a specific organization
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.organizationId = :organizationId AND ur.active = true")
    List<UserRole> findByUserIdAndOrganizationId(@Param("userId") String userId, @Param("organizationId") String organizationId);
    
    /**
     * Find all users with a specific role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.role = :role AND ur.active = true")
    List<UserRole> findByRole(@Param("role") String role);
    
    /**
     * Check if user has a specific role
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.userId = :userId AND ur.role = :role AND ur.active = true")
    boolean hasRole(@Param("userId") String userId, @Param("role") String role);
}