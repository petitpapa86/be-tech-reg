package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class User extends Entity {
    private String id;
    private Email email;
    private Password password;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private String googleId;
    private String facebookId;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    private List<BankAssignment> bankAssignments;
    private String username;

    private User() {
        this.bankAssignments = new ArrayList<>();
    }

    /**
     * Factory method to create a new user
     */
    public static User create(Email email, Password password, String firstName, String lastName) {
        User user = new User();
        user.id = UserId.generate().getValue();
        user.email = email;
        user.password = password;
        // Default username derived from email
        user.username = email.getValue();
        user.firstName = firstName;
        user.lastName = lastName;
        user.status = UserStatus.PENDING_PAYMENT; // New users need payment verification
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        user.version = 0;
        return user;
    }

    public static User createWithBank(Email email, Password password, String firstName, String lastName, String bankId, String paymentMethodId, String causationId) {
        User user = create(email, password, firstName, lastName);
        // username already set in create()
        user.assignToBank(bankId, "USER"); // Default role for new users
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.id,
                email.getValue(),
                bankId,
                paymentMethodId,
                causationId);
        user.addDomainEvent(event);
        return user;
    }

    /**
     * Factory method to create a new user from OAuth authentication
     */
    public static User createOAuth(Email email, String firstName, String lastName, String externalId) {
        User user = new User();
        user.id = UserId.generate().getValue();
        user.email = email;
        user.password = Password.create("TEMP_PASSWORD").getValue().get(); // Temporary password
        // Derive username from email for OAuth-created users
        user.username = email.getValue();
        user.firstName = firstName;
        user.lastName = lastName;
        user.status = UserStatus.ACTIVE; // OAuth users are active by default
        user.googleId = externalId; // Assume Google for now
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        user.version = 0;
        return user;
    }

    /**
     * Package-private factory method for persistence layer reconstruction
     * Used by JPA entities to recreate domain objects from database
     */
    public static User createFromPersistence(UserId id, Email email, Password password, String username, String firstName,
                                             String lastName, UserStatus status, String googleId, String facebookId,
                                             Instant createdAt, Instant updatedAt, long version,
                                             List<BankAssignment> bankAssignments) {
        User user = new User();
        user.id = id.getValue();
        user.email = email;
        user.password = password;
        user.username = username;
        user.firstName = firstName;
        user.lastName = lastName;
        user.status = status;
        user.googleId = googleId;
        user.facebookId = facebookId;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        user.version = version;
        user.bankAssignments = new ArrayList<>(bankAssignments);
        return user;
    }

    // Getter for username
    public String getUsername() {
        return username;
    }

    /**
     * Records a successful authentication
     */
    public void recordAuthentication() {
        this.updatedAt = Instant.now();
    }

    /**
     * Changes the user's password
     */
    public void changePassword(Password newPassword) {
        this.password = newPassword;
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Associates user with a selected bank
     */
    public void assignToBank(String bankId, String role) {
        BankAssignment assignment = new BankAssignment(
                bankId,
                role,
                Instant.now()
        );
        this.bankAssignments.add(assignment);
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Marks email as verified
     */
    public void verifyEmail() {
        // Email verification logic if needed
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Updates user profile information
     */
    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Activates user (after payment)
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Suspends user account
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Cancels user account
     */
    public void cancel() {
        this.status = UserStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.version++;
    }

    /**
     * Sets user to pending payment status
     */
    public void setPendingPayment() {
        this.status = UserStatus.PENDING_PAYMENT;
        this.updatedAt = Instant.now();
        this.version++;
    }

    // Getters
    public UserId getId() {
        return UserId.fromString(id);
    }


    public List<BankAssignment> getBankAssignments() {
        return List.copyOf(bankAssignments);
    }

    // Setters for OAuth
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
        this.updatedAt = Instant.now();
        this.version++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email=" + email +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", version=" + version +
                '}';
    }

    /**
     * Bank Assignment embedded entity
     */
    @Getter
    public static class BankAssignment {
        private String id;
        private final String bankId;
        private final String role;
        private final Instant assignedAt;

        public BankAssignment(String bankId, String role, Instant assignedAt) {
            this.bankId = bankId;
            this.role = role;
            this.assignedAt = assignedAt;
        }

    }
}
