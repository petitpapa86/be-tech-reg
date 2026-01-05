# Gestione Utenti - Complete Top-Down Development Plan

we already have results pattern and value objects in C:\Users\alseny\Desktop\react projects\regtech\regtech-core\domain\src\main\java\com\bcbs239\regtech\core\domain\shared
the migration is here C:\Users\alseny\Desktop\react projects\regtech\regtech-app\src\main\resources\db\migration\iam


# Implementation Plan - Using EXISTING Domain Only

## ⚠️ CRITICAL: DO NOT CREATE NEW DOMAIN MODELS

**Use these EXISTING domain models:**
- `com.bcbs239.regtech.iam.domain.users.User`
- `com.bcbs239.regtech.iam.domain.users.UserId`
- `com.bcbs239.regtech.iam.domain.users.UserRole`
- `com.bcbs239.regtech.iam.domain.users.UserStatus` (enum: PENDING_PAYMENT, ACTIVE, SUSPENDED, CANCELLED)
- `com.bcbs239.regtech.iam.domain.users.UserRepository`
- `com.bcbs239.regtech.iam.domain.users.Password`
- `com.bcbs239.regtech.core.domain.shared.valueobjects.Email`

**DO NOT create:**
- ❌ New User aggregate
- ❌ New Email value object
- ❌ New UserId value object
- ❌ New UserRole class
- ❌ New UserStatus enum

---

## Part 1: Database Migrations

### Migration 1: Italian Translations for Roles

**File: `regtech-app/src/main/resources/db/migration/iam/V014__add_italian_translations.sql`**

