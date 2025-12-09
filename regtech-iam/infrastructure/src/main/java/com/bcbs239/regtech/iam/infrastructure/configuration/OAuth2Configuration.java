package com.bcbs239.regtech.iam.infrastructure.configuration;

/**
 * Type-safe configuration for OAuth2 settings.
 * Provides access to OAuth2 provider configurations.
 */
public record OAuth2Configuration(
    GoogleConfig google,
    FacebookConfig facebook
) {
    
    /**
     * Google OAuth2 configuration
     */
    public record GoogleConfig(
        String clientId,
        String clientSecret
    ) {
        
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && 
                   clientSecret != null && !clientSecret.isBlank() &&
                   !clientId.contains("placeholder") && !clientSecret.contains("placeholder");
        }
        
        public void validate() {
            if (!isConfigured()) {
                throw new IllegalStateException("Google OAuth2 configuration is incomplete");
            }
            if (clientId.length() < 10) {
                throw new IllegalStateException("Google OAuth2 client ID appears to be invalid");
            }
            if (clientSecret.length() < 10) {
                throw new IllegalStateException("Google OAuth2 client secret appears to be invalid");
            }
        }
    }
    
    /**
     * Facebook OAuth2 configuration
     */
    public record FacebookConfig(
        String clientId,
        String clientSecret
    ) {
        
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && 
                   clientSecret != null && !clientSecret.isBlank() &&
                   !clientId.contains("placeholder") && !clientSecret.contains("placeholder");
        }
        
        public void validate() {
            if (!isConfigured()) {
                throw new IllegalStateException("Facebook OAuth2 configuration is incomplete");
            }
            if (clientId.length() < 10) {
                throw new IllegalStateException("Facebook OAuth2 client ID appears to be invalid");
            }
            if (clientSecret.length() < 10) {
                throw new IllegalStateException("Facebook OAuth2 client secret appears to be invalid");
            }
        }
    }
    
    /**
     * Gets Google OAuth2 configuration
     */
    public GoogleConfig getGoogle() {
        return google;
    }
    
    /**
     * Gets Facebook OAuth2 configuration
     */
    public FacebookConfig getFacebook() {
        return facebook;
    }
    
    /**
     * Checks if any OAuth2 provider is configured
     */
    public boolean hasConfiguredProviders() {
        return google.isConfigured() || facebook.isConfigured();
    }
    
    /**
     * Validates OAuth2 configuration
     */
    public void validate() {
        // OAuth2 providers are optional, but if configured, they should be valid
        if (google.clientId != null && !google.clientId.isBlank()) {
            google.validate();
        }
        if (facebook.clientId != null && !facebook.clientId.isBlank()) {
            facebook.validate();
        }
    }
}

