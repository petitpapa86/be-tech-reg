package com.bcbs239.regtech.iam.application.authenticate;

import com.bcbs239.regtech.iam.domain.users.Email;

/**
 * OAuth2 User Info record for authentication
 */
public record OAuth2UserInfo(
    Email email,
    String firstName,
    String lastName,
    String externalId
) {}