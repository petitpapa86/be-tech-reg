# Architecture Violations & Refactoring Guide

> **Critical issues found in the current codebase that violate Clean Architecture and Module Independence principles.**

---

## Executive Summary

The following architecture violations have been identified and must be corrected:

1. ❌ **Infrastructure depends on Application** (violates Clean Architecture)
2. ❌ **Module-to-Module dependencies** (violates Module Independence)
3. ❌ **Domain layer depends on other module domains** (violates Bounded Context)

---

## Violation 1: Infrastructure → Application Dependency

### Problem

**Location**: All module infrastructure layers

**Current State**:
```xml
<!-- regtech-billing/infrastructure/pom.xml -->
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-billing-application</artifactId>  <!-- ❌ WRONG -->
</dependency>
```

**Why This is Wrong**:
- Infrastructure should only depend on Domain, NOT Application
- Violates Dependency Inversion Principle
- Prevents proper layer isolation
- Makes it impossible to swap infrastructure implementations

### Solution

**Remove Application dependency from Infrastructure**:

```xml
<!-- regtech-billing/infrastructure/pom.xml -->
<dependencies>
    <!-- ✅ CORRECT: Only depend on Domain -->
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-domain</artifactId>
    </dependency>
    
    <!-- ❌ REMOVE THIS -->
    <!-- 
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-application</artifactId>
    </dependency>
    -->
    
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-infrastructure</artifactId>
    </dependency>
</dependencies>
```

### Refactoring Steps

1. **Identify Infrastructure classes using Application layer**:
```powershell
# Search for imports from application layer in infrastructure
grep -r "import.*application" regtech-billing/infrastructure/src/
```

2. **Move interfaces to Domain layer**:
   - If Infrastructure implements interfaces, they should be in Domain
   - Example: `IUserRepository` should be in `regtech-iam-domain`

3. **Use Domain interfaces only**:
```java
// ❌ BEFORE (Infrastructure using Application)
import com.bcbs239.regtech.billing.application.SomeApplicationService;

// ✅ AFTER (Infrastructure using Domain interfaces)
import com.bcbs239.regtech.billing.domain.repositories.ISubscriptionRepository;
```

4. **Remove dependency from POM files**:
   - `regtech-billing/infrastructure/pom.xml`
   - `regtech-iam/infrastructure/pom.xml`
   - `regtech-data-quality/infrastructure/pom.xml`
   - `regtech-ingestion/infrastructure/pom.xml`

---

## Violation 2: Module-to-Module Dependencies

### Problem

**Location**: `regtech-billing` module depends on `regtech-iam`

**Current State**:
```xml
<!-- regtech-billing/infrastructure/pom.xml -->
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-iam</artifactId>  <!-- ❌ Module-to-Module dependency -->
    <version>${project.version}</version>
</dependency>
```

**Why This is Wrong**:
- Modules should be independent bounded contexts
- Prevents extracting modules to microservices
- Creates tight coupling between business domains
- Violates Domain-Driven Design principles

### Solution

**Replace direct dependencies with Integration Events**:

#### Step 1: Remove Module Dependency

```xml
<!-- regtech-billing/infrastructure/pom.xml -->
<!-- ❌ REMOVE THIS -->
<!-- 
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-iam</artifactId>
    <version>${project.version}</version>
</dependency>
-->
```

#### Step 2: Define Integration Event in IAM Module

```java
// regtech-iam/domain/src/.../events/UserRegisteredEvent.java
public class UserRegisteredEvent extends IntegrationEvent {
    private final String userId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final Instant registeredAt;
    
    public UserRegisteredEvent(
        String correlationId,
        String userId,
        String email,
        String firstName,
        String lastName
    ) {
        super(correlationId, Maybe.none(), "UserRegistered");
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registeredAt = Instant.now();
    }
    
    @Override
    public String eventType() {
        return "UserRegistered";
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Instant getRegisteredAt() { return registeredAt; }
}
```

#### Step 3: Publish Event in IAM Module

```java
// regtech-iam/application/.../RegisterUserCommandHandler.java
@Component
public class RegisterUserCommandHandler {
    private final IUserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;
    
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // 1. Create user
        User user = User.create(
            command.getEmail(),
            command.getPassword(),
            command.getFirstName(),
            command.getLastName()
        );
        
        // 2. User aggregate raises UserRegisteredEvent domain event
        // This is done internally in User.create() method
        
        // 3. Save user
        Result<UserId> saveResult = userRepository.save(user);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 4. Register entity with unit of work (collects domain events)
        unitOfWork.registerEntity(user);
        
        // 5. Save changes (events persisted to outbox)
        unitOfWork.saveChanges();
        
        return Result.success(new RegisterUserResponse(user.getId().getValue()));
    }
}
```

