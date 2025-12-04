package com.bcbs239.regtech.iam.infrastructure;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.application.authentication.*;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.domain.banks.Bank;
import com.bcbs239.regtech.iam.domain.banks.BankName;
import com.bcbs239.regtech.iam.domain.banks.IBankRepository;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.config.IamModule;
import com.bcbs239.regtech.iam.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for regtech-iam module.
 * 
 * Tests:
 * - Module initialization
 * - User authentication flows
 * - JWT token generation and validation
 * - Refresh token functionality
 * - Authorization and role-based access control
 * - Bank selection functionality
 * 
 * Requirements: 14.1, 16.2
 */
@SpringBootTest(classes = {IamModule.class})
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class IAMModuleIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LoginCommandHandler loginHandler;

    @Autowired
    private RefreshTokenCommandHandler refreshTokenHandler;

    @Autowired
    private SelectBankCommandHandler selectBankHandler;

    @Autowired
    private LogoutCommandHandler logoutHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IBankRepository bankRepository;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private User testUser;
    private Bank testBank;

    @BeforeEach
    void setUp() {
        // Create test bank
        Result<BankName> bankNameResult = BankName.create("Test Bank");
        assertTrue(bankNameResult.isSuccess());
        
        testBank = Bank.create(
            UUID.randomUUID().toString(),
            bankNameResult.getValue().get(),
            "TEST"
        );
        bankRepository.save(testBank);

        // Create test user with password
        Result<Email> emailResult = Email.create("test@example.com");
        assertTrue(emailResult.isSuccess());
        
        Result<Password> passwordResult = Password.create("TestPassword123!");
        assertTrue(passwordResult.isSuccess());
        
        Result<Username> usernameResult = Username.create("testuser");
        assertTrue(usernameResult.isSuccess());

        testUser = User.create(
            UUID.randomUUID().toString(),
            usernameResult.getValue().get(),
            emailResult.getValue().get(),
            passwordResult.getValue().get(),
            Set.of("USER")
        );
        
        userRepository.save(testUser);
    }

    @Test
    void shouldInitializeIAMModuleSuccessfully() {
        // Verify the application context loaded successfully
        assertNotNull(applicationContext, "Application context should be initialized");
        
        // Verify IAM module configuration is loaded
        assertTrue(applicationContext.containsBean("iamModule"),
            "IAM module configuration should be registered");
    }

    @Test
    void shouldLoadAuthenticationComponents() {
        // Verify authentication command handlers
        assertNotNull(loginHandler, "LoginCommandHandler should be available");
        assertNotNull(refreshTokenHandler, "RefreshTokenCommandHandler should be available");
        assertNotNull(selectBankHandler, "SelectBankCommandHandler should be available");
        assertNotNull(logoutHandler, "LogoutCommandHandler should be available");
    }

    @Test
    void shouldLoadRepositories() {
        // Verify repositories are available
        assertNotNull(userRepository, "UserRepository should be available");
        assertNotNull(bankRepository, "BankRepository should be available");
        assertNotNull(refreshTokenRepository, "RefreshTokenRepository should be available");
    }

    @Test
    void shouldLoadJwtTokenService() {
        // Verify JWT token service is available
        assertNotNull(jwtTokenService, "JwtTokenService should be available");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Test user authentication flow
        Result<LoginCommand> commandResult = LoginCommand.create(
            "test@example.com",
            "TestPassword123!",
            "127.0.0.1"
        );
        assertTrue(commandResult.isSuccess(), "Login command should be created successfully");

        Result<LoginResponse> loginResult = loginHandler.handle(commandResult.getValue().get());
        
        assertTrue(loginResult.isSuccess(), "Login should succeed");
        LoginResponse response = loginResult.getValue().get();
        
        assertNotNull(response.accessToken(), "Access token should be generated");
        assertNotNull(response.refreshToken(), "Refresh token should be generated");
        assertNotNull(response.userId(), "User ID should be present");
        assertEquals("test@example.com", response.email(), "Email should match");
    }

    @Test
    void shouldFailAuthenticationWithInvalidPassword() {
        // Test authentication failure with wrong password
        Result<LoginCommand> commandResult = LoginCommand.create(
            "test@example.com",
            "WrongPassword123!",
            "127.0.0.1"
        );
        assertTrue(commandResult.isSuccess());

        Result<LoginResponse> loginResult = loginHandler.handle(commandResult.getValue().get());
        
        assertTrue(loginResult.isFailure(), "Login should fail with wrong password");
    }

    @Test
    void shouldFailAuthenticationWithNonExistentUser() {
        // Test authentication failure with non-existent user
        Result<LoginCommand> commandResult = LoginCommand.create(
            "nonexistent@example.com",
            "TestPassword123!",
            "127.0.0.1"
        );
        assertTrue(commandResult.isSuccess());

        Result<LoginResponse> loginResult = loginHandler.handle(commandResult.getValue().get());
        
        assertTrue(loginResult.isFailure(), "Login should fail for non-existent user");
    }

    @Test
    void shouldGenerateValidJwtToken() {
        // Test JWT token generation
        String userId = testUser.getId();
        String email = testUser.getEmail().getValue();
        
        String token = jwtTokenService.generateAccessToken(userId, email, Set.of("USER"), null);
        
        assertNotNull(token, "JWT token should be generated");
        assertFalse(token.isEmpty(), "JWT token should not be empty");
        
        // Validate the generated token
        assertTrue(jwtTokenService.validateToken(token), "Generated token should be valid");
    }

    @Test
    void shouldExtractUserIdFromJwtToken() {
        // Test extracting user ID from JWT token
        String userId = testUser.getId();
        String email = testUser.getEmail().getValue();
        
        String token = jwtTokenService.generateAccessToken(userId, email, Set.of("USER"), null);
        String extractedUserId = jwtTokenService.extractUserId(token);
        
        assertEquals(userId, extractedUserId, "Extracted user ID should match");
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // First, login to get a refresh token
        Result<LoginCommand> loginCommandResult = LoginCommand.create(
            "test@example.com",
            "TestPassword123!",
            "127.0.0.1"
        );
        assertTrue(loginCommandResult.isSuccess());

        Result<LoginResponse> loginResult = loginHandler.handle(loginCommandResult.getValue().get());
        assertTrue(loginResult.isSuccess());
        
        String refreshToken = loginResult.getValue().get().refreshToken();
        
        // Now test refresh token functionality
        Result<RefreshTokenCommand> refreshCommandResult = RefreshTokenCommand.create(refreshToken);
        assertTrue(refreshCommandResult.isSuccess());

        Result<RefreshTokenResponse> refreshResult = refreshTokenHandler.handle(
            refreshCommandResult.getValue().get()
        );
        
        assertTrue(refreshResult.isSuccess(), "Token refresh should succeed");
        RefreshTokenResponse response = refreshResult.getValue().get();
        
        assertNotNull(response.accessToken(), "New access token should be generated");
        assertNotNull(response.refreshToken(), "New refresh token should be generated");
    }

    @Test
    void shouldFailRefreshWithInvalidToken() {
        // Test refresh token failure with invalid token
        Result<RefreshTokenCommand> commandResult = RefreshTokenCommand.create("invalid-token");
        assertTrue(commandResult.isSuccess());

        Result<RefreshTokenResponse> refreshResult = refreshTokenHandler.handle(
            commandResult.getValue().get()
        );
        
        assertTrue(refreshResult.isFailure(), "Refresh should fail with invalid token");
    }

    @Test
    void shouldSelectBankSuccessfully() {
        // First, login to get tokens
        Result<LoginCommand> loginCommandResult = LoginCommand.create(
            "test@example.com",
            "TestPassword123!",
            "127.0.0.1"
        );
        assertTrue(loginCommandResult.isSuccess());

        Result<LoginResponse> loginResult = loginHandler.handle(loginCommandResult.getValue().get());
        assertTrue(loginResult.isSuccess());
        
        String userId = loginResult.getValue().get().userId();
        String refreshToken = loginResult.getValue().get().refreshToken();
        
        // Now test bank selection
        Result<SelectBankCommand> selectBankCommandResult = SelectBankCommand.create(
            userId,
            testBank.getId(),
            refreshToken
        );
        assertTrue(selectBankCommandResult.isSuccess());

        Result<SelectBankResponse> selectBankResult = selectBankHandler.handle(
            selectBankCommandResult.getValue().get()
        );
        
        assertTrue(selectBankResult.isSuccess(), "Bank selection should succeed");
        SelectBankResponse response = selectBankResult.getValue().get();
        
        assertNotNull(response.accessToken(), "New access token with bank context should be generated");
        assertEquals(testBank.getId(), response.bankId(), "Bank ID should match");
    }

    @Test
    void shouldLogoutSuccessfully() {
        // First, login to get tokens
        Result<LoginCommand> loginCommandResult = LoginCommand.create(
            "test@example.com",
            "TestPassword123!",
            "127.0.0.1"
        );
        assertTrue(loginCommandResult.isSuccess());

        Result<LoginResponse> loginResult = loginHandler.handle(loginCommandResult.getValue().get());
        assertTrue(loginResult.isSuccess());
        
        String userId = loginResult.getValue().get().userId();
        String refreshToken = loginResult.getValue().get().refreshToken();
        
        // Now test logout
        Result<LogoutCommand> logoutCommandResult = LogoutCommand.create(userId, refreshToken);
        assertTrue(logoutCommandResult.isSuccess());

        Result<LogoutResponse> logoutResult = logoutHandler.handle(
            logoutCommandResult.getValue().get()
        );
        
        assertTrue(logoutResult.isSuccess(), "Logout should succeed");
        
        // Verify refresh token is revoked
        Result<RefreshTokenCommand> refreshCommandResult = RefreshTokenCommand.create(refreshToken);
        assertTrue(refreshCommandResult.isSuccess());

        Result<RefreshTokenResponse> refreshResult = refreshTokenHandler.handle(
            refreshCommandResult.getValue().get()
        );
        
        assertTrue(refreshResult.isFailure(), "Refresh should fail after logout");
    }

    @Test
    void shouldHandleRoleBasedAccessControl() {
        // Verify user has correct roles
        Set<String> roles = testUser.getRoles();
        assertNotNull(roles, "User roles should not be null");
        assertTrue(roles.contains("USER"), "User should have USER role");
    }

    @Test
    void shouldPersistAndRetrieveUser() {
        // Test user persistence
        Optional<User> retrievedUser = userRepository.findByEmail(testUser.getEmail().getValue());
        
        assertTrue(retrievedUser.isPresent(), "User should be retrievable by email");
        assertEquals(testUser.getId(), retrievedUser.get().getId(), "User ID should match");
        assertEquals(testUser.getEmail().getValue(), retrievedUser.get().getEmail().getValue(), 
            "Email should match");
    }

    @Test
    void shouldPersistAndRetrieveBank() {
        // Test bank persistence
        Optional<Bank> retrievedBank = bankRepository.findById(testBank.getId());
        
        assertTrue(retrievedBank.isPresent(), "Bank should be retrievable by ID");
        assertEquals(testBank.getId(), retrievedBank.get().getId(), "Bank ID should match");
        assertEquals(testBank.getName().getValue(), retrievedBank.get().getName().getValue(), 
            "Bank name should match");
    }
}
