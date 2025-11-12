package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ValidationUtils;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcbs239.regtech.core.infrastructure.context.CorrelationContext;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Service
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;

    public RegisterUserCommandHandler(
            UserRepository userRepository,
            BaseUnitOfWork unitOfWork) {
        this.userRepository = userRepository;
        this.unitOfWork = unitOfWork;
    }

    /**
     * Handles the register user command with transactional outbox pattern
     */
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {

        Result<Email> emailResult = Email.create(command.getEmail());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        Email email = emailResult.getValue().get();

        Maybe<User> existingUser = userRepository.emailLookup(email);
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS", ErrorType.VALIDATION_ERROR, "Email already exists", "user.email.already.exists"));
        }

        Result<Password> passwordResult = Password.create(command.getPassword());
        if (passwordResult.isFailure()) {
            return Result.failure(passwordResult.getError().get());
        }
        Password password = passwordResult.getValue().get();

        Maybe<String> maybeFirst = ValidationUtils.validateName(command.getFirstName());
        if (maybeFirst.isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME", ErrorType.VALIDATION_ERROR, "First name is required and cannot be empty", "user.invalid.first.name"));
        }
        String firstName = maybeFirst.getValue();

        Maybe<String> maybeLast = ValidationUtils.validateName(command.getLastName());
        if (maybeLast.isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_LAST_NAME", ErrorType.VALIDATION_ERROR, "Last name is required and cannot be empty", "user.invalid.last.name"));
        }
        String lastName = maybeLast.getValue();

        User newUser = User.createWithBank(email, password, firstName, lastName, command.getBankId(), command.getPaymentMethodId(), command.getId());

        // Step 6: Save user
        Result<UserId> saveResult = userRepository.userSaver(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        UserId userId = saveResult.getValue().get();
        newUser.setId(userId.getValue());

        com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                userId, Bcbs239Role.SYSTEM_ADMIN, "default-org"
        );

        Result<String> roleSaveResult = userRepository.userRoleSaver(adminRole);
        if (roleSaveResult.isFailure()) {
            return Result.failure(roleSaveResult.getError().get());
        }

        unitOfWork.registerEntity(newUser);

        unitOfWork.saveChanges();

        return Result.success(new RegisterUserResponse(userId, CorrelationContext.correlationId()));


    }


}

