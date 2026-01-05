package com.bcbs239.regtech.iam.presentation.usermanagement;

import jakarta.validation.constraints.*;

public record InviteUserRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}
