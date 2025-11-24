package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a bank in the database.
 */
@Setter
@Getter
@Entity
@Table(name = "banks", schema = "iam")
public class BankEntity {

    // Getters and setters
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;
    
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    // Default constructor for JPA
    protected BankEntity() {
    }
    
    public BankEntity(String id, String name, String countryCode, String status) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankEntity that = (BankEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString() {
        return "BankEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
