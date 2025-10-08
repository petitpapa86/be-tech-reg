# Modular Security Architecture for RegTech Application

## Problem Statement

The original implementation had billing-specific security configurations in the IAM bounded context, which violated Domain-Driven Design principles and created tight coupling between modules.

## Solution: Modular Security Architecture

### Option 1: Centralized Security in Core Module (Recommended)

**Benefits:**
- Single place to manage security configuration
- Consistent security policies across all modules
- Easy to maintain and audit
- Works well for modular monoliths

**Implementation:**
```
regtech-core/
└── security/
    ├── ModularSecurityConfiguration.java    # Main security config
    ├── SecurityConfigurationRegistry.java   # Registry pattern
    └── filters/                            # Shared security filters
```

### Option 2: Module-Specific Security (Alternative)

**Benefits:**
- Each module owns its security concerns
- Better separation of concerns
- Easier to extract modules later

**Implementation:**
```
regtech-[module]/
└── infrastructure/
    └── security/
        ├── [Module]SecurityConfiguration.java
        └── filters/                        # Module-specific filters
```

## Current Implementation

We've implemented **Option 1** with module-specific configurations that register with the core:

### Core Security Configuration
- `ModularSecurityConfiguration.java` - Main security setup with multiple filter chains
- `SecurityConfigurationRegistry.java` - Registry pattern for module configurations

### Module Security Configurations
- **IAM Module**: Handles user authentication and authorization
- **Billing Module**: Handles payment security, webhooks, and rate limiting

## Security Filter Chain Order

1. **IAM Security** (Order 1): `/api/v1/users/**`, `/api/v1/auth/**`
2. **Billing Security** (Order 2): `/api/v1/billing/**`, `/api/v1/subscriptions/**`
3. **Base Security** (Order 100): Catches all other endpoints

## Key Security Features by Module

### IAM Module Security
- User registration (public)
- Authentication endpoints (public)
- User profile management (authenticated)
- User administration (admin role)

### Billing Module Security
- Webhook signature validation (Stripe)
- Rate limiting for payment endpoints
- Payment processing (authenticated)
- Subscription management (authenticated)
- Monitoring endpoints (admin role)

## GCP Deployment Considerations

For GCP deployment, this architecture provides several advantages:

### 1. Cloud Load Balancer Integration
```yaml
# No need for API Gateway - use GCP Load Balancer
gcp:
  load_balancer:
    backend_services:
      - name: regtech-app
        health_check: /actuator/health
        security: 
          - cloud_armor_policy: regtech-security-policy
```

### 2. Cloud Armor for DDoS Protection
```yaml
cloud_armor:
  security_policy:
    - rule: rate_limit_billing
      expression: "request.path.startsWith('/api/v1/billing/')"
      rate_limit: 100/minute
    - rule: rate_limit_payments
      expression: "request.path.startsWith('/api/v1/billing/process-payment')"
      rate_limit: 10/minute
```

### 3. Identity-Aware Proxy (IAP)
```yaml
iap:
  enabled: true
  oauth_client_id: ${GCP_OAUTH_CLIENT_ID}
  allowed_domains:
    - your-company.com
```

## Migration Path

### Phase 1: Current State ✅
- Move billing security from IAM to billing module
- Implement modular security configuration
- Clean up cross-module dependencies

### Phase 2: Enhanced Security
- Add JWT token validation
- Implement OAuth2/OIDC integration
- Add audit logging

### Phase 3: Cloud-Native Security
- Integrate with GCP IAM
- Use Cloud KMS for secrets
- Implement Workload Identity

## Configuration Examples

### Application Properties
```yaml
# Core security settings
regtech:
  security:
    enabled: true
    cors:
      allowed_origins: "*"
      allowed_methods: "GET,POST,PUT,DELETE,OPTIONS"
    
# Module-specific settings
billing:
  security:
    rate_limit:
      payment:
        requests_per_minute: 10
      webhook:
        requests_per_minute: 100
  webhook:
    stripe:
      endpoint_secret: ${STRIPE_WEBHOOK_SECRET}

iam:
  security:
    password:
      min_length: 8
      require_special_chars: true
    jwt:
      secret: ${JWT_SECRET}
      expiration: 3600
```

### Environment Variables
```bash
# Stripe webhook security
STRIPE_WEBHOOK_SECRET=whsec_...

# JWT configuration
JWT_SECRET=your-jwt-secret
JWT_EXPIRATION=3600

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/regtech
DATABASE_USERNAME=regtech_user
DATABASE_PASSWORD=secure_password
```

## Benefits of This Architecture

1. **Separation of Concerns**: Each module handles its own security
2. **Maintainability**: Clear ownership and boundaries
3. **Scalability**: Easy to add new modules with their own security
4. **Testability**: Module security can be tested independently
5. **Cloud-Ready**: Works well with GCP security services
6. **No API Gateway Needed**: Built-in security handling reduces complexity

## Alternative: API Gateway Approach

If you later decide you need an API Gateway, consider:

### GCP API Gateway
```yaml
# api-gateway.yaml
swagger: '2.0'
info:
  title: RegTech API
  version: 1.0.0
host: api.regtech.com
schemes:
  - https
security:
  - api_key: []
paths:
  /api/v1/billing/**:
    x-google-backend:
      address: https://regtech-app.run.app
      path_translation: APPEND_PATH_TO_ADDRESS
```

### Kong or Istio Service Mesh
- More complex but provides advanced features
- Better for microservices architecture
- Overkill for current modular monolith

## Recommendation

Stick with the **modular security approach** for now because:
1. Simpler to maintain and deploy
2. Better performance (no extra network hop)
3. Easier to debug and monitor
4. Cost-effective on GCP
5. Can always add API Gateway later if needed

The current architecture gives you the flexibility to evolve toward microservices while keeping things simple for the modular monolith approach.