```sql
-- Add Italian translations to existing roles and permissions system

-- 1. Add Italian columns to roles table
ALTER TABLE iam.roles 
ADD COLUMN IF NOT EXISTS display_name_it VARCHAR(100),
ADD COLUMN IF NOT EXISTS description_it TEXT;

-- 2. Update existing roles with Italian translations
UPDATE iam.roles SET 
    display_name_it = 'Visualizzatore Base',
    description_it = 'Può solo visualizzare report e dati - accesso in sola lettura per utenti di base'
WHERE name = 'VIEWER';

UPDATE iam.roles SET 
    display_name_it = 'Analista Dati',
    description_it = 'Può caricare file e visualizzare report - gestisce elaborazione e analisi dati'
WHERE name = 'DATA_ANALYST';

UPDATE iam.roles SET 
    display_name_it = 'Revisore',
    description_it = 'Accesso in sola lettura con capacità di audit - monitora sistema e traccia sottomissioni'
WHERE name = 'AUDITOR';

UPDATE iam.roles SET 
    display_name_it = 'Responsabile Rischi',
    description_it = 'Può gestire violazioni e generare report - gestisce valutazione e mitigazione rischi'
WHERE name = 'RISK_MANAGER';

UPDATE iam.roles SET 
    display_name_it = 'Responsabile Compliance',
    description_it = 'Capacità complete di gestione compliance - supervisiona conformità normativa e reporting'
WHERE name = 'COMPLIANCE_OFFICER';

UPDATE iam.roles SET 
    display_name_it = 'Amministratore Banca',
    description_it = 'Gestisce configurazioni specifiche della banca - amministra impostazioni e utenti a livello banca'
WHERE name = 'BANK_ADMIN';

UPDATE iam.roles SET 
    display_name_it = 'Utente Holding',
    description_it = 'Può visualizzare attraverso più banche - accesso a dati consolidati e report'
WHERE name = 'HOLDING_COMPANY_USER';

UPDATE iam.roles SET 
    display_name_it = 'Amministratore Sistema',
    description_it = 'Accesso completo al sistema - controllo amministrativo completo su tutto il sistema'
WHERE name = 'SYSTEM_ADMIN';

-- 3. Create permission translations table
CREATE TABLE IF NOT EXISTS iam.permission_translations (
    permission_code VARCHAR(100) PRIMARY KEY,
    display_name_en VARCHAR(200) NOT NULL,
    display_name_it VARCHAR(200) NOT NULL,
    description_it TEXT,
    category VARCHAR(50) NOT NULL
);

-- 4. Insert permission translations (only Italian needed - English from permission code)
INSERT INTO iam.permission_translations (permission_code, display_name_en, display_name_it, description_it, category) VALUES
-- File Operations
('BCBS239_UPLOAD_FILES', 'Upload Files', 'Carica File', 'Carica file dati per elaborazione', 'FILES'),
('BCBS239_DOWNLOAD_FILES', 'Download Files', 'Scarica File', 'Scarica file dati', 'FILES'),
('BCBS239_DELETE_FILES', 'Delete Files', 'Elimina File', 'Elimina file dati', 'FILES'),

-- Report Operations
('BCBS239_VIEW_REPORTS', 'View Reports', 'Visualizza Report', 'Visualizza report generati', 'REPORTS'),
('BCBS239_GENERATE_REPORTS', 'Generate Reports', 'Genera Report', 'Genera nuovi report', 'REPORTS'),
('BCBS239_EXPORT_REPORTS', 'Export Reports', 'Esporta Report', 'Esporta report in formati esterni', 'REPORTS'),
('BCBS239_SCHEDULE_REPORTS', 'Schedule Reports', 'Pianifica Report', 'Pianifica generazione automatica report', 'REPORTS'),

-- Configuration
('BCBS239_CONFIGURE_PARAMETERS', 'Configure Parameters', 'Configura Parametri', 'Configura parametri di sistema', 'CONFIG'),
('BCBS239_MANAGE_TEMPLATES', 'Manage Templates', 'Gestisci Template', 'Gestisci template report', 'CONFIG'),
('BCBS239_CONFIGURE_WORKFLOWS', 'Configure Workflows', 'Configura Flussi', 'Configura flussi di lavoro', 'CONFIG'),
('BCBS239_MANAGE_BANK_CONFIG', 'Manage Bank Config', 'Gestisci Config Banca', 'Gestisci configurazione banca', 'CONFIG'),
('BCBS239_MANAGE_SYSTEM_CONFIG', 'Manage System Config', 'Gestisci Config Sistema', 'Gestisci configurazione sistema', 'CONFIG'),

-- Violations
('BCBS239_VIEW_VIOLATIONS', 'View Violations', 'Visualizza Violazioni', 'Visualizza violazioni conformità', 'VIOLATIONS'),
('BCBS239_MANAGE_VIOLATIONS', 'Manage Violations', 'Gestisci Violazioni', 'Gestisci violazioni conformità', 'VIOLATIONS'),
('BCBS239_APPROVE_VIOLATIONS', 'Approve Violations', 'Approva Violazioni', 'Approva risoluzioni violazioni', 'VIOLATIONS'),

-- Data Management
('BCBS239_VALIDATE_DATA', 'Validate Data', 'Valida Dati', 'Valida qualità dati', 'DATA'),
('BCBS239_APPROVE_DATA', 'Approve Data', 'Approva Dati', 'Approva dati per sottomissione', 'DATA'),
('BCBS239_REJECT_DATA', 'Reject Data', 'Rifiuta Dati', 'Rifiuta sottomissioni dati', 'DATA'),

-- User Administration
('BCBS239_ADMINISTER_USERS', 'Administer Users', 'Amministra Utenti', 'Gestisci account utenti', 'USERS'),
('BCBS239_ASSIGN_ROLES', 'Assign Roles', 'Assegna Ruoli', 'Assegna ruoli agli utenti', 'USERS'),

-- Audit & Monitoring
('BCBS239_VIEW_AUDIT_LOGS', 'View Audit Logs', 'Visualizza Log Audit', 'Visualizza log audit sistema', 'AUDIT'),
('BCBS239_MONITOR_SYSTEM', 'Monitor System', 'Monitora Sistema', 'Monitora salute e prestazioni sistema', 'AUDIT'),
('BCBS239_TRACK_SUBMISSIONS', 'Track Submissions', 'Traccia Sottomissioni', 'Traccia sottomissioni normative', 'AUDIT'),

-- Regulatory
('BCBS239_SUBMIT_REGULATORY_REPORTS', 'Submit Reports', 'Sottometti Report', 'Sottometti report alle autorità', 'REGULATORY'),
('BCBS239_REVIEW_SUBMISSIONS', 'Review Submissions', 'Rivedi Sottomissioni', 'Rivedi sottomissioni normative', 'REGULATORY'),

-- Cross-Bank (Holding)
('BCBS239_VIEW_CROSS_BANK_DATA', 'View Cross-Bank Data', 'Visualizza Dati Multi-Banca', 'Visualizza dati su più banche', 'HOLDING'),
('BCBS239_CONSOLIDATE_REPORTS', 'Consolidate Reports', 'Consolida Report', 'Consolida report da più banche', 'HOLDING'),

-- System Administration
('BCBS239_BACKUP_RESTORE', 'Backup & Restore', 'Backup e Ripristino', 'Esegui backup e ripristino sistema', 'SYSTEM')
ON CONFLICT (permission_code) DO NOTHING;
```

