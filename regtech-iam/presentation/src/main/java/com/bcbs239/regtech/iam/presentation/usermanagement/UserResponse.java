package com.bcbs239.regtech.iam.presentation.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;

/**
 * Response DTO mapping from EXISTING User domain model
 */
public record UserResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String initials,
    String status,
    String lastAccess,
    boolean hasRecentActivity
) {
    public static UserResponse from(User user) {
        String fullName = user.getFirstName() + " " + user.getLastName();
        String initials = getInitials(user.getFirstName(), user.getLastName());
        
        return new UserResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getFirstName(),
            user.getLastName(),
            fullName,
            initials,
            user.getStatus().name(),
            user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null,
            isRecentActivity(user.getUpdatedAt())
        );
    }
    
    private static String getInitials(String firstName, String lastName) {
        String first = firstName != null && !firstName.isEmpty() ? firstName.substring(0, 1) : "";
        String last = lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
    
    private static boolean isRecentActivity(java.time.Instant updatedAt) {
        if (updatedAt == null) {
            return false;
        }
        return java.time.Duration.between(updatedAt, java.time.Instant.now()).toDays() <= 7;
    }
}
