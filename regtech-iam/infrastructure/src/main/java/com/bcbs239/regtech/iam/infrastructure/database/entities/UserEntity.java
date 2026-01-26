package com.bcbs239.regtech.iam.infrastructure.database.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA Entity for users table with proper domain conversion.
 * Follows the established pattern of separating persistence from domain model.
 */
@Setter
@Getter
@Entity
@Table(name = "users", schema = "iam", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status", columnList = "status"),
    @Index(name = "idx_users_google_id", columnList = "google_id"),
    @Index(name = "idx_users_facebook_id", columnList = "facebook_id")
})
public class UserEntity {

    // Getters and setters
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "bank_id", nullable = false, length = 50)
    private String bankId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status;


    @Column(name = "google_id", length = 255)
    private String googleId;

    @Column(name = "invitation_token", length = 128)
    private String invitationToken;

    @Column(name = "facebook_id", length = 255)
    private String facebookId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Bidirectional OneToMany with cascade for proper relationship management
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private List<UserBankAssignmentEntity> bankAssignments = new ArrayList<>();


    // Default constructor for JPA
    public UserEntity() {}

    /**
     * Factory method to create entity from domain object
     */
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId().getUUID();
        entity.email = user.getEmail().getValue();
        entity.passwordHash = user.getPassword().getHashedValue();
        // Populate username from domain
        entity.username = user.getUsername();
        entity.firstName = user.getFirstName();
        entity.lastName = user.getLastName();
        entity.status = user.getStatus();
        entity.googleId = user.getGoogleId();
        entity.facebookId = user.getFacebookId();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        // Set version to null for new entities so Hibernate treats them as transient
        entity.version = null;

        // Convert bank assignments
        entity.bankAssignments = user.getBankAssignments().stream()
            .map(assignment -> UserBankAssignmentEntity.fromDomain(assignment, entity.id))
            .toList();

        // Set bankId for single-bank context (first assignment)
        if (!user.getBankAssignments().isEmpty()) {
            entity.bankId = user.getBankAssignments().get(0).getBankId();
        } else {
            entity.bankId = null;
        }

        // Set invitationToken if present
        entity.invitationToken = user.getInvitationToken();

        return entity;
    }

    /**
     * Convert to domain object
     */
    public User toDomain() {
        // Create email and password value objects
        Email emailVO = Email.create(email).getValue().get();
        Password passwordVO = Password.fromHash(passwordHash);

        return User.createFromPersistence(
            new UserId(id),
            emailVO,
            passwordVO,
            username,
            firstName,
            lastName,
            status,
            googleId,
            facebookId,
            createdAt,
            updatedAt,
            version,
            bankAssignments.stream()
                .map(UserBankAssignmentEntity::toDomain)
                .toList(),
            invitationToken
        );
    }


}
