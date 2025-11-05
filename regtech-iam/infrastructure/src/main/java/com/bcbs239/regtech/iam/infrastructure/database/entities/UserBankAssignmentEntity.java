package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.User.BankAssignment;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA Entity for user_bank_assignments table.
 */
@Entity
@Table(name = "user_bank_assignments", schema = "iam", indexes = {
    @Index(name = "idx_user_bank_assignments_user_id", columnList = "user_id"),
    @Index(name = "idx_user_bank_assignments_bank_id", columnList = "bank_id")
})
public class UserBankAssignmentEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private String userId;

    @Column(name = "bank_id", nullable = false, length = 255)
    private String bankId;

    @Column(name = "role", nullable = false, length = 100)
    private String role;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Default constructor for JPA
    protected UserBankAssignmentEntity() {}

    // Constructor for creation
    public UserBankAssignmentEntity(String userId, String bankId, String role, Instant assignedAt) {
        this.userId = userId;
        this.bankId = bankId;
        this.role = role;
        this.assignedAt = assignedAt;
        this.version = 0L;
    }

    /**
     * Factory method to create entity from domain object
     */
    public static UserBankAssignmentEntity fromDomain(BankAssignment assignment, String userId) {
        return new UserBankAssignmentEntity(
            userId,
            assignment.getBankId(),
            assignment.getRole(),
            assignment.getAssignedAt()
        );
    }

    /**
     * Convert to domain object
     */
    public BankAssignment toDomain() {
        return new BankAssignment( bankId, role, assignedAt);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

