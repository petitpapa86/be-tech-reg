# Registration and Authentication Workflows

## 1. User Registration Workflow

```mermaid
sequenceDiagram
    participant Client as Web Client
    participant LB as GCP Load Balancer
    participant App as RegTech App
    participant IAM as IAM Module
    participant AuthSvc as Authorization Service
    participant Billing as Billing Module
    participant DB as Database
    participant Events as Event Bus

    Note over Client, Events: User Registration Flow

    Client->>LB: POST /api/v1/users/register
    Note right of Client: {email, password, firstName, lastName}
    
    LB->>App: Route to application
    App->>IAM: UserController.registerUser()
    
    Note over IAM: Security Filter Chain (Order 1)
    IAM->>IAM: Check endpoint permissions
    Note right of IAM: /api/v1/users/register is permitAll()
    
    IAM->>IAM: RegisterUserCommandHandler.handle()
    
    Note over IAM: Validation & Business Logic
    IAM->>IAM: Validate email format
    IAM->>IAM: Check password strength
    IAM->>DB: Check if email exists
    DB-->>IAM: Email availability
    
    alt Email already exists
        IAM-->>Client: 409 Conflict - Email exists
    else Email available
        IAM->>IAM: Create User entity
        IAM->>IAM: Hash password
        IAM->>DB: Save user (status: PENDING_PAYMENT)
        DB-->>IAM: User saved with ID
        
        Note over IAM: Assign Default Role
        IAM->>IAM: Create UserRole (Role.USER)
        IAM->>DB: Save user role
        
        Note over IAM: Publish Domain Event
        IAM->>Events: Publish UserRegisteredEvent
        Note right of Events: {userId, email, correlationId}
        
        IAM-->>Client: 201 Created - Registration successful
        Note right of Client: {userId, correlationId}
    end
    
    Note over Events, Billing: Async Event Processing
    Events->>Billing: UserRegisteredEventHandler
    Billing->>Billing: Create billing account
    Billing->>DB: Save billing account (status: PENDING)
    Billing->>Events: Publish BillingAccountCreatedEvent
```

## 2. User Authentication Workflow

```mermaid
sequenceDiagram
    participant Client as Web Client
    participant LB as GCP Load Balancer
    participant App as RegTech App
    participant IAM as IAM Module
    participant AuthSvc as Authorization Service
    participant DB as Database
    participant JWT as JWT Service

    Note over Client, JWT: User Authentication Flow

    Client->>LB: POST /api/v1/auth/login
    Note right of Client: {email, password}
    
    LB->>App: Route to application
    App->>IAM: AuthController.login()
    
    Note over IAM: Security Filter Chain (Order 1)
    IAM->>IAM: Check endpoint permissions
    Note right of IAM: /api/v1/auth/login is permitAll()
    
    IAM->>IAM: AuthenticateUserCommandHandler.handle()
    
    Note over IAM: Authentication Logic
    IAM->>DB: Find user by email
    DB-->>IAM: User entity
    
    alt User not found
        IAM-->>Client: 401 Unauthorized - Invalid credentials
    else User found
        IAM->>IAM: Verify password hash
        
        alt Password invalid
            IAM-->>Client: 401 Unauthorized - Invalid credentials
        else Password valid
            Note over IAM: Load User Permissions
            IAM->>DB: Get user roles
            DB-->>IAM: List<UserRole>
            
            IAM->>IAM: Extract permissions from roles
            Note right of IAM: Role.USER -> [user:read, billing:read, etc.]
            
            Note over IAM: Generate JWT Token
            IAM->>JWT: Create JWT with user info & permissions
            Note right of JWT: {userId, email, roles, permissions, exp}
            JWT-->>IAM: JWT token
            
            IAM-->>Client: 200 OK - Login successful
            Note right of Client: {token, userId, roles, expiresIn}
        end
    end
```

## 3. Authorized Request Workflow (Billing Example)

```mermaid
sequenceDiagram
    participant Client as Web Client
    participant LB as GCP Load Balancer
    participant App as RegTech App
    participant Security as Security Filter
    participant AuthSvc as Authorization Service
    participant Billing as Billing Module
    participant IAM as IAM Module
    participant DB as Database

    Note over Client, DB: Authorized Billing Request Flow

    Client->>LB: POST /api/v1/billing/process-payment
    Note right of Client: Authorization: Bearer <JWT>
    
    LB->>App: Route to application
    App->>Security: Security Filter Chain
    
    Note over Security: JWT Validation
    Security->>Security: Extract JWT from header
    Security->>Security: Validate JWT signature
    Security->>Security: Check expiration
    Security->>Security: Extract user info & permissions
    
    alt JWT invalid/expired
        Security-->>Client: 401 Unauthorized - Invalid token
    else JWT valid
        Note over Security: Set Security Context
        Security->>Security: Create Authentication object
        Security->>Security: Set permissions as authorities
        
        Note over Security: Check Endpoint Permissions
        Security->>Security: Match /api/v1/billing/** pattern
        Security->>Security: Check hasAuthority(BILLING_PROCESS_PAYMENT)
        
        alt Insufficient permissions
            Security-->>Client: 403 Forbidden - Insufficient permissions
        else Has permission
            Security->>Billing: BillingController.processPayment()
            
            Note over Billing: Additional Business Logic Checks
            Billing->>AuthSvc: Check additional permissions
            AuthSvc->>IAM: IamAuthorizationService.hasPermission()
            IAM->>DB: Verify current user permissions
            DB-->>IAM: User permissions
            IAM-->>AuthSvc: Permission check result
            AuthSvc-->>Billing: Authorization result
            
            alt Business rule violation
                Billing-->>Client: 403 Forbidden - Business rule violation
            else All checks pass
                Billing->>Billing: Process payment logic
                Billing->>DB: Save payment record
                DB-->>Billing: Payment saved
                
                Billing-->>Client: 200 OK - Payment processed
                Note right of Client: {paymentId, status, amount}
            end
        end
    end
```