### Migration 2: Multi-Tenancy for Users

**File: `regtech-app/src/main/resources/db/migration/iam/V015__add_bank_context_to_users.sql`**

```sql
-- Add multi-tenancy context to existing iam.users table
-- DO NOT create new users table - extend existing one

-- 1. Add bank_id for multi-tenancy
ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS bank_id BIGINT;

-- Add foreign key to bank_profile
ALTER TABLE iam.users
ADD CONSTRAINT fk_users_bank_id 
    FOREIGN KEY (bank_id) 
    REFERENCES bank_profile(bank_id) 
    ON DELETE RESTRICT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_bank_id ON iam.users(bank_id);

-- 2. Update email uniqueness: per bank, not globally
DROP INDEX IF EXISTS iam.users_email_key;
ALTER TABLE iam.users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_bank_id ON iam.users(email, bank_id);

-- 3. Backfill existing users with default bank_id
UPDATE iam.users SET bank_id = 1 WHERE bank_id IS NULL;
ALTER TABLE iam.users ALTER COLUMN bank_id SET NOT NULL;

-- 4. Add invitation workflow columns
ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS invitation_token VARCHAR(64),
ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS invited_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS last_access TIMESTAMP;

-- 5. Add constraint: PENDING_PAYMENT status means invitation pending
ALTER TABLE iam.users
ADD CONSTRAINT chk_pending_invitation 
    CHECK (
        (status = 'PENDING_PAYMENT' AND invitation_token IS NOT NULL) OR
        (status != 'PENDING_PAYMENT')
    );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_invitation_token ON iam.users(invitation_token) WHERE invitation_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_bank_status ON iam.users(bank_id, status);

-- Comments
COMMENT ON COLUMN iam.users.bank_id IS 'Bank context - user belongs to one bank (multi-tenancy)';
COMMENT ON COLUMN iam.users.invitation_token IS 'Secure token for pending user invitation (status = PENDING_PAYMENT)';
COMMENT ON COLUMN iam.users.last_access IS 'Last successful login timestamp';
```

---

## Part 2: Extend EXISTING UserRepository Interface

**File: `regtech-iam/domain/src/main/java/com/bcbs239/regtech/iam/domain/users/UserRepository.java`**

```java
package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import java.util.List;

/**
 * Domain repository interface for User aggregate operations.
 * 
 * EXTENDED with bank-scoped queries for multi-tenancy
 */
public interface UserRepository {
    
    // ========================================
    // EXISTING METHODS - DO NOT REMOVE
    // ========================================
    
    Maybe<User> userLoader(UserId userId);
    Result<UserId> userSaver(User user);
    List<UserRole> userRolesFinder(UserId userId);
    List<UserRole> userOrgRolesFinder(UserOrgQuery query);
    Result<String> userRoleSaver(UserRole userRole);
    Maybe<User> emailLookup(Email email);
    Result<JwtToken> tokenGenerator(User user, String secretKey);
    
    record UserOrgQuery(UserId userId, String organizationId) {}
    
    // ========================================
    // NEW METHODS - For User Management
    // ========================================
    
    /**
     * Find all users for a specific bank
     */
    List<User> findByBankId(Long bankId);
    
    /**
     * Find active users for a specific bank
     */
    List<User> findActiveByBankId(Long bankId);
    
    /**
     * Find pending users (PENDING_PAYMENT status) for a specific bank
     */
    List<User> findPendingByBankId(Long bankId);
    
    /**
     * Check if email exists within a bank
     */
    boolean existsByEmailAndBankId(Email email, Long bankId);
    
    /**
     * Find user by email within a specific bank
     */
    Maybe<User> findByEmailAndBankId(Email email, Long bankId);
    
    /**
     * Delete user (for revoking pending invitations)
     */
    void deleteUser(UserId userId);
}
```

---

## Part 3: Application Layer - User Management Handlers

### Handler 1: Get Users by Bank

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/GetUsersByBankHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Query Handler: Get users by bank
 * 
 * Uses EXISTING User domain model
 */
