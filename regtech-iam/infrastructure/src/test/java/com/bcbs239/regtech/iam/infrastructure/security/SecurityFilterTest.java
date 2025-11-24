package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.domain.security.SecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContextHolder;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.Email;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.iam.domain.users.UserStatus;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityFilter JWT validation.
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
class SecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityFilter securityFilter;
    private IAMProperties iamProperties;
    // HS512 requires at least 512 bits (64 bytes) - this string is exactly 64 characters
    private String jwtSecret = "testsecretkeyforjwtsigningmustbelongenoughforhs512algorithm!!";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup IAM properties
        iamProperties = new IAMProperties();
        IAMProperties.SecurityProperties securityProps = new IAMProperties.SecurityProperties();
        IAMProperties.SecurityProperties.JwtProperties jwtProps = new IAMProperties.SecurityProperties.JwtProperties();
        jwtProps.setSecret(jwtSecret);
        jwtProps.setAccessTokenExpirationMinutes(15);
        securityProps.setJwt(jwtProps);
        securityProps.setPublicPaths(List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/select-bank",
            "/h2-console/**"
        ));
        iamProperties.setSecurity(securityProps);

        securityFilter = new SecurityFilter(iamProperties);
    }

    @Test
    void testPublicPathBypassesAuthentication() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testMissingAuthorizationHeaderReturns401() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);

        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("MISSING_TOKEN"));
    }

    @Test
    void testInvalidBearerFormatReturns401() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testExpiredTokenReturns401() throws Exception {
        // Arrange - Create an expired token manually
        // We'll create a token that's already expired
        String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOiIxMjM0NTY3OC05MGFiLWNkZWYtMTIzNC01Njc4OTBhYmNkZWYiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJiYW5rcyI6W10sImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDAwMDAxfQ.invalid";

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);

        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("JWT_") || responseBody.contains("INVALID_TOKEN"));
    }

    @Test
    void testInvalidSignatureReturns401() throws Exception {
        // Arrange - Create token with invalid signature
        String invalidToken = "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOiIxMjM0NTY3OC05MGFiLWNkZWYtMTIzNC01Njc4OTBhYmNkZWYiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJiYW5rcyI6W10sImlhdCI6MTYwMDAwMDAwMCwiZXhwIjo5OTk5OTk5OTk5fQ.invalid-signature";

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);

        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("JWT_") || responseBody.contains("INVALID_TOKEN"));
    }

    @Test
    void testValidTokenPopulatesSecurityContext() throws Exception {
        // Arrange - Create valid token using JwtToken.generate
        User user = createTestUser();
        user.assignToBank("bank-123", "USER");
        
        var tokenResult = JwtToken.generate(user, jwtSecret, Duration.ofMinutes(15));
        if (tokenResult.isFailure()) {
            fail("Failed to generate token: " + tokenResult.getError().get().getMessage());
        }
        JwtToken validToken = tokenResult.getValue().get();

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken.value());

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void testWildcardPublicPathMatching() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/h2-console/login.do");

        // Act
        securityFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private User createTestUser() {
        Email email = Email.create("test@example.com").getValue().get();
        Password password = Password.create("TestPassword123!").getValue().get();

        return User.create(
            email,
            password,
            "Test",
            "User"
        );
    }
}
