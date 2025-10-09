package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.core.security.authorization.Role;
import com.bcbs239.regtech.iam.domain.users.UserRole;
import com.bcbs239.regtech.iam.domain.users.UserId;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for user_roles table with proper domain conversion.
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_roles_organization_id", columnList = "organization_id"),
    @Index(name = "idx_user_roles_role", columnList = "role"),
    @Index(name = "idx_user_roles_active", columnList = "active")
})
public class UserRoleEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Column(name = "organization_id", nullable = false, length = 255)
    private String organizationId;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Default constructor for JPA
    protected UserRoleEntity() {}

    // Constructor for creation
    public UserRoleEntity(String id, String userId, Role role, String organizationId, 
                         Boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.organizationId = organizationId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = 0L;
    }

    /**
     * Factory method to create entity from domain object
     */
    public static UserRoleEntity fromDomain(UserRole userRole) {
        UserRoleEntity entity = new UserRoleEntity();
        entity.id = userRole.getId();
        entity.userId = userRole.getUserId();
        entity.role = userRole.getRole();
        entity.organizationId = userRole.getOrganizationId();
        entity.active = userRole.isActive();
        entity.createdAt = Instant.now(); // Set from domain if available
        entity.updatedAt = Instant.now();
        return entity;
    }

    /**
     * Convert to domain object
     */
    public UserRole toDomain() {
        return UserRole.create(
            UserId.fromString(userId),
            role,
            organizationId
        );
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}