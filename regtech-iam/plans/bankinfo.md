# Bank Profile Configuration - regtech-iam Module Implementation Plan


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ITERATION 1: API Contract with Fake Data
 * 
 * TOP LAYER - Start here!
 * Design the API response FIRST with fake data.
 * Cover ALL use cases:
 * - Complete bank profile with all fields
 * - Bank with optional fields missing
 * - Different bank types and categories
 * 
 * This endpoint is FROZEN after this iteration.
 * All further work happens BELOW this layer.
 */

public class BankProfileConfigurationController {

  
    public ResponseEntity<BankProfileResponse> getBankProfile() {
        // Lorem Ipsum for APIs - temporary record with fake data
        var fakeProfile = new FakeBankProfile(
            "Banca Italiana SpA",
            "12345",
            "549300ABCDEFGH12345",
            "INDEPENDENT",
            "COMMERCIAL",
            "SIGNIFICANT_SSM",
            "Via Roma 1, 20121 Milano MI, Italia",
            "IT12345678901",
            "12345678901",
            "MI-123456",
            "info@bancaitaliana.it",
            "pec@bancaitaliana.pec.it",
            "+39 02 1234567",
            "https://www.bancaitaliana.it",
            "2024-12-10T15:30:00Z",
            "Marco Rossi"
        );
        
        return ResponseEntity.ok(new BankProfileResponse(
            fakeProfile.legalName(),
            fakeProfile.abiCode(),
            fakeProfile.leiCode(),
            fakeProfile.groupType(),
            fakeProfile.bankType(),
            fakeProfile.supervisionCategory(),
            fakeProfile.legalAddress(),
            fakeProfile.vatNumber(),
            fakeProfile.taxCode(),
            fakeProfile.companyRegistry(),
            fakeProfile.institutionalEmail(),
            fakeProfile.pec(),
            fakeProfile.phone(),
            fakeProfile.website(),
            fakeProfile.lastModified(),
            fakeProfile.lastModifiedBy()
        ));
    }


    public ResponseEntity<BankProfileResponse> updateBankProfile(
            @RequestBody UpdateBankProfileRequest request) {
        
        // For now, just echo back the request as if it was saved
        // This proves the API contract works
        var fakeUpdatedProfile = new BankProfileResponse(
            request.legalName(),
            request.abiCode(),
            request.leiCode(),
            request.groupType(),
            request.bankType(),
            request.supervisionCategory(),
            request.legalAddress(),
            request.vatNumber(),
            request.taxCode(),
            request.companyRegistry(),
            request.institutionalEmail(),
            request.pec(),
            request.phone(),
            request.website(),
            java.time.Instant.now().toString(),
            "Marco Rossi"
        );
        
        return ResponseEntity.ok(fakeUpdatedProfile);
    }

    /**
     * Temporary fake data record
     * Will be replaced with proper DTO in next iteration
     */
    private record FakeBankProfile(
        String legalName,
        String abiCode,
        String leiCode,
        String groupType,
        String bankType,
        String supervisionCategory,
        String legalAddress,
        String vatNumber,
        String taxCode,
        String companyRegistry,
        String institutionalEmail,
        String pec,
        String phone,
        String website,
        String lastModified,
        String lastModifiedBy
    ) {}

    /**
     * Response DTO - proper structure
     */
    public record BankProfileResponse(
        String legalName,
        String abiCode,
        String leiCode,
        String groupType,
        String bankType,
        String supervisionCategory,
        String legalAddress,
        String vatNumber,
        String taxCode,
        String companyRegistry,
        String institutionalEmail,
        String pec,
        String phone,
        String website,
        String lastModified,
        String lastModifiedBy
    ) {}

    /**
     * Request DTO for updates
     */
    public record UpdateBankProfileRequest(
        String legalName,
        String abiCode,
        String leiCode,
        String groupType,
        String bankType,
        String supervisionCategory,
        String legalAddress,
        String vatNumber,
        String taxCode,
        String companyRegistry,
        String institutionalEmail,
        String pec,
        String phone,
        String website
    ) {}
}

## Project Structure (Organized by Capabilities)

