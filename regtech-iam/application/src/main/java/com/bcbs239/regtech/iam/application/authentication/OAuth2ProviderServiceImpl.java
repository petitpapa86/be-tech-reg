package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.infrastructure.configuration.OAuth2Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Implementation of OAuth2ProviderService for Google and Facebook authentication
 */
@Service
public class OAuth2ProviderServiceImpl implements OAuth2ProviderService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProviderServiceImpl.class);

    private final OAuth2Configuration oauth2Config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OAuth2ProviderServiceImpl(OAuth2Configuration oauth2Config, ObjectMapper objectMapper) {
        this.oauth2Config = oauth2Config;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Result<OAuth2AuthenticationResult> authenticate(String provider, String authorizationCode) {
        try {
            // Exchange code for token
            Result<String> tokenResult = exchangeCodeForToken(provider, authorizationCode);
            if (tokenResult.isFailure()) {
                return Result.failure(tokenResult.getError().get());
            }

            String accessToken = tokenResult.getValue().get();

            // Get user info
            Result<OAuth2UserInfo> userInfoResult = getUserInfo(provider, accessToken);
            if (userInfoResult.isFailure()) {
                return Result.failure(userInfoResult.getError().get());
            }

            OAuth2UserInfo userInfo = userInfoResult.getValue().get();

            // Create authentication result
            OAuth2AuthenticationResult result = new OAuth2AuthenticationResult(
                provider,
                userInfo.id(),
                userInfo.email(),
                userInfo.name(),
                accessToken,
                null // Refresh token not implemented yet
            );

            return Result.success(result);

        } catch (Exception e) {
            logger.error("OAuth2 authentication failed for provider: {}", provider, e);
            return Result.failure(ErrorDetail.of("OAUTH2_AUTH_FAILED",
                "OAuth2 authentication failed: " + e.getMessage(),
                "error.oauth2.authFailed"));
        }
    }

    @Override
    public String getAuthorizationUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> {
                if (!oauth2Config.getGoogle().isConfigured()) {
                    throw new IllegalStateException("Google OAuth2 is not configured");
                }
                yield String.format(
                    "https://accounts.google.com/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&response_type=code",
                    oauth2Config.getGoogle().clientId(),
                    "http://localhost:8080/auth/oauth2/callback/google", // TODO: Make configurable
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
                    "http://localhost:8080/auth/oauth2/callback/facebook", // TODO: Make configurable
                    "email,public_profile"
                );
            }
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    @Override
    public Result<String> exchangeCodeForToken(String provider, String authorizationCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", authorizationCode);

            String tokenUrl;
            String clientId;
            String clientSecret;

            switch (provider.toLowerCase()) {
                case "google" -> {
                    if (!oauth2Config.getGoogle().isConfigured()) {
                        return Result.failure(ErrorDetail.of("OAUTH2_NOT_CONFIGURED",
                            "Google OAuth2 is not configured",
                            "error.oauth2.notConfigured"));
                    }
                    tokenUrl = "https://oauth2.googleapis.com/token";
                    clientId = oauth2Config.getGoogle().clientId();
                    clientSecret = oauth2Config.getGoogle().clientSecret();
                    params.add("redirect_uri", "http://localhost:8080/auth/oauth2/callback/google"); // TODO: Make configurable
                }
                case "facebook" -> {
                    if (!oauth2Config.getFacebook().isConfigured()) {
                        return Result.failure(ErrorDetail.of("OAUTH2_NOT_CONFIGURED",
                            "Facebook OAuth2 is not configured",
                            "error.oauth2.notConfigured"));
                    }
                    tokenUrl = "https://graph.facebook.com/v18.0/oauth/access_token";
                    clientId = oauth2Config.getFacebook().clientId();
                    clientSecret = oauth2Config.getFacebook().clientSecret();
                    params.add("redirect_uri", "http://localhost:8080/auth/oauth2/callback/facebook"); // TODO: Make configurable
                }
                default -> {
                    return Result.failure(ErrorDetail.of("OAUTH2_UNSUPPORTED_PROVIDER",
                        "Unsupported OAuth2 provider: " + provider,
                        "error.oauth2.unsupportedProvider"));
                }
            }

            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String accessToken = jsonNode.get("access_token").asText();
                return Result.success(accessToken);
            } else {
                logger.error("Token exchange failed with status: {} and body: {}", response.getStatusCode(), response.getBody());
                return Result.failure(ErrorDetail.of("OAUTH2_TOKEN_EXCHANGE_FAILED",
                    "Failed to exchange authorization code for token",
                    "error.oauth2.tokenExchangeFailed"));
            }

        } catch (Exception e) {
            logger.error("Token exchange failed for provider: {}", provider, e);
            return Result.failure(ErrorDetail.of("OAUTH2_TOKEN_EXCHANGE_ERROR",
                "Token exchange error: " + e.getMessage(),
                "error.oauth2.tokenExchangeError"));
        }
    }

    @Override
    public Result<OAuth2UserInfo> getUserInfo(String provider, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String userInfoUrl;

            switch (provider.toLowerCase()) {
                case "google" -> userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
                case "facebook" -> userInfoUrl = "https://graph.facebook.com/me?fields=id,email,name,first_name,last_name,picture";
                default -> {
                    return Result.failure(ErrorDetail.of("OAUTH2_UNSUPPORTED_PROVIDER",
                        "Unsupported OAuth2 provider: " + provider,
                        "error.oauth2.unsupportedProvider"));
                }
            }

            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                OAuth2UserInfo userInfo = new OAuth2UserInfo(
                    jsonNode.get("id").asText(),
                    jsonNode.get("email").asText(),
                    jsonNode.get("name").asText(),
                    jsonNode.has("first_name") ? jsonNode.get("first_name").asText() : null,
                    jsonNode.has("last_name") ? jsonNode.get("last_name").asText() : null,
                    jsonNode.has("picture") ? jsonNode.get("picture").asText() : null
                );

                return Result.success(userInfo);
            } else {
                logger.error("User info request failed with status: {} and body: {}", response.getStatusCode(), response.getBody());
                return Result.failure(ErrorDetail.of("OAUTH2_USER_INFO_FAILED",
                    "Failed to get user info from provider",
                    "error.oauth2.userInfoFailed"));
            }

        } catch (Exception e) {
            logger.error("User info request failed for provider: {}", provider, e);
            return Result.failure(ErrorDetail.of("OAUTH2_USER_INFO_ERROR",
                "User info error: " + e.getMessage(),
                "error.oauth2.userInfoError"));
        }
    }
}