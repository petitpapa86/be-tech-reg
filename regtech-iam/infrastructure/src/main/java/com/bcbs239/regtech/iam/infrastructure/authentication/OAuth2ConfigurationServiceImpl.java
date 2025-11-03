package com.bcbs239.regtech.iam.infrastructure.authentication;

import com.bcbs239.regtech.iam.domain.authentication.OAuth2ConfigurationService;
import com.bcbs239.regtech.iam.infrastructure.configuration.OAuth2Configuration;
import org.springframework.stereotype.Service;

/**
 * Infrastructure implementation of OAuth2ConfigurationService.
 * Bridges the domain interface with the infrastructure configuration.
 */
@Service
public class OAuth2ConfigurationServiceImpl implements OAuth2ConfigurationService {
    
    private final OAuth2Configuration oauth2Config;
    
    public OAuth2ConfigurationServiceImpl(OAuth2Configuration oauth2Config) {
        this.oauth2Config = oauth2Config;
    }
    
    @Override
    public OAuth2ProviderConfig getProviderConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> {
                if (!oauth2Config.getGoogle().isConfigured()) {
                    throw new IllegalStateException("Google OAuth2 is not configured");
                }
                yield new OAuth2ProviderConfig(
                    oauth2Config.getGoogle().clientId(),
                    oauth2Config.getGoogle().clientSecret(),
                    "https://oauth2.googleapis.com/token",
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    "openid email profile"
                );
            }
            case "facebook" -> {
                if (!oauth2Config.getFacebook().isConfigured()) {
                    throw new IllegalStateException("Facebook OAuth2 is not configured");
                }
                yield new OAuth2ProviderConfig(
                    oauth2Config.getFacebook().clientId(),
                    oauth2Config.getFacebook().clientSecret(),
                    "https://graph.facebook.com/v18.0/oauth/access_token",
                    "https://graph.facebook.com/me?fields=id,email,name,first_name,last_name,picture",
                    "email,public_profile"
                );
            }
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }
    
    @Override
    public boolean isProviderConfigured(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> oauth2Config.getGoogle().isConfigured();
            case "facebook" -> oauth2Config.getFacebook().isConfigured();
            default -> false;
        };
    }
    
    @Override
    public String getAuthorizationUrl(String provider, String redirectUri) {
        return switch (provider.toLowerCase()) {
            case "google" -> {
                if (!oauth2Config.getGoogle().isConfigured()) {
                    throw new IllegalStateException("Google OAuth2 is not configured");
                }
                yield String.format(
                    "https://accounts.google.com/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&response_type=code",
                    oauth2Config.getGoogle().clientId(),
                    redirectUri,
                    "openid email profile"
                );
            }
            case "facebook" -> {
                if (!oauth2Config.getFacebook().isConfigured()) {
                    throw new IllegalStateException("Facebook OAuth2 is not configured");
                }
                yield String.format(
                    "https://www.facebook.com/v18.0/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s",
                    oauth2Config.getFacebook().clientId(),
                    redirectUri,
                    "email,public_profile"
                );
            }
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }
}