```
regtech-iam/
├── domain/
│   └── src/main/java/com/bcbs239/regtech/iam/domain/
│       └── bankprofile/                          # Capability: Bank Profile
│           ├── BankProfile.java                  → Aggregate Root
│           ├── BankProfileRepository.java        → Repository interface
│           └── valueobject/
│               ├── AbiCode.java                  → Smart constructor with Result
│               ├── LeiCode.java                  → Smart constructor with Result
│               ├── LegalName.java                → Smart constructor with Result
│               ├── GroupType.java                → Enum
│               ├── BankType.java                 → Enum
│               ├── SupervisionCategory.java      → Enum
│               ├── VatNumber.java                → Smart constructor with Maybe
│               ├── TaxCode.java                  → Smart constructor with Maybe
│               ├── EmailAddress.java             → Smart constructor with Maybe
│               ├── PhoneNumber.java              → Smart constructor with Maybe
│               └── WebsiteUrl.java               → Smart constructor with Maybe
│
├── application/
│   └── src/main/java/com/bcbs239/regtech/iam/application/
│       └── bankprofile/                          # Capability: Bank Profile
│           ├── GetBankProfileHandler.java        → Query handler
│           └── UpdateBankProfileHandler.java     → Command handler
│
├── infrastructure/
│   └── src/main/java/com/bcbs239/regtech/iam/infrastructure/
│       └── persistence/bankprofile/
│           ├── BankProfileJpaEntity.java         → JPA entity
│           ├── BankProfileJpaRepository.java     → Spring Data
│           └── BankProfileRepositoryAdapter.java → Repository implementation
│
└── presentation/
    └── src/main/java/com/bcbs239/regtech/iam/presentation/
        └── bankprofile/                          # Capability: Bank Profile
            ├── BankProfileController.java        → Route functions
            ├── BankProfileRequest.java           → Request DTO (record)
            └── BankProfileResponse.java          → Response DTO (record)
```

---

## Reference: Result and Maybe Patterns

From `regtech-core/domain/src/main/java/com/bcbs239/regtech/core/domain/shared/`:

```java
// Result pattern - for operations that can fail
Result<AbiCode> result = AbiCode.of("12345");
if (result.isSuccess()) {
    AbiCode code = result.getValue();
}

// Maybe pattern - for nullable values  
Maybe<VatNumber> maybe = VatNumber.of("IT12345678901");
if (maybe.isPresent()) {
    VatNumber vat = maybe.get();
}
```

---

## ITERATION 1: Domain Layer - Value Objects with Smart Constructors

### Value Objects with Result Pattern (Required Fields)

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/AbiCode.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * ABI Code - Associazione Bancaria Italiana
 * 
 * Business Rules:
 * - Exactly 5 digits
 * - Assigned by Banca d'Italia
 * 
 * Smart constructor returns Result to handle validation errors.
 */
@Value
public class AbiCode {
    String value;
    
    private AbiCode(String value) {
        this.value = value;
    }
    