@Service
@RequiredArgsConstructor
public class GetUsersByBankHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Query {
        Long bankId;
        String filter; // "all", "active", "pending"
    }
    
    @Transactional(readOnly = true)
    public Result<List<User>> handle(Query query) {
        if (query.bankId == null || query.bankId <= 0) {
            return Result.failure("Invalid bank ID");
        }
        
        List<User> users = switch (query.filter.toLowerCase()) {
            case "active" -> userRepository.findActiveByBankId(query.bankId);
            case "pending" -> userRepository.findPendingByBankId(query.bankId);
            default -> userRepository.findByBankId(query.bankId);
        };
        
        return Result.success(users);
    }
}
```

### Handler 2: Invite User

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/InviteUserHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Command Handler: Invite new user
 * 
 * Creates user with PENDING_PAYMENT status + invitation token
 * Uses EXISTING User.createWithBank() factory method
 */
@Service
@RequiredArgsConstructor
public class InviteUserHandler {
    
    private final UserRepository userRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @Value
    public static class Command {
        Long bankId;
        String email;
        String firstName;
        String lastName;
        String invitedBy;
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // Validate email
        var emailResult = Email.create(command.email);
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Email email = emailResult.getValue().get();
        
        // Check if user already exists in this bank
        if (userRepository.existsByEmailAndBankId(email, command.bankId)) {
            return Result.failure("User with email " + command.email + " already exists in this bank");
        }
        
        // Generate secure invitation token
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String invitationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Create temporary password (will be set when user accepts invitation)
        var passwordResult = Password.create("TEMP_" + invitationToken.substring(0, 16));
        if (passwordResult.isFailure()) {
            return Result.failure(passwordResult.getError().get());
        }
        
        // Create user using EXISTING factory method
        User user = User.create(
            email,
            passwordResult.getValue().get(),
            command.firstName,
            command.lastName
        );
        
        // Assign to bank (adds BankAssignment)
        user.assignToBank(command.bankId.toString(), "USER");
        
        // User status is already PENDING_PAYMENT by default from factory
        // Store invitation metadata (will be added to User entity or separate table)
        
        // Save user
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // TODO: Send invitation email with token
        
        return Result.success(user);
    }
}
```

### Handler 3: Update User Role

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/UpdateUserRoleHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Update user's role
 * 
 * Uses EXISTING UserRole and UserRepository
 */
@Service
@RequiredArgsConstructor
public class UpdateUserRoleHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
        String newRoleName; // e.g., "BANK_ADMIN", "DATA_ANALYST"
        String organizationId;
        String modifiedBy;
    }
    
    @Transactional
    public Result<UserRole> handle(Command command) {
        // Parse user ID
        UserId userId;
        try {
            userId = UserId.fromString(command.userId);
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid user ID format");
        }
        
        // Find user
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure("User not found");
        }
        
        // Validate role name exists
        // TODO: Query iam.roles to validate role exists
        
        // Create new user role using EXISTING UserRole.create()
        UserRole userRole = UserRole.create(userId, command.newRoleName, command.organizationId);
        
        // Save role
        var saveResult = userRepository.userRoleSaver(userRole);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(userRole);
    }
}
```

### Handler 4: Suspend User

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/SuspendUserHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Suspend user account
 * 
 * Uses EXISTING User.suspend() domain method
 */
@Service
@RequiredArgsConstructor
public class SuspendUserHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
        String suspendedBy;
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // Parse user ID
        UserId userId;
        try {
            userId = UserId.fromString(command.userId);
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid user ID format");
        }
        
        // Find user
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure("User not found");
        }
        
        User user = userMaybe.get();
        
        // Use EXISTING domain method
        user.suspend();
        
        // Save
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        return Result.success(user);
    }
}
```

### Handler 5: Revoke Invitation

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/RevokeInvitationHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Revoke pending user invitation
 * 
 * Only works for users with PENDING_PAYMENT status
 */
@Service
@RequiredArgsConstructor
public class RevokeInvitationHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        String userId;
    }
    
    @Transactional
    public Result<Void> handle(Command command) {
        // Parse user ID
        UserId userId;
        try {
            userId = UserId.fromString(command.userId);
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid user ID format");
        }
        
        // Find user
        var userMaybe = userRepository.userLoader(userId);
        if (userMaybe.isEmpty()) {
            return Result.failure("User not found");
        }
        
        User user = userMaybe.get();
        
        // Business rule: Can only revoke pending invitations
        if (user.getStatus() != UserStatus.PENDING_PAYMENT) {
            return Result.failure("Can only revoke pending invitations");
        }
        
        // Delete user
        userRepository.deleteUser(userId);
        
        return Result.success(null);
    }
}
```

---

## Part 4: Presentation Layer - DTOs

**File: `regtech-iam/presentation/src/main/java/com/bcbs239/regtech/iam/presentation/usermanagement/UserResponse.java`**

```java
package com.bcbs239.regtech.iam.presentation.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;

