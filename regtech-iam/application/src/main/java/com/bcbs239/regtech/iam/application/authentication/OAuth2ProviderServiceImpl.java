package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.OAuth2ConfigurationService;
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

    private final OAuth2ConfigurationService oauth2ConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OAuth2ProviderServiceImpl(OAuth2ConfigurationService oauth2ConfigService, ObjectMapper objectMapper) {
        this.oauth2ConfigService = oauth2ConfigService;
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
        String redirectUri = switch (provider.toLowerCase()) {
            case "google" -> "http://localhost:8080/auth/oauth2/callback/google"; // TODO: Make configurable
            case "facebook" -> "http://localhost:8080/auth/oauth2/callback/facebook"; // TODO: Make configurable
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
        
        return oauth2ConfigService.getAuthorizationUrl(provider, redirectUri);
    }

    @Override
    public Result<String> exchangeCodeForToken(String provider, String authorizationCode) {
        try {
            if (!oauth2ConfigService.isProviderConfigured(provider)) {
                return Result.failure(ErrorDetail.of("OAUTH2_NOT_CONFIGURED",
                    provider + " OAuth2 is not configured",
                    "error.oauth2.notConfigured"));
            }

            OAuth2ConfigurationService.OAuth2ProviderConfig config = oauth2ConfigService.getProviderConfig(provider);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", authorizationCode);
            params.add("client_id", config.clientId());
            params.add("client_secret", config.clientSecret());

            // Add redirect URI based on provider
            String redirectUri;
            switch (provider.toLowerCase()) {
                case "google" -> redirectUri = "http://localhost:8080/auth/oauth2/callback/google"; // TODO: Make configurable
                case "facebook" -> redirectUri = "http://localhost:8080/auth/oauth2/callback/facebook"; // TODO: Make configurable
                default -> {
                    return Result.failure(ErrorDetail.of("OAUTH2_UNSUPPORTED_PROVIDER",
                        "Unsupported OAuth2 provider: " + provider,
                        "error.oauth2.unsupportedProvider"));
                }
            }
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(config.tokenUrl(), request, String.class);

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
            if (!oauth2ConfigService.isProviderConfigured(provider)) {
                return Result.failure(ErrorDetail.of("OAUTH2_NOT_CONFIGURED",
                    provider + " OAuth2 is not configured",
                    "error.oauth2.notConfigured"));
            }

            OAuth2ConfigurationService.OAuth2ProviderConfig config = oauth2ConfigService.getProviderConfig(provider);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(config.userInfoUrl(), HttpMethod.GET, entity, String.class);

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