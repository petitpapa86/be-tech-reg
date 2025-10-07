package com.bcbs239.regtech.iam.application.authenticate;

/**
 * OAuth2 Authentication Command
 *
 * Represents an OAuth2 authentication request with provider token
 */
public record OAuth2AuthenticationCommand(
    String provider, // "google" or "facebook"
    String token     // OAuth2 access token from provider
) {

    /**
     * Validates the OAuth2 authentication command
     */
    public boolean isValid() {
        return provider != null && !provider.trim().isEmpty() &&
               token != null && !token.trim().isEmpty() &&
               (provider.equals("google") || provider.equals("facebook"));
    }
}