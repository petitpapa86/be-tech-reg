package com.bcbs239.regtech.iam.application.users;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Command Handler: Invite new user
 * 
 * Creates user with PENDING_PAYMENT status + invitation token
 * Uses EXISTING User.create() factory method with proper value object validation
 */
@Service
@RequiredArgsConstructor
public class InviteUserHandler {
    
    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(InviteUserHandler.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @Value
    public static class Command {
        String bankId;
        String email;
        String firstName;
        String lastName;
        String invitedBy;
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        log.info("InviteUserHandler.handle - email={}, bankId={}, invitedBy={}", command.email, command.bankId, command.invitedBy);
        List<FieldError> validationErrors = new ArrayList<>();
        
        // 1. Validate and create Email value object
        Result<Email> emailResult = Email.create(command.email);
        Email email = null;
        if (emailResult.isFailure()) {
            validationErrors.add(new FieldError("email", emailResult.getError().get().getMessage(), emailResult.getError().get().getMessageKey()));
        } else {
            email = emailResult.getValue().get();
        }

        // 2. Validate and create BankId value object with numeric validation
        Result<BankId> bankIdResult = BankId.createNumeric(command.bankId);
        BankId bankId = null;
        if (bankIdResult.isFailure()) {
            validationErrors.add(new FieldError("bankId", bankIdResult.getError().get().getMessage(), bankIdResult.getError().get().getMessageKey()));
        } else {
            bankId = bankIdResult.getValue().get();
        }

        // 3. Validate firstName
        if (command.firstName == null || command.firstName.trim().isEmpty()) {
            validationErrors.add(new FieldError("firstName", "First name is required", "validation.first_name_required"));
        }

        // 4. Validate lastName
        if (command.lastName == null || command.lastName.trim().isEmpty()) {
            validationErrors.add(new FieldError("lastName", "Last name is required", "validation.last_name_required"));
        }

        // 5. Validate invitedBy
        if (command.invitedBy == null || command.invitedBy.trim().isEmpty()) {
            validationErrors.add(new FieldError("invitedBy", "Invited by field is required", "validation.invited_by_required"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            log.warn("InviteUserHandler validation failed - errors={}", validationErrors);
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }
        
        // 6. Check if user already exists in this bank (business rule validation)
        if (bankId != null && userRepository.existsByEmailAndBankId(email, bankId.getAsLong())) {
            log.warn("InviteUserHandler - user already exists - email={}, bankId={}", command.email, bankId.getValue());
            return Result.failure(
                ErrorDetail.of("USER_EXISTS", ErrorType.BUSINESS_RULE_ERROR,
                    "User with email " + command.email + " already exists in this bank",
                    "usermanagement.user.exists")
            );
        }
        
        // 7. Generate secure invitation token
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String invitationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // 8. Create temporary password hash (will be replaced when user accepts invitation)
        // For pending invitations, we use a placeholder hash since the real password will be set later
        Password tempPassword = Password.fromHash("PENDING_INVITATION_" + invitationToken.substring(0, 16));

        // 9. Create user using EXISTING factory method
        User user = User.create(email, tempPassword, command.firstName.trim(), command.lastName.trim());

        // Set invitation token on user
        user.setInvitationToken(invitationToken);

        // Set status to INVITED (not PENDING_PAYMENT)
        user.setStatus(UserStatus.INVITED);

        // 10. Assign to bank (adds BankAssignment)
        if (bankId != null) {
            user.assignToBank(bankId.getValue(), "USER");
        }

        // Store invitation metadata (now added to User entity)

        // 11. Save user
        Result<UserId> saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            log.error("InviteUserHandler - failed to save user - email={}, error={}", command.email, saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }

        var newUserId = saveResult.getValue().get();
        log.info("User invited - userId={}, email={}, bankId={}", newUserId.getValue(), user.getEmail().getValue(), bankId != null ? bankId.getValue() : null);

        // TODO: Send invitation email with token

        return Result.success(user);
    }
}
