package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for role_permissions table.
 * Represents the many-to-many relationship between roles and permissions.
 */
@Entity
@Table(name = "role_permissions", schema = "iam", indexes = {
    @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
    @Index(name = "idx_role_permissions_permission", columnList = "permission"),
    @Index(name = "idx_role_permissions_role_permission", columnList = "role_id, permission", unique = true)
})
public class RolePermissionEntity {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(100)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "permission", nullable = false, length = 100)
    private String permission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor for JPA
    protected RolePermissionEntity() {}

    public RolePermissionEntity(String id, RoleEntity role, String permission) {
        this.id = id;
        this.role = role;
        this.permission = permission;
        this.createdAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public RoleEntity getRole() { return role; }
    public String getPermission() { return permission; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setRole(RoleEntity role) { this.role = role; }
    public void setPermission(String permission) { this.permission = permission; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermissionEntity that = (RolePermissionEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RolePermissionEntity{" +
                "id='" + id + '\'' +
                ", roleId='" + (role != null ? role.getId() : null) + '\'' +
                ", permission='" + permission + '\'' +
                '}';
    }
}