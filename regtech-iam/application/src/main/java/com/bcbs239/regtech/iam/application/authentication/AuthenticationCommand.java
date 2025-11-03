package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.domain.users.Bcbs239Role;
import com.bcbs239.regtech.iam.domain.users.Email;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Authentication Command
 *
 * Represents a user authentication request with email and password
 */
public record AuthenticationCommand(
    String email,
    String password
) {

    /**
     * Validates the authentication command
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }
}