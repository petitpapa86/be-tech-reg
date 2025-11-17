# Authentication System Documentation

## Overview

The RegTech application uses a JWT-based authentication system with role-based access control (RBAC). The system integrates with the IAM domain model to provide secure, permission-based authorization for all endpoints.

## How Authentication Works

### 1. User Login Process

1. **User Authentication**: User provides email/password to `/api/v1/auth/login`
2. **User Lookup**: System finds user by email in database
3. **Password Verification**: Validates password hash
4. **Status Check**: Ensures user account is ACTIVE
5. **JWT Generation**: Creates JWT token containing:
   - `userId`: User's unique identifier
   - `email`: User's email address
   - `banks`: Array of bank assignments with roles
   - `exp`: Token expiration timestamp

### 2. Request Authorization Flow

```
HTTP Request → SecurityFilter → JwtAuthenticationService → UserRepository → RolePermissionService → Permissions
```

1. **SecurityFilter**: Intercepts requests, extracts JWT from Authorization header
2. **Token Validation**: JwtAuthenticationService validates JWT signature and expiration
3. **User Loading**: Loads User entity from database using userId from token
4. **Permission Resolution**: For each bank assignment:
   - Extracts role name from user assignment
   - Uses `RolePermissionService` to load permissions from database
   - Collects role names for context
5. **SecurityContext**: Creates SimpleAuthentication with resolved permissions and roles
6. **Authorization**: PermissionCheckFilter validates required permissions for endpoint

**Note**: System now fully loads roles and permissions from database.

### 3. Permission Checking

The system supports two types of authorization:

- **Role-based**: `hasRole("ADMIN")`
- **Permission-based**: `hasPermission("BCBS239_UPLOAD_FILES")`

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status ENUM('PENDING_PAYMENT', 'ACTIVE', 'SUSPENDED', 'DELETED'),
    google_id VARCHAR(255),
    facebook_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT
);
```

### User Bank Assignments
```sql
CREATE TABLE user_bank_assignments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bank_id VARCHAR(36) NOT NULL,
    role VARCHAR(50) NOT NULL, -- Role name (e.g., 'SYSTEM_ADMIN', 'DATA_ANALYST')
    organization_id VARCHAR(36),
    assigned_at TIMESTAMP,
    assigned_by VARCHAR(36),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (bank_id) REFERENCES banks(id)
);
```

### Roles Table (Future - Database-Driven)
```sql
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL, -- Role identifier (e.g., 'SYSTEM_ADMIN')
    display_name VARCHAR(100) NOT NULL, -- Human-readable name
    description TEXT,
    level INT NOT NULL, -- Hierarchical level (higher = more permissions)
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE role_permissions (
    id VARCHAR(36) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission VARCHAR(100) NOT NULL, -- Permission string (e.g., 'BCBS239_UPLOAD_FILES')
    created_at TIMESTAMP,

    FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE(role_id, permission)
);
```

### Sample Data

#### Users
```sql
INSERT INTO users (id, email, password_hash, first_name, last_name, status) VALUES
('user-1', 'admin@bank.com', '$2a$10$...', 'System', 'Administrator', 'ACTIVE'),
('user-2', 'analyst@bank.com', '$2a$10$...', 'Data', 'Analyst', 'ACTIVE');
```

#### Bank Assignments
```sql
INSERT INTO user_bank_assignments (id, user_id, bank_id, role, organization_id) VALUES
('assign-1', 'user-1', 'bank-1', 'SYSTEM_ADMIN', 'org-1'),
('assign-2', 'user-2', 'bank-1', 'DATA_ANALYST', 'org-1');
```

### Available Roles

Roles are now fully database-driven. The following role names are available:

#### Database Roles
- `SYSTEM_ADMIN` - Full system access and administration
- `COMPLIANCE_OFFICER` - Compliance management and regulatory reporting
- `RISK_MANAGER` - Risk assessment and violation management
- `BANK_ADMIN` - Bank-specific administration
- `DATA_ANALYST` - Data upload, analysis, and reporting
- `VIEWER` - Read-only access to reports and data
- `AUDITOR` - Audit log access and monitoring
- `HOLDING_COMPANY_USER` - Cross-bank data viewing

Roles are stored in the `roles` table with associated permissions in `role_permissions`:

```sql
-- Example role definitions
INSERT INTO roles (id, name, display_name, description, level) VALUES
('role-1', 'SYSTEM_ADMIN', 'System Administrator', 'Full system access', 5),
('role-2', 'DATA_ANALYST', 'Data Analyst', 'Data operations and reporting', 2);

-- Example permissions for SYSTEM_ADMIN
INSERT INTO role_permissions (id, role_id, permission) VALUES
('perm-1', 'role-1', 'BCBS239_UPLOAD_FILES'),
('perm-2', 'role-1', 'BCBS239_MANAGE_USERS'),
('perm-3', 'role-1', 'BCBS239_SYSTEM_CONFIG');
```

## JWT Token Structure

```json
{
  "userId": "user-1",
  "email": "admin@bank.com",
  "banks": [
    {
      "bankId": "bank-1",
      "role": "SYSTEM_ADMIN"
    }
  ],
  "iat": 1731628800,
  "exp": 1731715200
}
```

## Security Filters

### SecurityFilter (Servlet Filter)
- **Location**: `regtech-iam` module
- **Purpose**: Authenticates requests and sets up SecurityContext
- **Integration**: Uses JwtAuthenticationService to validate tokens

### PermissionCheckFilter (WebFlux Filter)
- **Location**: `regtech-app` module (planned)
- **Purpose**: Authorizes requests based on required permissions
- **Integration**: Checks SecurityContext for permissions

## API Endpoints

### Authentication Endpoints
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - Token invalidation

### Protected Endpoints
All business endpoints require authentication and appropriate permissions:

```java
// Example endpoint protection
@PreAuthorize("hasPermission('BCBS239_UPLOAD_FILES')")
@PostMapping("/upload")
public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {
    // Implementation
}
```

## Error Handling

### Authentication Errors
- `INVALID_CREDENTIALS`: Wrong email/password
- `USER_NOT_ACTIVE`: Account suspended
- `TOKEN_EXPIRED`: JWT expired
- `TOKEN_INVALID`: Invalid JWT signature

### Authorization Errors
- `INSUFFICIENT_PERMISSIONS`: User lacks required permissions
- `ACCESS_DENIED`: General access denial

## Security Best Practices

1. **Token Expiration**: JWTs expire after 24 hours
2. **Password Hashing**: BCrypt with salt
3. **Role Hierarchy**: Higher-level roles include lower-level permissions
4. **Bank Isolation**: Users can only access assigned banks
5. **Audit Logging**: All authentication/authorization events logged

## Migration Notes

When upgrading from previous security implementations:

1. Ensure all users have proper bank assignments
2. Update role strings to match database role names
3. Verify permission mappings in database tables
4. Test authentication flow with real user data

### Database-Driven Roles Migration

✅ Migration to database-driven roles is complete:

1. ✅ `Bcbs239Role` enum removed
2. ✅ Database role and permission tables implemented
3. ✅ `RolePermissionService` loads from database
4. ✅ Enum dependency removed
5. ✅ Role assignments use role IDs

**Benefits of Database-Driven Roles:**
- Roles can be modified without code changes
- Dynamic permission assignment
- Easier role management through UI
- Audit trail for role changes
- Flexible permission combinations</content>
<parameter name="filePath">C:\Users\alseny\Desktop\react projects\regtech\AUTHENTICATION_GUIDE.md