/**
 * Response DTO mapping from EXISTING User domain model
 */
public record UserResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String initials,
    String status,
    String lastAccess,
    boolean hasRecentActivity
) {
    public static UserResponse from(User user) {
        String fullName = user.getFirstName() + " " + user.getLastName();
        String initials = getInitials(user.getFirstName(), user.getLastName());
        
        return new UserResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getFirstName(),
            user.getLastName(),
            fullName,
            initials,
            user.getStatus().name(),
            user.getUpdatedAt().toString(),
            isRecentActivity(user.getUpdatedAt())
        );
    }
    
    private static String getInitials(String firstName, String lastName) {
        String first = firstName != null && !firstName.isEmpty() ? firstName.substring(0, 1) : "";
        String last = lastName != null && !lastName.isEmpty() ? lastName.substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
    
    private static boolean isRecentActivity(java.time.Instant updatedAt) {
        return java.time.Duration.between(updatedAt, java.time.Instant.now()).toDays() <= 7;
    }
}
```

**File: `regtech-iam/presentation/src/main/java/com/bcbs239/regtech/iam/presentation/usermanagement/InviteUserRequest.java`**

```java
package com.bcbs239.regtech.iam.presentation.usermanagement;

import jakarta.validation.constraints.*;

public record InviteUserRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}
```

---

## Part 5: Presentation Layer - Controller with Route Functions

**File: `regtech-iam/presentation/src/main/java/com/bcbs239/regtech/iam/presentation/usermanagement/UserManagementController.java`**

```java
package com.bcbs239.regtech.iam.presentation.usermanagement;

import com.bcbs239.regtech.iam.application.usermanagement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * User Management Routes
 * Uses EXISTING User domain model
 */
@Configuration
@RequiredArgsConstructor
public class UserManagementController {
    
    private final GetUsersByBankHandler getUsersByBankHandler;
    private final InviteUserHandler inviteUserHandler;
    private final UpdateUserRoleHandler updateUserRoleHandler;
    private final SuspendUserHandler suspendUserHandler;
    private final RevokeInvitationHandler revokeInvitationHandler;
    
    @Bean
    public RouterFunction<ServerResponse> userManagementRoutes() {
        return route()
            .GET("/api/v1/banks/{bankId}/users", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::getUsers)
            .POST("/api/v1/banks/{bankId}/users/invite", 
                  accept(MediaType.APPLICATION_JSON), 
                  this::inviteUser)
            .PUT("/api/v1/users/{userId}/role", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::updateUserRole)
            .PUT("/api/v1/users/{userId}/suspend", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::suspendUser)
            .DELETE("/api/v1/users/{userId}/invitation", 
                    accept(MediaType.APPLICATION_JSON), 
                    this::revokeInvitation)
            .build();
    }
    
