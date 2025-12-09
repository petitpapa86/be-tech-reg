package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for Role entities.
 */
@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, String> {

    /**
     * Find role by name.
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * Find all roles ordered by level.
     */
    @Query("SELECT r FROM RoleEntity r ORDER BY r.level ASC")
    Set<RoleEntity> findAllOrderedByLevel();

    /**
     * Find roles by level range.
     */
    @Query("SELECT r FROM RoleEntity r WHERE r.level >= :minLevel AND r.level <= :maxLevel ORDER BY r.level ASC")
    Set<RoleEntity> findByLevelRange(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel);

    /**
     * Check if role exists by name.
     */
    boolean existsByName(String name);
}