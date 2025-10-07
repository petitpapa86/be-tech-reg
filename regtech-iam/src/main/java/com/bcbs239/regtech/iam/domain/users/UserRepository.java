package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Repository for User aggregate operations using EntityManager
 * Note: Using concrete class instead of interface as per requirements
 */
@Repository
@Transactional
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Function to find user by email
     * Returns Maybe<User> to handle optional results functionally
     */
    public Function<Email, Maybe<User>> emailLookup() {
        return email -> {
            try {
                User user = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email.getValue())
                    .getSingleResult();
                return Maybe.some(user);
            } catch (NoResultException e) {
                return Maybe.none();
            }
        };
    }

    /**
     * Function to save user
     * Returns Result<UserId> for functional error handling
     */
    public Function<User, Result<UserId>> userSaver() {
        return user -> {
            try {
                if (user.getId() == null) {
                    entityManager.persist(user);
                } else {
                    user = entityManager.merge(user);
                }
                return Result.success(user.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_SAVE_FAILED",
                    "Failed to save user: " + e.getMessage(), "error.user.saveFailed"));
            }
        };
    }

    /**
     * Function to find user by ID
     */
    public Function<UserId, Maybe<User>> userLookup() {
        return userId -> {
            try {
                User user = entityManager.find(User.class, userId.getValue());
                return user != null ? Maybe.some(user) : Maybe.none();
            } catch (Exception e) {
                return Maybe.none();
            }
        };
    }
}