package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.application.shared.Result;

/**
 * Service interface for OAuth2 provider authentication
 */
public interface OAuth2ProviderService {

    /**
     * Authenticates a user using OAuth2 provider
     *
     * @param provider The OAuth2 provider (google, facebook)
     * @param authorizationCode The authorization code from OAuth2 flow
     * @return Result of authentication attempt
     */
    Result<OAuth2AuthenticationResult> authenticate(String provider, String authorizationCode);

    /**
     * Gets the authorization URL for OAuth2 provider
     *
     * @param provider The OAuth2 provider (google, facebook)
     * @return The authorization URL
     */
    String getAuthorizationUrl(String provider);

    /**
     * Exchanges authorization code for access token
     *
     * @param provider The OAuth2 provider (google, facebook)
     * @param authorizationCode The authorization code
     * @return Result containing access token
     */
    Result<String> exchangeCodeForToken(String provider, String authorizationCode);

    /**
     * Gets user info from OAuth2 provider using access token
     *
     * @param provider The OAuth2 provider (google, facebook)
     * @param accessToken The access token
     * @return Result containing user info
     */
    Result<OAuth2UserInfo> getUserInfo(String provider, String accessToken);
}