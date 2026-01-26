package com.bcbs239.regtech.iam.infrastructure.database.entities;

/**
 * User status enum for JPA entity
 */
public enum UserStatusEntity {
    PENDING_PAYMENT,
    INVITED,
    ACTIVE,
    SUSPENDED,
    CANCELLED
}

