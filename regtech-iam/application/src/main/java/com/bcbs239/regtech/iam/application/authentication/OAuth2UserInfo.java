package com.bcbs239.regtech.iam.application.authentication;

/**
 * User information from OAuth2 provider
 */
public record OAuth2UserInfo(
    String id,
    String email,
    String name,
    String firstName,
    String lastName,
    String picture
) {}