    private ServerResponse getUsers(org.springframework.web.servlet.function.ServerRequest request) {
        Long bankId = Long.parseLong(request.pathVariable("bankId"));
        String filter = request.param("filter").orElse("all");
        
        var query = new GetUsersByBankHandler.Query(bankId, filter);
        var result = getUsersByBankHandler.handle(query);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        var users = result.getValue();
        var response = users.stream().map(UserResponse::from).toList();
        
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("users", response));
    }
    
    private ServerResponse inviteUser(org.springframework.web.servlet.function.ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            var req = request.body(InviteUserRequest.class);
            
            var command = new InviteUserHandler.Command(
                bankId,
                req.email(),
                req.firstName(),
                req.lastName(),
                "Admin" // TODO: Get from security context
            );
            
            var result = inviteUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .body(Map.of("error", result.getError().get().getMessage()));
            }
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(UserResponse.from(result.getValue()));
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    private ServerResponse updateUserRole(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        // TODO: Parse request body for role update
        
        return ServerResponse.noContent().build();
    }
    
    private ServerResponse suspendUser(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        var command = new SuspendUserHandler.Command(userId, "Admin");
        var result = suspendUserHandler.handle(command);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        return ServerResponse.ok()
            .body(UserResponse.from(result.getValue()));
    }
    
    private ServerResponse revokeInvitation(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        var command = new RevokeInvitationHandler.Command(userId);
        var result = revokeInvitationHandler.handle(command);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        return ServerResponse.noContent().build();
    }
}
```

---

## Summary Checklist

### ✅ Database Migrations
- [ ] V014: Italian translations for roles/permissions
- [ ] V015: Multi-tenancy (bank_id) for users

### ✅ Domain Layer (EXTEND only, don't create new)
- [ ] Add methods to EXISTING UserRepository interface

### ✅ Application Layer
- [ ] GetUsersByBankHandler
- [ ] InviteUserHandler
- [ ] UpdateUserRoleHandler
- [ ] SuspendUserHandler
- [ ] RevokeInvitationHandler

### ✅ Presentation Layer
- [ ] UserResponse DTO (maps from existing User)
- [ ] InviteUserRequest DTO
- [ ] UserManagementController (route functions)

### ⚠️ DO NOT CREATE
- ❌ New User aggregate
- ❌ New UserId, Email, Password value objects
- ❌ New UserRole class
- ❌ New UserStatus enum

### 🎯 USE EXISTING
- ✅ `com.bcbs239.regtech.iam.domain.users.User`
- ✅ `com.bcbs239.regtech.iam.domain.users.UserId`
- ✅ `com.bcbs239.regtech.iam.domain.users.UserRole`
- ✅ `com.bcbs239.regtech.iam.domain.users.UserStatus`
- ✅ `com.bcbs239.regtech.iam.domain.users.UserRepository`
- ✅ `com.bcbs239.regtech.core.domain.shared.valueobjects.Email`

This plan ONLY extends your existing domain without creating duplicates! 🎯

# Add New User Handler - Direct User Creation

## Handler: Add New Active User (No Invitation)

This is DIFFERENT from InviteUserHandler:
- **InviteUserHandler**: Creates user with `PENDING_PAYMENT` status + invitation token (user must accept)
- **AddNewUserHandler**: Creates user with `ACTIVE` status directly (no invitation needed)

---

**File: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/usermanagement/AddNewUserHandler.java`**

```java
package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.Password;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Add New Active User
 * 
 * Creates user with ACTIVE status directly (no invitation workflow).
 * Used by admins to create users with immediate access.
 * 
 * Uses EXISTING User domain model.
 */
@Service
@RequiredArgsConstructor
public class AddNewUserHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Command {
        Long bankId;
        String email;
        String firstName;
        String lastName;
        String password;        // Plain text password (will be validated and hashed)
        String roleName;        // e.g., "BANK_ADMIN", "DATA_ANALYST", "VIEWER"
        String createdBy;       // Admin username
    }
    
    @Transactional
    public Result<User> handle(Command command) {
        // 1. Validate email
        var emailResult = Email.create(command.email);
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        Email email = emailResult.getValue().get();
        
        // 2. Check if user already exists in this bank
        if (userRepository.existsByEmailAndBankId(email, command.bankId)) {
            return Result.failure(
                "User with email " + command.email + " already exists in this bank"
            );
        }
        
        // 3. Validate password strength (using existing Password value object)
        var passwordValidation = Password.validateStrength(command.password);
        if (passwordValidation.isFailure()) {
            return Result.failure(passwordValidation.getError().get());
        }
        
        // 4. Hash password (in real implementation, use PasswordHasher service)
        // For now, using Password.create as placeholder
        var passwordResult = Password.create(command.password);
        if (passwordResult.isFailure()) {
            return Result.failure(passwordResult.getError().get());
        }
        
        // 5. Create user using EXISTING factory method
        User user = User.create(
            email,
            passwordResult.getValue().get(),
            command.firstName,
            command.lastName
        );
        
        // 6. Assign to bank (adds BankAssignment)
        user.assignToBank(command.bankId.toString(), command.roleName);
        
        // 7. Activate user immediately (change status from PENDING_PAYMENT to ACTIVE)
        user.activate();
        
        // 8. Save user
        var saveResult = userRepository.userSaver(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 9. TODO: Send welcome email with credentials
        
        return Result.success(user);
    }
}
```

---

## Request DTO

**File: `regtech-iam/presentation/src/main/java/com/bcbs239/regtech/iam/presentation/usermanagement/AddUserRequest.java`**

