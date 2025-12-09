package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.UserRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for user_roles table with proper domain conversion.
 * Updated to use role_id instead of enum for database-driven role management.
 */
@Entity
@Table(name = "user_roles", schema = "iam", indexes = {
    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_roles_organization_id", columnList = "organization_id"),
    @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
    @Index(name = "idx_user_roles_active", columnList = "active")
})
public class UserRoleEntity {

    @Id
    @Column(name = "id", length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", length = 36)
    private String roleId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

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
    public UserRoleEntity(String id, UUID userId, String roleId, String organizationId,
                         Boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.organizationId = organizationId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = 0L;
    }

    /**
     * Factory method to create entity from domain object
     * Note: roleId must be set separately as entities cannot have repository dependencies
     */
    public static UserRoleEntity fromDomain(UserRole userRole) {
        UserRoleEntity entity = new UserRoleEntity();
        entity.id = userRole.getId();
        entity.userId = UUID.fromString(userRole.getUserId());
        // roleId will be set by the service layer
        entity.roleName = userRole.getRoleName();
        entity.organizationId = userRole.getOrganizationId();
        entity.active = userRole.isActive();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoleEntity that = (UserRoleEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserRoleEntity{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", roleId='" + roleId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", active=" + active +
                '}';
    }
}
