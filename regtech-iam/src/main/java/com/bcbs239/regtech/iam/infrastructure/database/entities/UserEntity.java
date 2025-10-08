package com.bcbs239.regtech.iam.infrastructure.database.entities;

import com.bcbs239.regtech.iam.domain.users.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for users table with proper domain conversion.
 * Follows the established pattern of separating persistence from domain model.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status", columnList = "status"),
    @Index(name = "idx_users_google_id", columnList = "google_id"),
    @Index(name = "idx_users_facebook_id", columnList = "facebook_id")
})
public class UserEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private String id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

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

    @Column(name = "facebook_id", length = 255)
    private String facebookId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserBankAssignmentEntity> bankAssignments = new ArrayList<>();

    // Default constructor for JPA
    protected UserEntity() {}

    // Constructor for creation
    public UserEntity(String id, String email, String passwordHash, String firstName,
                     String lastName, UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = 0L;
    }

    /**
     * Factory method to create entity from domain object
     */
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId().getValue();
        entity.email = user.getEmail().getValue();
        entity.passwordHash = user.getPassword().getHashedValue();
        entity.firstName = user.getFirstName();
        entity.lastName = user.getLastName();
        entity.status = user.getStatus();
        entity.googleId = user.getGoogleId();
        entity.facebookId = user.getFacebookId();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        entity.version = user.getVersion();
        
        // Convert bank assignments
        entity.bankAssignments = user.getBankAssignments().stream()
            .map(assignment -> UserBankAssignmentEntity.fromDomain(assignment, entity.id))
            .toList();
        
        return entity;
    }

    /**
     * Convert to domain object
     */
    public User toDomain() {
        // Create email and password value objects
        Email emailVO = Email.create(email).getValue().get();
        Password passwordVO = Password.fromHash(passwordHash);
        
        // Use reflection or package-private constructor to create domain object
        // This is a simplified approach - in practice you might need a more sophisticated builder
        User user = User.createFromPersistence(
            UserId.fromString(id),
            emailVO,
            passwordVO,
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
                .toList()
        );
        
        return user;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getFacebookId() { return facebookId; }
    public void setFacebookId(String facebookId) { this.facebookId = facebookId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<UserBankAssignmentEntity> getBankAssignments() { return bankAssignments; }
    public void setBankAssignments(List<UserBankAssignmentEntity> bankAssignments) {
        this.bankAssignments = bankAssignments;
    }
}