#### Step 4: Consume Event in Billing Module

```java
// regtech-billing/application/.../UserEventHandler.java
@Component
public class UserEventHandler {
    private final IBillingAccountRepository billingAccountRepository;
    private final BaseUnitOfWork unitOfWork;
    private final ILogger logger;
    
    public UserEventHandler(
        IBillingAccountRepository billingAccountRepository,
        BaseUnitOfWork unitOfWork,
        ILogger logger
    ) {
        this.billingAccountRepository = billingAccountRepository;
        this.unitOfWork = unitOfWork;
        this.logger = logger;
    }
    
    @EventListener
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        logger.asyncStructuredLog("USER_REGISTERED_EVENT_RECEIVED", Map.of(
            "userId", event.getUserId(),
            "email", event.getEmail(),
            "correlationId", event.getCorrelationId()
        ));
        
        // Create billing account for new user
        BillingAccount billingAccount = BillingAccount.createForUser(
            UserId.of(event.getUserId()),
            Email.of(event.getEmail()),
            SubscriptionTier.FREE  // Default tier
        );
        
        // Save billing account
        Result<BillingAccountId> saveResult = billingAccountRepository.save(billingAccount);
        if (saveResult.isFailure()) {
            logger.asyncStructuredErrorLog("BILLING_ACCOUNT_CREATION_FAILED", null, Map.of(
                "userId", event.getUserId(),
                "error", saveResult.getError().toString()
            ));
            return;
        }
        
        // Register with unit of work
        unitOfWork.registerEntity(billingAccount);
        unitOfWork.saveChanges();
        
        logger.asyncStructuredLog("BILLING_ACCOUNT_CREATED_FOR_USER", Map.of(
            "userId", event.getUserId(),
            "billingAccountId", billingAccount.getId().getValue()
        ));
    }
}
```

### Refactoring Steps

1. **Find all cross-module imports**:
```powershell
# In billing module, find IAM imports
grep -r "import.*regtech.iam" regtech-billing/
```

2. **Identify what data is needed**:
   - What information does Billing need from IAM?
   - Create Integration Events with that data

3. **Replace direct calls with event handlers**:
   - Remove direct method calls
   - Subscribe to integration events
   - Process events asynchronously

4. **Remove POM dependencies**:
   - Remove `regtech-iam` from `regtech-billing` POMs
   - Ensure only `regtech-core` is referenced

---

## Violation 3: Domain-to-Domain Dependencies

### Problem

**Location**: `regtech-billing-domain` depends on `regtech-iam-domain`

**Current State**:
```xml
<!-- regtech-billing/domain/pom.xml -->
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-iam-domain</artifactId>  <!-- ❌ Domain-to-Domain dependency -->
    <version>${project.version}</version>
</dependency>
```

**Why This is Wrong**:
- Domain layers should be completely independent
- Violates Bounded Context principle
- Each module should have its own domain model
- Prevents independent evolution of domains

### Solution

**Create module-specific domain concepts**:

#### Instead of sharing domain objects:

```java
// ❌ WRONG - Billing domain using IAM domain object
import com.bcbs239.regtech.iam.domain.User;

public class BillingAccount {
    private User user;  // ❌ Direct reference to another domain
}
```

#### Use Value Objects with only IDs:

```java
// ✅ CORRECT - Billing domain with its own UserId value object
// regtech-billing/domain/.../valueobjects/UserId.java
public record UserId(String value) {
    public static UserId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or blank");
        }
        return new UserId(value);
    }
}

// regtech-billing/domain/.../aggregates/BillingAccount.java
public class BillingAccount extends AggregateRoot {
    private BillingAccountId id;
    private UserId userId;  // ✅ Only reference by ID
    private Email email;    // ✅ Own value object copy
    private SubscriptionTier tier;
    
    public static BillingAccount createForUser(
        UserId userId,
        Email email,
        SubscriptionTier tier
    ) {
        BillingAccount account = new BillingAccount();
        account.id = BillingAccountId.generate();
        account.userId = userId;
        account.email = email;
        account.tier = tier;
        account.status = BillingAccountStatus.PENDING;
        
        return account;
    }
}
```

