package com.bcbs239.regtech.iam.domain.authentication;

/**
 * Domain service interface for OAuth2 configuration access.
 * This abstraction allows the application layer to access OAuth2 configuration
 * without depending on infrastructure implementation details.
 */
public interface OAuth2ConfigurationService {
    
    /**
     * Gets the OAuth2 configuration for a specific provider
     * 
     * @param provider The OAuth2 provider name (e.g., "google", "facebook")
     * @return The provider configuration
     * @throws IllegalArgumentException if the provider is not supported
     * @throws IllegalStateException if the provider is not configured
     */
    OAuth2ProviderConfig getProviderConfig(String provider);
    
    /**
     * Checks if a provider is configured and available
     * 
     * @param provider The OAuth2 provider name
     * @return true if the provider is configured, false otherwise
     */
    boolean isProviderConfigured(String provider);
    
    /**
     * Gets the authorization URL for a provider
     * 
     * @param provider The OAuth2 provider name
     * @param redirectUri The redirect URI after authorization
     * @return The authorization URL
     */
    String getAuthorizationUrl(String provider, String redirectUri);
    
    /**
     * OAuth2 provider configuration
     */
    record OAuth2ProviderConfig(
        String clientId,
        String clientSecret,
        String tokenUrl,
        String userInfoUrl,
        String scope
    ) {
        
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && 
                   clientSecret != null && !clientSecret.isBlank() &&
                   !clientId.contains("placeholder") && !clientSecret.contains("placeholder");
        }
    }
}