    /**
     * Smart constructor - returns Result<AbiCode>
     * Validation errors are captured in Result, not exceptions.
     */
    public static Result<AbiCode> of(String value) {
        if (value == null) {
            return Result.failure("ABI code cannot be null");
        }
        
        String trimmed = value.trim();
        
        if (!trimmed.matches("\\d{5}")) {
            return Result.failure(
                "ABI code must be exactly 5 digits, got: " + trimmed
            );
        }
        
        return Result.success(new AbiCode(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/LeiCode.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * LEI Code - Legal Entity Identifier
 * ISO 17442 standard
 */
@Value
public class LeiCode {
    String value;
    
    private LeiCode(String value) {
        this.value = value;
    }
    
    public static Result<LeiCode> of(String value) {
        if (value == null) {
            return Result.failure("LEI code cannot be null");
        }
        
        String normalized = value.trim().toUpperCase();
        
        if (!normalized.matches("[A-Z0-9]{20}")) {
            return Result.failure(
                "LEI code must be exactly 20 alphanumeric characters, got: " + normalized
            );
        }
        
        return Result.success(new LeiCode(normalized));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/LegalName.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Value;

/**
 * Legal Name of the banking institution
 */
@Value
public class LegalName {
    String value;
    
    private LegalName(String value) {
        this.value = value;
    }
    
    public static Result<LegalName> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Legal name cannot be empty");
        }
        
        String trimmed = value.trim();
        
        if (trimmed.length() > 255) {
            return Result.failure(
                "Legal name too long (max 255 chars): " + trimmed.length()
            );
        }
        
        return Result.success(new LegalName(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Value Objects with Maybe Pattern (Optional Fields)

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/VatNumber.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Italian VAT Number (Partita IVA)
 * Format: IT + 11 digits
 * 
 * Optional field - uses Maybe pattern
 */
@Value
public class VatNumber {
    String value;
    
    private VatNumber(String value) {
        this.value = value;
    }
    
    /**
     * Smart constructor - returns Maybe<VatNumber>
     * Returns Maybe.empty() for null/invalid, Maybe.of(vat) for valid
     */
    public static Maybe<VatNumber> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim();
        
        if (!trimmed.matches("IT\\d{11}")) {
            // Invalid format - return empty (validation happened at boundary)
            return Maybe.empty();
        }
        
        return Maybe.of(new VatNumber(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/EmailAddress.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Email address value object
 * Optional field - uses Maybe pattern
 */
@Value
public class EmailAddress {
    String value;
    
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private EmailAddress(String value) {
        this.value = value;
    }
    
    public static Maybe<EmailAddress> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim();
        
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Maybe.empty();
        }
        
        return Maybe.of(new EmailAddress(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### Enums

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/GroupType.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Banking group structure type
 */
@Getter
@RequiredArgsConstructor
public enum GroupType {
    INDEPENDENT("Istituto Indipendente"),
    NATIONAL_GROUP("Gruppo Nazionale"),
    INTERNATIONAL_GROUP("Gruppo Internazionale");
    
    private final String displayName;
    
    public static Result<GroupType> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Group type cannot be empty");
        }
        
        try {
            return Result.success(GroupType.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid group type: " + value);
        }
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/BankType.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankType {
    COMMERCIAL("Banca Commerciale"),
    INVESTMENT("Banca di Investimento"),
    COOPERATIVE("Banca Cooperativa"),
    POPULAR("Banca Popolare"),
    BCC("Banca di Credito Cooperativo");
    
    private final String displayName;
    
    public static Result<BankType> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Bank type cannot be empty");
        }
        
        try {
            return Result.success(BankType.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid bank type: " + value);
        }
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/valueobject/SupervisionCategory.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupervisionCategory {
    SIGNIFICANT_SSM("Banche significative (SSM)"),
    LESS_SIGNIFICANT("Banche meno significative"),
    SYSTEMICALLY_IMPORTANT("Banche di rilevanza sistemica nazionale"),
    OTHER("Altre banche");
    
    private final String displayName;
    
    public static Result<SupervisionCategory> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure("Supervision category cannot be empty");
        }
        
        try {
            return Result.success(SupervisionCategory.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid supervision category: " + value);
        }
    }
}
```

---

## ITERATION 2: Domain Model - Aggregate Root

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/BankProfile.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;

/**
 * BankProfile - Aggregate Root
 * 
 * Represents the bank's institutional profile.
 * Immutable - use builder for updates.
 */
@Value
@Builder(toBuilder = true)
public class BankProfile {
    
    // Required fields
    LegalName legalName;
    AbiCode abiCode;
    LeiCode leiCode;
    GroupType groupType;
    BankType bankType;
    SupervisionCategory supervisionCategory;
    String legalAddress;
    
    // Optional fields (Maybe pattern)
    Maybe<VatNumber> vatNumber;
    Maybe<TaxCode> taxCode;
    Maybe<String> companyRegistry;
    Maybe<EmailAddress> institutionalEmail;
    Maybe<EmailAddress> pec;
    Maybe<PhoneNumber> phone;
    Maybe<WebsiteUrl> website;
    
    // Metadata
    Instant lastModified;
    String lastModifiedBy;
    
    /**
     * Domain behavior: Is this a significant bank under SSM?
     */
    public boolean isSignificant() {
        return supervisionCategory == SupervisionCategory.SIGNIFICANT_SSM;
    }
    
    /**
     * Domain behavior: Is this an independent institution?
     */
    public boolean isIndependent() {
        return groupType == GroupType.INDEPENDENT;
    }
    
    /**
     * Domain method: Update profile with new values
     * Returns new instance (immutability)
     */
    public BankProfile update(
            LegalName legalName,
            AbiCode abiCode,
            LeiCode leiCode,
            GroupType groupType,
            BankType bankType,
            SupervisionCategory supervisionCategory,
            String legalAddress,
            Maybe<VatNumber> vatNumber,
            Maybe<TaxCode> taxCode,
            Maybe<String> companyRegistry,
            Maybe<EmailAddress> institutionalEmail,
            Maybe<EmailAddress> pec,
            Maybe<PhoneNumber> phone,
            Maybe<WebsiteUrl> website,
            String modifiedBy) {
        
        return this.toBuilder()
            .legalName(legalName)
            .abiCode(abiCode)
            .leiCode(leiCode)
            .groupType(groupType)
            .bankType(bankType)
            .supervisionCategory(supervisionCategory)
            .legalAddress(legalAddress)
            .vatNumber(vatNumber)
            .taxCode(taxCode)
            .companyRegistry(companyRegistry)
            .institutionalEmail(institutionalEmail)
            .pec(pec)
            .phone(phone)
            .website(website)
            .lastModified(Instant.now())
            .lastModifiedBy(modifiedBy)
            .build();
    }
}
```

**File: `domain/src/main/java/com/bcbs239/regtech/iam/domain/bankprofile/BankProfileRepository.java`**

```java
package com.bcbs239.regtech.iam.domain.bankprofile;

import com.bcbs239.regtech.core.domain.shared.Maybe;

/**
 * Repository interface (domain layer)
 * Implementation in infrastructure layer
 */
public interface BankProfileRepository {
    
    /**
     * Get current bank profile (singleton)
     */
    Maybe<BankProfile> findCurrent();
    
    /**
     * Save/update bank profile
     */
    BankProfile save(BankProfile profile);
}
```

---

## ITERATION 3: Application Layer - Handlers

**File: `application/src/main/java/com/bcbs239/regtech/iam/application/bankprofile/GetBankProfileHandler.java`**

```java
package com.bcbs239.regtech.iam.application.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.BankProfile;
import com.bcbs239.regtech.iam.domain.bankprofile.BankProfileRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query Handler: Get Bank Profile
 * 
 * Application layer = GLUE only
 * Just coordinates repository call
 */
@Service
@RequiredArgsConstructor
public class GetBankProfileHandler {
    
    private final BankProfileRepository repository;
    
    @Transactional(readOnly = true)
    public Maybe<BankProfile> handle() {
        return repository.findCurrent();
    }
}
```

**File: `application/src/main/java/com/bcbs239/regtech/iam/application/bankprofile/UpdateBankProfileHandler.java`**

```java
package com.bcbs239.regtech.iam.application.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.BankProfile;
import com.bcbs239.regtech.iam.domain.bankprofile.BankProfileRepository;
import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Update Bank Profile
 * 
 * Returns Result<BankProfile> because:
 * - Value object construction can fail
 * - Validation can fail
 */
@Service
@RequiredArgsConstructor
public class UpdateBankProfileHandler {
    
    private final BankProfileRepository repository;
    
    @Value
    public static class UpdateCommand {
        String legalName;
        String abiCode;
        String leiCode;
        String groupType;
        String bankType;
        String supervisionCategory;
        String legalAddress;
        String vatNumber;
        String taxCode;
        String companyRegistry;
        String institutionalEmail;
        String pec;
        String phone;
        String website;
        String modifiedBy;
    }
    
    @Transactional
    public Result<BankProfile> handle(UpdateCommand command) {
        // Create value objects using smart constructors
        // All return Result - compose them!
        
        var legalNameResult = LegalName.of(command.legalName);
        if (legalNameResult.isFailure()) {
            return Result.failure(legalNameResult.getError());
        }
        
        var abiCodeResult = AbiCode.of(command.abiCode);
        if (abiCodeResult.isFailure()) {
            return Result.failure(abiCodeResult.getError());
        }
        
        var leiCodeResult = LeiCode.of(command.leiCode);
        if (leiCodeResult.isFailure()) {
            return Result.failure(leiCodeResult.getError());
        }
        
        var groupTypeResult = GroupType.of(command.groupType);
        if (groupTypeResult.isFailure()) {
            return Result.failure(groupTypeResult.getError());
        }
        
        var bankTypeResult = BankType.of(command.bankType);
        if (bankTypeResult.isFailure()) {
            return Result.failure(bankTypeResult.getError());
        }
        
        var supervisionCategoryResult = SupervisionCategory.of(command.supervisionCategory);
        if (supervisionCategoryResult.isFailure()) {
            return Result.failure(supervisionCategoryResult.getError());
        }
        
        // Validate legal address
        if (command.legalAddress == null || command.legalAddress.isBlank()) {
            return Result.failure("Legal address is required");
        }
        
        // Optional fields - use Maybe pattern
        var vatNumber = VatNumber.of(command.vatNumber);
        var taxCode = TaxCode.of(command.taxCode);
        var companyRegistry = command.companyRegistry != null && !command.companyRegistry.isBlank()
            ? Maybe.of(command.companyRegistry)
            : Maybe.empty();
        var institutionalEmail = EmailAddress.of(command.institutionalEmail);
        var pec = EmailAddress.of(command.pec);
        var phone = PhoneNumber.of(command.phone);
        var website = WebsiteUrl.of(command.website);
        
        // Build domain model
        var bankProfile = BankProfile.builder()
            .legalName(legalNameResult.getValue())
            .abiCode(abiCodeResult.getValue())
            .leiCode(leiCodeResult.getValue())
            .groupType(groupTypeResult.getValue())
            .bankType(bankTypeResult.getValue())
            .supervisionCategory(supervisionCategoryResult.getValue())
            .legalAddress(command.legalAddress.trim())
            .vatNumber(vatNumber)
            .taxCode(taxCode)
            .companyRegistry(companyRegistry)
            .institutionalEmail(institutionalEmail)
            .pec(pec)
            .phone(phone)
            .website(website)
            .lastModified(java.time.Instant.now())
            .lastModifiedBy(command.modifiedBy)
            .build();
        
        // Save via repository
        var saved = repository.save(bankProfile);
        
        return Result.success(saved);
    }
}
```

---

## ITERATION 4: Presentation Layer - Route Functions

**File: `presentation/src/main/java/com/bcbs239/regtech/iam/presentation/bankprofile/BankProfileResponse.java`**

```java
package com.bcbs239.regtech.iam.presentation.bankprofile;

/**
 * Response DTO for bank profile
 */
public record BankProfileResponse(
    String legalName,
    String abiCode,
    String leiCode,
    String groupType,
    String bankType,
    String supervisionCategory,
    String legalAddress,
    String vatNumber,
    String taxCode,
    String companyRegistry,
    String institutionalEmail,
    String pec,
    String phone,
    String website,
    String lastModified,
    String lastModifiedBy
) {
    /**
     * Map domain model → DTO
     */
    public static BankProfileResponse from(com.bcbs239.regtech.iam.domain.bankprofile.BankProfile profile) {
        return new BankProfileResponse(
            profile.getLegalName().getValue(),
            profile.getAbiCode().getValue(),
            profile.getLeiCode().getValue(),
            profile.getGroupType().name(),
            profile.getBankType().name(),
            profile.getSupervisionCategory().name(),
            profile.getLegalAddress(),
            profile.getVatNumber().map(vat -> vat.getValue()).orElse(null),
            profile.getTaxCode().map(tax -> tax.getValue()).orElse(null),
            profile.getCompanyRegistry().orElse(null),
            profile.getInstitutionalEmail().map(email -> email.getValue()).orElse(null),
            profile.getPec().map(email -> email.getValue()).orElse(null),
            profile.getPhone().map(phone -> phone.getValue()).orElse(null),
            profile.getWebsite().map(url -> url.getValue()).orElse(null),
            profile.getLastModified().toString(),
            profile.getLastModifiedBy()
        );
    }
}
```

**File: `presentation/src/main/java/com/bcbs239/regtech/iam/presentation/bankprofile/BankProfileRequest.java`**

```java
package com.bcbs239.regtech.iam.presentation.bankprofile;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating bank profile
 * Validation at the boundary
 */
public record BankProfileRequest(
    
    @NotBlank(message = "Legal name is required")
    @Size(max = 255, message = "Legal name too long")
    String legalName,
    
    @NotBlank(message = "ABI code is required")
    @Pattern(regexp = "\\d{5}", message = "ABI code must be exactly 5 digits")
    String abiCode,
    
    @NotBlank(message = "LEI code is required")
    @Pattern(regexp = "[A-Z0-9]{20}", message = "LEI code must be exactly 20 alphanumeric characters")
    String leiCode,
    
    @NotBlank(message = "Group type is required")
    String groupType,
    
    @NotBlank(message = "Bank type is required")
    String bankType,
    
    @NotBlank(message = "Supervision category is required")
    String supervisionCategory,
    
    @NotBlank(message = "Legal address is required")
    String legalAddress,
    
    @Pattern(regexp = "IT\\d{11}|^$", message = "Invalid VAT number format")
    String vatNumber,
    
    @Pattern(regexp = "\\d{11}|^$", message = "Invalid tax code format")
    String taxCode,
    
    String companyRegistry,
    
    @Email(message = "Invalid email format")
    String institutionalEmail,
    
    @Email(message = "Invalid PEC format")
    String pec,
    
    String phone,
    
    @Pattern(regexp = "https?://.+|^$", message = "Invalid website URL")
    String website
) {
    /**
     * Map DTO → Command
     */
    public com.bcbs239.regtech.iam.application.bankprofile.UpdateBankProfileHandler.UpdateCommand toCommand(String modifiedBy) {
        return new com.bcbs239.regtech.iam.application.bankprofile.UpdateBankProfileHandler.UpdateCommand(
            legalName,
            abiCode,
            leiCode,
            groupType,
            bankType,
            supervisionCategory,
            legalAddress,
            vatNumber,
            taxCode,
            companyRegistry,
            institutionalEmail,
            pec,
            phone,
            website,
            modifiedBy
        );
    }
}
```

**File: `presentation/src/main/java/com/bcbs239/regtech/iam/presentation/bankprofile/BankProfileController.java`**

```java
package com.bcbs239.regtech.iam.presentation.bankprofile;

import com.bcbs239.regtech.iam.application.bankprofile.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * Bank Profile Routes (Route Functions)
 * 
 * Following the pattern from UserController in regtech-iam
 */
@Configuration
@RequiredArgsConstructor
public class BankProfileController {
    
    private final GetBankProfileHandler getBankProfileHandler;
    private final UpdateBankProfileHandler updateBankProfileHandler;
    
    @Bean
    public RouterFunction<ServerResponse> bankProfileRoutes() {
        return route()
            .GET("/api/v1/configuration/bank-profile", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::getBankProfile)
            .PUT("/api/v1/configuration/bank-profile", 
                 accept(MediaType.APPLICATION_JSON), 
                 this::updateBankProfile)
            .build();
    }
    
    /**
     * GET /api/v1/configuration/bank-profile
     */
    private ServerResponse getBankProfile(org.springframework.web.servlet.function.ServerRequest request) {
        var profileMaybe = getBankProfileHandler.handle();
        
        if (profileMaybe.isEmpty()) {
            return ServerResponse.notFound().build();
        }
        
        var response = BankProfileResponse.from(profileMaybe.get());
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    /**
     * PUT /api/v1/configuration/bank-profile
     */
    private ServerResponse updateBankProfile(org.springframework.web.servlet.function.ServerRequest request) {
        try {
            // Parse and validate request
            var requestDto = request.body(BankProfileRequest.class);
            
            // TODO: Get actual user from security context
            String currentUser = "Marco Rossi";
            
            // Convert to command and execute
            var command = requestDto.toCommand(currentUser);
            var result = updateBankProfileHandler.handle(command);
            
            // Handle Result
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(result.getError()));
            }
            
            var response = BankProfileResponse.from(result.getValue());
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("Invalid request: " + e.getMessage()));
        }
    }
    
    private record ErrorResponse(String error) {}
}
```

---

## ITERATION 5: Infrastructure - Persistence

**File: `infrastructure/src/main/java/com/bcbs239/regtech/iam/infrastructure/persistence/bankprofile/BankProfileJpaEntity.java`**

```java
package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA Entity for bank_profile table
 * Separate from domain model
 */
@Entity
@Table(name = "bank_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankProfileJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "legal_name", nullable = false)
    private String legalName;
    
    @Column(name = "abi_code", nullable = false, length = 5, unique = true)
    private String abiCode;
    
    @Column(name = "lei_code", nullable = false, length = 20, unique = true)
    private String leiCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private String groupType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_type", nullable = false)
    private String bankType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "supervision_category", nullable = false)
    private String supervisionCategory;
    
    @Column(name = "legal_address", nullable = false, columnDefinition = "TEXT")
    private String legalAddress;
    
    @Column(name = "vat_number", length = 13)
    private String vatNumber;
    
    @Column(name = "tax_code", length = 11)
    private String taxCode;
    
    @Column(name = "company_registry", length = 100)
    private String companyRegistry;
    
    @Column(name = "institutional_email")
    private String institutionalEmail;
    
    @Column(name = "pec")
    private String pec;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;
    
    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;
}
```

**File: `infrastructure/src/main/java/com/bcbs239/regtech/iam/infrastructure/persistence/bankprofile/BankProfileJpaRepository.java`**

```java
package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface BankProfileJpaRepository extends JpaRepository<BankProfileJpaEntity, Long> {
    
    /**
     * Get current bank profile (singleton pattern)
     */
    @Query("SELECT b FROM BankProfileJpaEntity b ORDER BY b.lastModified DESC LIMIT 1")
    Optional<BankProfileJpaEntity> findCurrent();
}
```

**File: `infrastructure/src/main/java/com/bcbs239/regtech/iam/infrastructure/persistence/bankprofile/BankProfileRepositoryAdapter.java`**

```java
package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.*;
import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository Adapter - maps JPA ↔ Domain
 */
@Repository
@RequiredArgsConstructor
public class BankProfileRepositoryAdapter implements BankProfileRepository {
    
    private final BankProfileJpaRepository jpaRepository;
    
    @Override
    public Maybe<BankProfile> findCurrent() {
        return jpaRepository.findCurrent()
            .map(this::toDomain)
            .map(Maybe::of)
            .orElse(Maybe.empty());
    }
    
    @Override
    public BankProfile save(BankProfile profile) {
        var entity = toEntity(profile);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    /**
     * JPA → Domain
     */
    private BankProfile toDomain(BankProfileJpaEntity entity) {
        return BankProfile.builder()
            .legalName(LegalName.of(entity.getLegalName()).getValue())
            .abiCode(AbiCode.of(entity.getAbiCode()).getValue())
            .leiCode(LeiCode.of(entity.getLeiCode()).getValue())
            .groupType(GroupType.of(entity.getGroupType()).getValue())
            .bankType(BankType.of(entity.getBankType()).getValue())
            .supervisionCategory(SupervisionCategory.of(entity.getSupervisionCategory()).getValue())
            .legalAddress(entity.getLegalAddress())
            .vatNumber(VatNumber.of(entity.getVatNumber()))
            .taxCode(TaxCode.of(entity.getTaxCode()))
            .companyRegistry(entity.getCompanyRegistry() != null ? Maybe.of(entity.getCompanyRegistry()) : Maybe.empty())
            .institutionalEmail(EmailAddress.of(entity.getInstitutionalEmail()))
            .pec(EmailAddress.of(entity.getPec()))
            .phone(PhoneNumber.of(entity.getPhone()))
            .website(WebsiteUrl.of(entity.getWebsite()))
            .lastModified(entity.getLastModified())
            .lastModifiedBy(entity.getLastModifiedBy())
            .build();
    }
    
    /**
     * Domain → JPA
     */
    private BankProfileJpaEntity toEntity(BankProfile profile) {
        return BankProfileJpaEntity.builder()
            .legalName(profile.getLegalName().getValue())
            .abiCode(profile.getAbiCode().getValue())
            .leiCode(profile.getLeiCode().getValue())
            .groupType(profile.getGroupType().name())
            .bankType(profile.getBankType().name())
            .supervisionCategory(profile.getSupervisionCategory().name())
            .legalAddress(profile.getLegalAddress())
            .vatNumber(profile.getVatNumber().map(VatNumber::getValue).orElse(null))
            .taxCode(profile.getTaxCode().map(TaxCode::getValue).orElse(null))
            .companyRegistry(profile.getCompanyRegistry().orElse(null))
            .institutionalEmail(profile.getInstitutionalEmail().map(EmailAddress::getValue).orElse(null))
            .pec(profile.getPec().map(EmailAddress::getValue).orElse(null))
            .phone(profile.getPhone().map(PhoneNumber::getValue).orElse(null))
            .website(profile.getWebsite().map(WebsiteUrl::getValue).orElse(null))
            .lastModified(profile.getLastModified())
            .lastModifiedBy(profile.getLastModifiedBy())
            .build();
    }
}
```

---

## ITERATION 6: Database Migration - Update Existing Flyway

**File: `regtech-app/src/main/resources/db/migration/iam/V002__add_bank_profile.sql`**

```sql
-- Add bank profile table to existing IAM schema

CREATE TABLE IF NOT EXISTS bank_profile (
    id BIGSERIAL PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    abi_code VARCHAR(5) NOT NULL,
    lei_code VARCHAR(20) NOT NULL,
    group_type VARCHAR(50) NOT NULL,
    bank_type VARCHAR(50) NOT NULL,
    supervision_category VARCHAR(50) NOT NULL,
    legal_address TEXT NOT NULL,
    vat_number VARCHAR(13),
    tax_code VARCHAR(11),
    company_registry VARCHAR(100),
    institutional_email VARCHAR(255),
    pec VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT uk_bank_profile_abi UNIQUE (abi_code),
    CONSTRAINT uk_bank_profile_lei UNIQUE (lei_code),
    CONSTRAINT chk_group_type CHECK (group_type IN ('INDEPENDENT', 'NATIONAL_GROUP', 'INTERNATIONAL_GROUP')),
    CONSTRAINT chk_bank_type CHECK (bank_type IN ('COMMERCIAL', 'INVESTMENT', 'COOPERATIVE', 'POPULAR', 'BCC')),
    CONSTRAINT chk_supervision CHECK (supervision_category IN ('SIGNIFICANT_SSM', 'LESS_SIGNIFICANT', 'SYSTEMICALLY_IMPORTANT', 'OTHER'))
);

-- Index for fast retrieval
CREATE INDEX idx_bank_profile_last_modified ON bank_profile(last_modified DESC);

-- Insert initial bank profile data
INSERT INTO bank_profile (
    legal_name,
    abi_code,
    lei_code,
    group_type,
    bank_type,
    supervision_category,
    legal_address,
    vat_number,
    tax_code,
    company_registry,
    institutional_email,
    pec,
    phone,
    website,
    last_modified,
    last_modified_by
) VALUES (
    'Banca Italiana SpA',
    '12345',
    '549300ABCDEFGH12345',
    'INDEPENDENT',
    'COMMERCIAL',
    'SIGNIFICANT_SSM',
    'Via Roma 1, 20121 Milano MI, Italia',
    'IT12345678901',
    '12345678901',
    'MI-123456',
    'info@bancaitaliana.it',
    'pec@bancaitaliana.pec.it',
    '+39 02 1234567',
    'https://www.bancaitaliana.it',
    NOW(),
    'System'
);

-- Add comment
COMMENT ON TABLE bank_profile IS 'Bank institutional profile for BCBS 239 compliance - Singleton table (one row only)';
```

---

## Summary: Implementation Checklist

### Domain Layer (regtech-iam/domain)
- [ ] Create value objects with smart constructors returning Result/Maybe:
  - [ ] AbiCode, LeiCode, LegalName (Result pattern)
  - [ ] VatNumber, TaxCode, EmailAddress, PhoneNumber, WebsiteUrl (Maybe pattern)
  - [ ] GroupType, BankType, SupervisionCategory (enums with Result)
- [ ] Create BankProfile aggregate root with Lombok @Value @Builder
- [ ] Create BankProfileRepository interface

### Application Layer (regtech-iam/application)
- [ ] Create GetBankProfileHandler (query)
- [ ] Create UpdateBankProfileHandler (command) returning Result

### Presentation Layer (regtech-iam/presentation)
- [ ] Create BankProfileResponse record with static from() mapper
- [ ] Create BankProfileRequest record with Jakarta validation
- [ ] Create BankProfileController with route functions (@Configuration + @Bean)

### Infrastructure Layer (regtech-iam/infrastructure)
- [ ] Create BankProfileJpaEntity with Lombok
- [ ] Create BankProfileJpaRepository (Spring Data)
- [ ] Create BankProfileRepositoryAdapter implementing domain repository

### Database
- [ ] Add Flyway migration V002__add_bank_profile.sql to regtech-app

---

## Key Differences from Initial Plan

1. ✅ **Smart constructors** return `Result<T>` or `Maybe<T>`
2. ✅ **Lombok** used for value objects and entities
3. ✅ **Route functions** instead of @RestController
4. ✅ **Capability-based** organization (not technical layers)
5. ✅ **regtech-iam** module structure
6. ✅ **Existing Flyway** migration extended

Start implementing from domain layer (value objects) and work your way out!