### Refactoring Steps

1. **Audit domain dependencies**:
```powershell
# Check all domain POMs
grep -A5 "regtech-.*-domain" regtech-*/domain/pom.xml
```

2. **Identify shared concepts**:
   - Which domain objects are being shared?
   - Why are they shared?

3. **Create module-specific value objects**:
   - Copy value objects to each module that needs them
   - Each module owns its own domain model

4. **Use only IDs for cross-module references**:
   - Replace object references with ID value objects
   - Example: `UserId`, `SubscriptionId`, `InvoiceId`

5. **Remove domain-to-domain dependencies**:
   - Remove all `regtech-*-domain` dependencies except `regtech-core-domain`

---

## Corrected Dependency Structure

### Correct POM Dependencies

#### Module Parent POM
```xml
<!-- regtech-billing/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core</artifactId>  <!-- ✅ Only core -->
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

#### Domain Layer POM
```xml
<!-- regtech-billing/domain/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-domain</artifactId>  <!-- ✅ Only core-domain -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### Application Layer POM
```xml
<!-- regtech-billing/application/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-domain</artifactId>  <!-- ✅ Own domain -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-application</artifactId>  <!-- ✅ Core application -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

#### Infrastructure Layer POM
```xml
<!-- regtech-billing/infrastructure/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-domain</artifactId>  <!-- ✅ Only domain -->
        <version>${project.version}</version>
    </dependency>
    <!-- ❌ NO APPLICATION DEPENDENCY -->
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-infrastructure</artifactId>  <!-- ✅ Core infra -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>
```

#### Presentation Layer POM
```xml
<!-- regtech-billing/presentation/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-domain</artifactId>  <!-- ✅ Domain -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-application</artifactId>  <!-- ✅ Application -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-billing-infrastructure</artifactId>  <!-- ✅ Infrastructure -->
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

---

## Refactoring Checklist

### Phase 1: Audit Current State
- [ ] List all module-to-module dependencies
- [ ] List all infrastructure → application dependencies
- [ ] List all domain-to-domain dependencies
- [ ] Document why each dependency exists

### Phase 2: Remove Infrastructure → Application
- [ ] Move interfaces from Application to Domain
- [ ] Update Infrastructure to use Domain interfaces only
- [ ] Remove Application dependency from Infrastructure POMs
- [ ] Run tests to ensure nothing breaks

### Phase 3: Remove Module-to-Module Dependencies
- [ ] Identify cross-module communication patterns
- [ ] Create Integration Events for each pattern
- [ ] Implement Event Handlers in consuming modules
- [ ] Remove module dependencies from POMs
- [ ] Test event-driven communication

### Phase 4: Remove Domain-to-Domain Dependencies
- [ ] Create module-specific Value Objects
- [ ] Replace object references with ID references
- [ ] Copy shared concepts to each module
- [ ] Remove domain dependencies from POMs
- [ ] Verify bounded context isolation

### Phase 5: Verify Architecture
- [ ] Run dependency analyzer: `mvn dependency:tree`
- [ ] Verify no circular dependencies
- [ ] Ensure modules can be built independently
- [ ] Run full test suite
- [ ] Update architecture documentation

---

## Benefits of Correcting These Violations

### 1. **Clean Architecture Compliance**
- Proper dependency flow (outward)
- Easy to test each layer
- Easy to swap implementations

### 2. **Module Independence**
- Can extract to microservices
- Teams can work independently
- Deploy modules separately

### 3. **Domain Integrity**
- Clear bounded contexts
- Independent domain evolution
- No cross-contamination

### 4. **Maintainability**
- Changes isolated to modules
- Easier to understand
- Reduced cognitive load

### 5. **Scalability**
- Horizontal scaling per module
- Different databases per module
- Event-driven scaling

---

## Next Steps

1. **Create feature branch**: `git checkout -b refactor/fix-architecture-violations`
2. **Start with Phase 1**: Audit and document
3. **Fix one violation type at a time**: Infrastructure → Application first
4. **Test after each change**: Ensure nothing breaks
5. **Update documentation**: Keep IMPLEMENTATION_GUIDE.md current
6. **Review and merge**: Get team approval

---

## References

- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Updated architecture guide
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Microservices Patterns by Chris Richardson](https://microservices.io/patterns/index.html)
