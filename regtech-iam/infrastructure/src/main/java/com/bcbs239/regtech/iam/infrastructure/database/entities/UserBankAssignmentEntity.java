package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.User.BankAssignment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for user_bank_assignments table.
 */
@Setter
@Getter
@Entity
@Table(name = "user_bank_assignments", schema = "iam", indexes = {
    @Index(name = "idx_user_bank_assignments_user_id", columnList = "user_id"),
    @Index(name = "idx_user_bank_assignments_bank_id", columnList = "bank_id")
})
public class UserBankAssignmentEntity {

    // Getters and setters
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

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
    public UserBankAssignmentEntity(UUID userId, String bankId, String role, Instant assignedAt) {
        this.userId = userId;
        this.bankId = bankId;
        this.role = role;
        this.assignedAt = assignedAt;
        this.version = 0L;
    }

    /**
     * Factory method to create entity from domain object
     */
    public static UserBankAssignmentEntity fromDomain(BankAssignment assignment, UUID userId) {
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

}