```java
package com.bcbs239.regtech.iam.presentation.usermanagement;

import jakarta.validation.constraints.*;

/**
 * Request DTO for adding a new active user (no invitation)
 */
public record AddUserRequest(
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    String lastName,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    String password,
    
    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "VIEWER|DATA_ANALYST|AUDITOR|RISK_MANAGER|COMPLIANCE_OFFICER|BANK_ADMIN|HOLDING_COMPANY_USER|SYSTEM_ADMIN",
        message = "Invalid role"
    )
    String role
) {}
```

---

## Update Controller - Add New Endpoint

**File: `regtech-iam/presentation/src/main/java/com/bcbs239/regtech/iam/presentation/usermanagement/UserManagementController.java`**

```java
package com.bcbs239.regtech.iam.presentation.usermanagement;

import com.bcbs239.regtech.iam.application.usermanagement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * User Management Routes
 * Uses EXISTING User domain model
 */
@Configuration
@RequiredArgsConstructor
public class UserManagementController {
    
    private final GetUsersByBankHandler getUsersByBankHandler;
    private final AddNewUserHandler addNewUserHandler;           // NEW
    private final InviteUserHandler inviteUserHandler;
    private final UpdateUserRoleHandler updateUserRoleHandler;
    private final SuspendUserHandler suspendUserHandler;
    private final RevokeInvitationHandler revokeInvitationHandler;
    
    @Bean
    public RouterFunction<ServerResponse> userManagementRoutes() {
        return route()
            // List users
            .GET("/api/v1/banks/{bankId}/users", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::getUsers)
            
            // Add new ACTIVE user (no invitation)
            .POST("/api/v1/banks/{bankId}/users", 
                  accept(MediaType.APPLICATION_JSON), 
                  this::addNewUser)
            
            // Invite user (creates PENDING user with invitation token)
            .POST("/api/v1/banks/{bankId}/users/invite", 
                  accept(MediaType.APPLICATION_JSON), 
                  this::inviteUser)
            
            // Update role
            .PUT("/api/v1/users/{userId}/role", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::updateUserRole)
            
            // Suspend user
            .PUT("/api/v1/users/{userId}/suspend", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::suspendUser)
            
            // Revoke invitation
            .DELETE("/api/v1/users/{userId}/invitation", 
                    accept(MediaType.APPLICATION_JSON), 
                    this::revokeInvitation)
            .build();
    }
    
    /**
     * GET /api/v1/banks/{bankId}/users?filter=all|active|pending
     */
    private ServerResponse getUsers(org.springframework.web.servlet.function.ServerRequest request) {
        Long bankId = Long.parseLong(request.pathVariable("bankId"));
        String filter = request.param("filter").orElse("all");
        
        var query = new GetUsersByBankHandler.Query(bankId, filter);
        var result = getUsersByBankHandler.handle(query);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        var users = result.getValue();
        var response = users.stream().map(UserResponse::from).toList();
        
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "users", response,
                "total", response.size()
            ));
    }
    
    /**
     * POST /api/v1/banks/{bankId}/users
     * Create new ACTIVE user directly (no invitation)
     */
    private ServerResponse addNewUser(org.springframework.web.servlet.function.ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            var req = request.body(AddUserRequest.class);
            
            // TODO: Get current admin username from security context
            String currentAdmin = "Admin";
            
            var command = new AddNewUserHandler.Command(
                bankId,
                req.email(),
                req.firstName(),
                req.lastName(),
                req.password(),
                req.role(),
                currentAdmin
            );
            
            var result = addNewUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get().getMessage()));
            }
            
            return ServerResponse.status(201) // 201 Created
                .contentType(MediaType.APPLICATION_JSON)
                .body(UserResponse.from(result.getValue()));
                
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                .body(Map.of("error", "Invalid bank ID"));
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/v1/banks/{bankId}/users/invite
     * Invite user (creates PENDING user with invitation token)
     */
    private ServerResponse inviteUser(org.springframework.web.servlet.function.ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            var req = request.body(InviteUserRequest.class);
            
            var command = new InviteUserHandler.Command(
                bankId,
                req.email(),
                req.firstName(),
                req.lastName(),
                "Admin" // TODO: Get from security context
            );
            
            var result = inviteUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .body(Map.of("error", result.getError().get().getMessage()));
            }
            
            return ServerResponse.status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UserResponse.from(result.getValue()));
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/v1/users/{userId}/role
     */
    private ServerResponse updateUserRole(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        // TODO: Parse request body for role update
        
        return ServerResponse.noContent().build();
    }
    
    /**
     * PUT /api/v1/users/{userId}/suspend
     */
    private ServerResponse suspendUser(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        var command = new SuspendUserHandler.Command(userId, "Admin");
        var result = suspendUserHandler.handle(command);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        return ServerResponse.ok()
            .body(UserResponse.from(result.getValue()));
    }
    
    /**
     * DELETE /api/v1/users/{userId}/invitation
     */
    private ServerResponse revokeInvitation(org.springframework.web.servlet.function.ServerRequest request) {
        String userId = request.pathVariable("userId");
        
        var command = new RevokeInvitationHandler.Command(userId);
        var result = revokeInvitationHandler.handle(command);
        
        if (result.isFailure()) {
            return ServerResponse.badRequest()
                .body(Map.of("error", result.getError().get().getMessage()));
        }
        
        return ServerResponse.noContent().build();
    }
}
```