## 4. Cross-Module Permission Check Workflow

```mermaid
sequenceDiagram
    participant Report as Reporting Module
    participant AuthSvc as Authorization Service
    participant IAM as IAM Module
    participant Cache as Permission Cache
    participant DB as Database

    Note over Report, DB: Cross-Module Permission Check

    Report->>AuthSvc: hasPermission("report:view")
    
    Note over AuthSvc: Check Current User
    AuthSvc->>AuthSvc: Get current user from SecurityContext
    
    alt User not authenticated
        AuthSvc-->>Report: false (no permission)
    else User authenticated
        Note over AuthSvc: Check Cache First
        AuthSvc->>Cache: Get cached permissions for user
        
        alt Cache hit
            Cache-->>AuthSvc: Cached permissions
            AuthSvc->>AuthSvc: Check if "report:view" in permissions
            AuthSvc-->>Report: true/false
        else Cache miss
            Note over AuthSvc: Load from IAM
            AuthSvc->>IAM: IamAuthorizationService.getUserPermissions()
            IAM->>DB: Query user roles
            DB-->>IAM: List<UserRole>
            
            IAM->>IAM: Extract permissions from roles
            Note right of IAM: Role.USER -> Set<Permission>
            IAM-->>AuthSvc: Set<String> permissions
            
            Note over AuthSvc: Cache Results
            AuthSvc->>Cache: Cache permissions (TTL: 5min)
            
            AuthSvc->>AuthSvc: Check if "report:view" in permissions
            AuthSvc-->>Report: true/false
        end
    end
```

## 5. Multi-Tenant Organization Access Workflow

```mermaid
sequenceDiagram
    participant Client as Web Client
    participant Billing as Billing Module
    participant AuthSvc as Authorization Service
    participant IAM as IAM Module
    participant DB as Database

    Note over Client, DB: Multi-Tenant Access Control

    Client->>Billing: GET /api/v1/billing/invoices/org-123
    Note right of Client: Request invoices for organization

    Billing->>AuthSvc: canAccessOrganization("org-123")
    
    Note over AuthSvc: Get Current User
    AuthSvc->>AuthSvc: getCurrentUserId()
    
    AuthSvc->>IAM: Check organization access
    IAM->>DB: Query user roles for organization
    Note right of DB: SELECT * FROM user_roles WHERE user_id=? AND organization_id=?
    DB-->>IAM: List<UserRole> for org-123
    
    alt No roles in organization
        IAM-->>AuthSvc: false (no access)
        AuthSvc-->>Billing: false
        Billing-->>Client: 403 Forbidden - No access to organization
    else Has roles in organization
        IAM-->>AuthSvc: true (has access)
        AuthSvc-->>Billing: true
        
        Note over Billing: Check Specific Permission
        Billing->>AuthSvc: hasPermission("billing:view-invoices")
        AuthSvc-->>Billing: true/false
        
        alt No billing permission
            Billing-->>Client: 403 Forbidden - Cannot view invoices
        else Has billing permission
            Billing->>DB: Get invoices for org-123
            DB-->>Billing: List<Invoice>
            Billing-->>Client: 200 OK - Invoice list
        end
    end
```

## 6. Role Assignment Workflow

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant IAM as IAM Module
    participant AuthSvc as Authorization Service
    participant DB as Database
    participant Events as Event Bus
    participant Cache as Permission Cache

    Note over Admin, Cache: Role Assignment Flow

    Admin->>IAM: POST /api/v1/users/admin/assign-role
    Note right of Admin: {userId, role, organizationId}

    Note over IAM: Check Admin Permissions
    IAM->>AuthSvc: hasPermission("user:admin")
    AuthSvc-->>IAM: true/false

    alt Not admin
        IAM-->>Admin: 403 Forbidden - Admin required
    else Is admin
        Note over IAM: Validate Request
        IAM->>DB: Check if user exists
        IAM->>DB: Check if organization exists
        
        Note over IAM: Create Role Assignment
        IAM->>IAM: Create UserRole entity
        IAM->>DB: Save user role
        DB-->>IAM: Role assigned
        
        Note over IAM: Publish Event
        IAM->>Events: Publish UserRoleChangedEvent
        Note right of Events: {userId, role, organizationId, action: ASSIGNED}
        
        Note over Events: Clear Caches
        Events->>Cache: Clear user permissions cache
        Events->>AuthSvc: Notify permission change
        
        IAM-->>Admin: 200 OK - Role assigned successfully
    end
```

## Key Security Features Demonstrated

### 1. **Layered Security**
- GCP Load Balancer (DDoS protection)
- JWT token validation
- Spring Security filter chains
- Business logic authorization

### 2. **Permission-Based Access Control**
- Fine-grained permissions (e.g., `billing:process-payment`)
- Role-based grouping of permissions
- Cross-module permission checking

### 3. **Multi-Tenant Support**
- Organization-based access control
- Users can have different roles per organization
- Data isolation by organization

### 4. **Performance Optimizations**
- Permission caching (5-minute TTL)
- JWT contains permissions (reduces DB calls)
- Async event processing

### 5. **Audit and Monitoring**
- All permission checks are logged
- Role changes trigger events
- Security context tracking

### 6. **Modular Architecture**
- Each module handles its own endpoints
- Shared authorization service
- Clean separation of concerns

This architecture ensures secure, scalable, and maintainable authentication and authorization across all your bounded contexts!