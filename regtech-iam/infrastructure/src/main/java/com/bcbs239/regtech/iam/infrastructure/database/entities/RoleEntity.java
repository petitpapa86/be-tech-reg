package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for roles table.
 * Represents a role with its metadata and associated permissions.
 */
@Entity
@Table(name = "roles", schema = "iam", indexes = {
    @Index(name = "idx_roles_name", columnList = "name", unique = true),
    @Index(name = "idx_roles_level", columnList = "level")
})
public class RoleEntity {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RolePermissionEntity> permissions = new HashSet<>();

    // Default constructor for JPA
    protected RoleEntity() {}

    public RoleEntity(String id, String name, String displayName, String description, Integer level) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // Don't set version for new entities - let Hibernate handle it
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Integer getLevel() { return level; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public Set<RolePermissionEntity> getPermissions() { return permissions; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setDescription(String description) { this.description = description; }
    public void setLevel(Integer level) { this.level = level; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }
    public void setPermissions(Set<RolePermissionEntity> permissions) { this.permissions = permissions; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleEntity that = (RoleEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RoleEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", level=" + level +
                '}';
    }
}