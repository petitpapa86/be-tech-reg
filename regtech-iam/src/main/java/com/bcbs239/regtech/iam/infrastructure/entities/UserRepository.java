package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.application.authenticate.OAuth2UserInfo;
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
     * Function to load user by ID
     * Returns Result<User> for functional error handling
     */
    public Function<UserId, Result<User>> userLoader() {
        return userId -> {
            try {
                User user = entityManager.find(User.class, userId.getValue());
                if (user == null) {
                    return Result.failure(ErrorDetail.of("USER_NOT_FOUND",
                        "User not found with ID: " + userId, "error.user.notFound"));
                }
                return Result.success(user);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_LOAD_FAILED",
                    "Failed to load user: " + e.getMessage(), "error.user.loadFailed"));
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
     * Function to save OAuth user (find or create)
     * Returns Result<User> for functional error handling
     */
    public Function<OAuth2UserInfo, Result<User>> saveOAuthUser() {
        return userInfo -> {
            try {
                // Check if user already exists
                Maybe<User> existingUser = emailLookup().apply(userInfo.email());
                if (existingUser.isPresent()) {
                    User user = existingUser.getValue();
                    // Update OAuth ID if not set
                    if (userInfo.externalId() != null) {
                        // This is a simplified approach - in reality you'd need to know the provider
                        // For now, we'll assume it's Google
                        if (user.getGoogleId() == null) {
                            user.setGoogleId(userInfo.externalId());
                            entityManager.merge(user);
                        }
                    }
                    return Result.success(user);
                }

                // Create new user from OAuth info
                User newUser = User.createOAuth(
                    userInfo.email(),
                    userInfo.firstName(),
                    userInfo.lastName(),
                    userInfo.externalId()
                );

                entityManager.persist(newUser);
                return Result.success(newUser);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("OAUTH_USER_SAVE_FAILED",
                    "Failed to save OAuth user: " + e.getMessage(),
                    "error.oauth.user.saveFailed"));
            }
        };
    }

    /**
     * Function to generate JWT token for user
     * Returns Result<JwtToken> for functional error handling
     */
    public Function<User, Result<JwtToken>> tokenGenerator(String secretKey) {
        return user -> JwtToken.generate(user, secretKey, java.time.Duration.ofHours(24));
    }
}