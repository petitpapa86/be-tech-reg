package com.bcbs239.regtech.iam.application.users;


import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.*;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Service
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;
    private final PasswordHasher passwordHasher;

    public RegisterUserCommandHandler(
            UserRepository userRepository,
            BaseUnitOfWork unitOfWork,
            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.unitOfWork = unitOfWork;
        this.passwordHasher = passwordHasher;
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

        // Validate password strength
        Result<Void> passwordValidation = Password.validateStrength(command.getPassword());
        if (passwordValidation.isFailure()) {
            return Result.failure(passwordValidation.getError().get());
        }
        
        // Hash the password using BCrypt
        String hashedPassword = passwordHasher.hash(command.getPassword());
        Password password = Password.fromHash(hashedPassword);

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

        User newUser = User.createWithBank(email, password, firstName, lastName, command.getBankId(), command.getPaymentMethodId());

        // Step 6: Save user
        Result<UserId> saveResult = userRepository.userSaver(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        UserId userId = saveResult.getValue().get();

        com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                userId, "SYSTEM_ADMIN", "default-org"
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

