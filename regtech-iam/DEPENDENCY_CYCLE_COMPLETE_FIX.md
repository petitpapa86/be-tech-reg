# IAM Module - Complete Dependency Cycle Fix

## Problem
Maven build was failing with a cyclic dependency error:
```
Edge between 'regtech-iam-infrastructure' and 'regtech-iam-application' 
introduces cycle: application → infrastructure → application
```

## Root Causes
Multiple issues were causing the circular dependency:

1. **Infrastructure → Application dependency**: The infrastructure layer pom.xml incorrectly included a dependency on the application layer
2. **Application → Infrastructure dependency**: The application layer pom.xml incorrectly included a dependency on the infrastructure layer  
3. **Direct infrastructure imports**: Application layer classes were directly importing infrastructure configuration classes (`OAuth2Configuration`)

This created a bidirectional dependency that violates clean architecture principles.

## Clean Architecture Dependency Rules

In clean architecture, dependencies should flow **inward** toward the domain:

```
Presentation → Application → Domain ← Infrastructure
```

### Correct Dependencies:
- **Domain**: No dependencies (except core utilities)
- **Application**: Depends on Domain only
- **Infrastructure**: Depends on Domain only (implements domain interfaces)
- **Presentation**: Depends on Application and Domain

### What We Had (Incorrect):
```
Infrastructure ↔ Application → Domain
```
This creates a cycle when both layers depend on each other.

## Complete Fix Applied

### 1. Removed Infrastructure → Application Dependency
**Before (Incorrect):**
```xml
<!-- regtech-iam/infrastructure/pom.xml -->
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-iam-application</artifactId>
</dependency>
```

**After (Correct):**
```xml
<!-- regtech-iam/infrastructure/pom.xml -->
<!-- Removed application dependency -->
<!-- Infrastructure only depends on domain -->
```

### 2. Removed Application → Infrastructure Dependency
**Before (Incorrect):**
```xml
<!-- regtech-iam/application/pom.xml -->
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-iam-infrastructure</artifactId>
</dependency>
```

**After (Correct):**
```xml
<!-- regtech-iam/application/pom.xml -->
<!-- Removed infrastructure dependency -->
<!-- Application only depends on domain -->
```

### 3. Created Domain Interface for OAuth2 Configuration

**Problem**: Application layer was directly importing `OAuth2Configuration` from infrastructure.

**Solution**: Created a domain service interface to abstract OAuth2 configuration access.

**New Domain Interface:**
```java
// regtech-iam/domain/src/main/java/com/bcbs239/regtech/iam/domain/authentication/OAuth2ConfigurationService.java
public interface OAuth2ConfigurationService {
    OAuth2ProviderConfig getProviderConfig(String provider);
    boolean isProviderConfigured(String provider);
    String getAuthorizationUrl(String provider, String redirectUri);
    
    record OAuth2ProviderConfig(
        String clientId,
        String clientSecret,
        String tokenUrl,
        String userInfoUrl,
        String scope
    ) {}
}
```

**Infrastructure Implementation:**
```java
// regtech-iam/infrastructure/src/main/java/com/bcbs239/regtech/iam/infrastructure/authentication/OAuth2ConfigurationServiceImpl.java
@Service
public class OAuth2ConfigurationServiceImpl implements OAuth2ConfigurationService {
    private final OAuth2Configuration oauth2Config;
    // Implementation bridges domain interface with infrastructure configuration
}
```

**Application Layer Update:**
```java
// Before: Direct infrastructure import
import com.bcbs239.regtech.iam.infrastructure.configuration.OAuth2Configuration;

// After: Domain interface import
import com.bcbs239.regtech.iam.domain.authentication.OAuth2ConfigurationService;
```

### 4. Added Missing Repository Method

**Problem**: `RegisterUserCommandHandler` was calling `userRepository.userRoleSaver()` but this method didn't exist.

**Solution**: Added the missing method to the `UserRepository` interface:
```java
// regtech-iam/domain/src/main/java/com/bcbs239/regtech/iam/domain/users/UserRepository.java
/**
 * Returns a function that saves a user role
 */
Function<UserRole, Result<String>> userRoleSaver();
```

## Verification

### Dependency Flow Check:
✅ **Domain** (`regtech-iam-domain`): 
- Depends on: `regtech-core`
- No other internal dependencies

✅ **Application** (`regtech-iam-application`):
- Depends on: `regtech-iam-domain`, `regtech-core`
- Uses domain interfaces (e.g., `OAuth2ConfigurationService`)

✅ **Infrastructure** (`regtech-iam-infrastructure`):
- Depends on: `regtech-iam-domain`, `regtech-core`
- Implements domain interfaces (e.g., `OAuth2ConfigurationServiceImpl`)

✅ **Presentation** (`regtech-iam-presentation`):
- Depends on: `regtech-iam-application`, `regtech-iam-domain`, `regtech-core`
- Uses application services and domain objects

### No Import Violations:
✅ Infrastructure layer has no imports from application layer
✅ Application layer has no imports from infrastructure layer
✅ Application layer uses domain interfaces, not infrastructure implementations
✅ All layers respect clean architecture boundaries

### Build Verification:
✅ **Domain layer**: `mvn clean compile -pl regtech-iam/domain -am` - SUCCESS
✅ **Application layer**: `mvn clean compile -pl regtech-iam/application -am` - SUCCESS  
✅ **Infrastructure layer**: `mvn clean compile -pl regtech-iam/infrastructure -am` - SUCCESS
✅ **Presentation layer**: `mvn clean compile -pl regtech-iam/presentation -am` - SUCCESS
✅ **Full IAM module**: `mvn clean compile -pl regtech-iam -am` - SUCCESS

## How Dependency Injection Works

With the correct dependency structure:

1. **Infrastructure** implements domain interfaces:
   ```java
   @Service
   public class OAuth2ConfigurationServiceImpl implements OAuth2ConfigurationService {
       // Implementation using OAuth2Configuration
   }
   ```

2. **Application** depends on domain interfaces:
   ```java
   @Service
   public class OAuth2ProviderServiceImpl implements OAuth2ProviderService {
       private final OAuth2ConfigurationService oauth2ConfigService; // Domain interface
   }
   ```

3. **Spring** wires implementations at runtime:
   ```java
   @Configuration
   public class IamConfiguration {
       @Bean
       public OAuth2ConfigurationService oauth2ConfigService(OAuth2Configuration config) {
           return new OAuth2ConfigurationServiceImpl(config);
       }
   }
   ```

## Benefits of Correct Architecture

1. **No Circular Dependencies**: Clean build process
2. **Testability**: Application layer can be tested with mock implementations
3. **Flexibility**: Infrastructure implementations can be swapped
4. **Maintainability**: Clear separation of concerns
5. **Compliance**: Follows clean architecture principles
6. **Dependency Injection**: Spring can properly wire dependencies at runtime

## Summary

The dependency cycle has been completely resolved by:
- Removing bidirectional dependencies between application and infrastructure layers
- Creating proper domain abstractions for infrastructure concerns
- Ensuring all layers follow clean architecture dependency rules
- Adding missing repository methods to support existing functionality

The IAM module now has a clean, maintainable architecture that follows dependency inversion principles and supports proper testing and flexibility.