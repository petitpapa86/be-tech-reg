package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.billing.domain.events.PaymentVerifiedEvent;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentVerificationEventHandlerTest {

    @Mock
    private JpaUserRepository userRepository;

    @InjectMocks
    private PaymentVerificationEventHandler eventHandler;

    @Test
    void shouldActivateUserWhenPaymentVerified() {
        // Given
        UserId userId = UserId.generate();
        BillingAccountId billingAccountId = BillingAccountId.generate();
        String correlationId = "test-correlation-id";
        
        PaymentVerifiedEvent event = new PaymentVerifiedEvent(userId, billingAccountId, correlationId);
        
        // Create a user with PENDING_PAYMENT status
        User user = createTestUser(userId, UserStatus.PENDING_PAYMENT);
        
        Function<UserId, Result<User>> userLoader = id -> Result.success(user);
        Function<User, Result<UserId>> userSaver = u -> Result.success(u.getId());
        
        when(userRepository.userLoader()).thenReturn(userLoader);
        when(userRepository.userSaver()).thenReturn(userSaver);

        // When
        eventHandler.handlePaymentVerified(event);

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).userLoader();
        verify(userRepository).userSaver();
    }

    @Test
    void shouldHandleUserNotFoundGracefully() {
        // Given
        UserId userId = UserId.generate();
        BillingAccountId billingAccountId = BillingAccountId.generate();
        String correlationId = "test-correlation-id";
        
        PaymentVerifiedEvent event = new PaymentVerifiedEvent(userId, billingAccountId, correlationId);
        
        Function<UserId, Result<User>> userLoader = id -> Result.failure(
            ErrorDetail.of("USER_NOT_FOUND", "User not found", "error.user.notFound"));
        Function<User, Result<UserId>> userSaver = u -> Result.success(u.getId());
        
        when(userRepository.userLoader()).thenReturn(userLoader);
        when(userRepository.userSaver()).thenReturn(userSaver);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> eventHandler.handlePaymentVerified(event));
        
        verify(userRepository).userLoader();
        verify(userRepository, never()).userSaver();
    }

    @Test
    void testActivateUserWithClosuresPureFunction() {
        // Given
        UserId userId = UserId.generate();
        String correlationId = "test-correlation-id";
        
        User user = createTestUser(userId, UserStatus.PENDING_PAYMENT);
        
        Function<UserId, Result<User>> userLoader = id -> Result.success(user);
        Function<User, Result<UserId>> userSaver = u -> Result.success(u.getId());

        // When
        Result<Void> result = PaymentVerificationEventHandler.activateUserWithClosures(
            userId, userLoader, userSaver, correlationId);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void testActivateUserWithClosuresHandlesLoadFailure() {
        // Given
        UserId userId = UserId.generate();
        String correlationId = "test-correlation-id";
        
        Function<UserId, Result<User>> userLoader = id -> Result.failure(
            ErrorDetail.of("USER_NOT_FOUND", "User not found", "error.user.notFound"));
        Function<User, Result<UserId>> userSaver = u -> Result.success(u.getId());

        // When
        Result<Void> result = PaymentVerificationEventHandler.activateUserWithClosures(
            userId, userLoader, userSaver, correlationId);

        // Then
        assertTrue(result.isFailure());
        assertEquals("USER_NOT_FOUND", result.getError().get().getCode());
    }

    @Test
    void testActivateUserWithClosuresHandlesSaveFailure() {
        // Given
        UserId userId = UserId.generate();
        String correlationId = "test-correlation-id";
        
        User user = createTestUser(userId, UserStatus.PENDING_PAYMENT);
        
        Function<UserId, Result<User>> userLoader = id -> Result.success(user);
        Function<User, Result<UserId>> userSaver = u -> Result.failure(
            ErrorDetail.of("USER_SAVE_FAILED", "Failed to save user", "error.user.saveFailed"));

        // When
        Result<Void> result = PaymentVerificationEventHandler.activateUserWithClosures(
            userId, userLoader, userSaver, correlationId);

        // Then
        assertTrue(result.isFailure());
        assertEquals("USER_SAVE_FAILED", result.getError().get().getCode());
        assertEquals(UserStatus.ACTIVE, user.getStatus()); // User was activated but save failed
    }

    private User createTestUser(UserId userId, UserStatus status) {
        try {
            Email email = Email.create("test@example.com").getValue().get();
            Password password = Password.create("TestPassword123!").getValue().get();
            User user = User.create(email, password, "Test", "User");
            
            // Use reflection to set the status since we need to test different statuses
            java.lang.reflect.Field statusField = User.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(user, status);
            
            // Set the ID using reflection
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId.getValue());
            
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test user", e);
        }
    }
}