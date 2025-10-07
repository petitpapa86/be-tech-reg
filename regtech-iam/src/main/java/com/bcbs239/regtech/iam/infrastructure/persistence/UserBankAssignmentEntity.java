package com.bcbs239.regtech.iam.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for user_bank_assignments table
 */
@Entity
@Table(name = "user_bank_assignments", indexes = {
    @Index(name = "idx_bank_assignments_user", columnList = "user_id"),
    @Index(name = "idx_bank_assignments_bank", columnList = "bank_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_bank", columnNames = {"user_id", "bank_id"})
})
public class UserBankAssignmentEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "bank_id", nullable = false, columnDefinition = "UUID")
    private String bankId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    // Constructors
    protected UserBankAssignmentEntity() {}

    public UserBankAssignmentEntity(String id, UserEntity user, String bankId,
                                   String role, Instant assignedAt) {
        this.id = id;
        this.user = user;
        this.bankId = bankId;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}