---

## API Endpoints Summary

| Method | Endpoint | Purpose | Status After Creation |
|--------|----------|---------|----------------------|
| POST | `/api/v1/banks/{bankId}/users` | **Add new ACTIVE user** | ACTIVE (immediate access) |
| POST | `/api/v1/banks/{bankId}/users/invite` | **Invite user** | PENDING_PAYMENT (needs to accept) |
| GET | `/api/v1/banks/{bankId}/users` | List users | - |
| PUT | `/api/v1/users/{userId}/role` | Update role | - |
| PUT | `/api/v1/users/{userId}/suspend` | Suspend user | SUSPENDED |
| DELETE | `/api/v1/users/{userId}/invitation` | Revoke invitation | User deleted |

---

## Request/Response Examples

### Add New Active User

**Request:**
```bash
POST /api/v1/banks/1/users
Content-Type: application/json

{
  "email": "new.user@bancaitaliana.it",
  "firstName": "Mario",
  "lastName": "Bianchi",
  "password": "SecurePass123!",
  "role": "DATA_ANALYST"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid-here",
  "email": "new.user@bancaitaliana.it",
  "firstName": "Mario",
  "lastName": "Bianchi",
  "fullName": "Mario Bianchi",
  "initials": "MB",
  "status": "ACTIVE",
  "lastAccess": "2025-01-05T10:00:00Z",
  "hasRecentActivity": true
}
```

### Invite User (Different!)

**Request:**
```bash
POST /api/v1/banks/1/users/invite
Content-Type: application/json

{
  "email": "invited.user@bancaitaliana.it",
  "firstName": "Giuseppe",
  "lastName": "Verdi"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid-here",
  "email": "invited.user@bancaitaliana.it",
  "firstName": "Giuseppe",
  "lastName": "Verdi",
  "fullName": "Giuseppe Verdi",
  "initials": "GV",
  "status": "PENDING_PAYMENT",
  "lastAccess": null,
  "hasRecentActivity": false
}
```

---

## Updated Checklist

### ✅ Application Handlers
- [ ] GetUsersByBankHandler
- [ ] **AddNewUserHandler** ← NEW (creates ACTIVE user)
- [ ] InviteUserHandler (creates PENDING user)
- [ ] UpdateUserRoleHandler
- [ ] SuspendUserHandler
- [ ] RevokeInvitationHandler

### ✅ Presentation DTOs
- [ ] UserResponse
- [ ] **AddUserRequest** ← NEW
- [ ] InviteUserRequest

### ✅ Controller Routes
- [ ] GET `/api/v1/banks/{bankId}/users`
- [ ] **POST `/api/v1/banks/{bankId}/users`** ← NEW
- [ ] POST `/api/v1/banks/{bankId}/users/invite`
- [ ] PUT `/api/v1/users/{userId}/role`
- [ ] PUT `/api/v1/users/{userId}/suspend`
- [ ] DELETE `/api/v1/users/{userId}/invitation`

---

## Key Differences: Add vs Invite

| Feature | Add New User | Invite User |
|---------|--------------|-------------|
| **Endpoint** | `POST /banks/{id}/users` | `POST /banks/{id}/users/invite` |
| **Status** | ACTIVE | PENDING_PAYMENT |
| **Password** | Provided by admin | Auto-generated temporary |
| **Access** | Immediate | After accepting invitation |
| **Token** | None | invitation_token generated |
| **Email** | Welcome email | Invitation email |
| **Use Case** | Admin creates user with known credentials | User will set their own password later |

Both handlers use the EXISTING `User` domain model - no duplicates! 🎯