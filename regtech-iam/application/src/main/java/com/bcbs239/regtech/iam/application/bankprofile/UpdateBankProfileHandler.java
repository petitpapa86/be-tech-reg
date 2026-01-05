package com.bcbs239.regtech.iam.application.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.BankProfile;
import com.bcbs239.regtech.iam.domain.bankprofile.BankProfileRepository;
import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Command Handler: Update Bank Profile
 * 
 * Application layer = Coordinator
 * Orchestrates the update use case, coordinating:
 * - Value object construction and validation
 * - Domain model updates
 * - Repository persistence
 * 
 * Returns Result&lt;BankProfile&gt; because validation can fail
 */
@Service
@RequiredArgsConstructor
public class UpdateBankProfileHandler {
    
    private final BankProfileRepository repository;
    
    @Value
    public static class UpdateCommand {
        Long bankId;
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
        List<FieldError> validationErrors = new ArrayList<>();
        
        Result<LegalName> legalNameResult = LegalName.of(command.legalName);
        if (legalNameResult.isFailure()) {
            validationErrors.add(new FieldError("legalName", legalNameResult.getError().map(ErrorDetail::getMessage).orElse("Invalid legal name"), "bank.profile.legal.name.invalid"));
        }
        
        Result<AbiCode> abiCodeResult = AbiCode.of(command.abiCode);
        if (abiCodeResult.isFailure()) {
            validationErrors.add(new FieldError("abiCode", abiCodeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid ABI code"), "bank.profile.abi.code.invalid"));
        }
        
        Result<LeiCode> leiCodeResult = LeiCode.of(command.leiCode);
        if (leiCodeResult.isFailure()) {
            validationErrors.add(new FieldError("leiCode", leiCodeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid LEI code"), "bank.profile.lei.code.invalid"));
        }
        
        Result<GroupType> groupTypeResult = GroupType.of(command.groupType);
        if (groupTypeResult.isFailure()) {
            validationErrors.add(new FieldError("groupType", groupTypeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid group type"), "bank.profile.group.type.invalid"));
        }
        
        Result<BankType> bankTypeResult = BankType.of(command.bankType);
        if (bankTypeResult.isFailure()) {
            validationErrors.add(new FieldError("bankType", bankTypeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid bank type"), "bank.profile.bank.type.invalid"));
        }
        
        Result<SupervisionCategory> supervisionCategoryResult = SupervisionCategory.of(command.supervisionCategory);
        if (supervisionCategoryResult.isFailure()) {
            validationErrors.add(new FieldError("supervisionCategory", supervisionCategoryResult.getError().map(ErrorDetail::getMessage).orElse("Invalid supervision category"), "bank.profile.supervision.category.invalid"));
        }
        
        if (command.legalAddress == null || command.legalAddress.isBlank()) {
            validationErrors.add(new FieldError("legalAddress", "Legal address cannot be null or blank", "bank.profile.legal.address.required"));
        }
        
        // If any required field validation failed, return error
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }
        
        // Process optional fields (Maybe pattern)
        Maybe<VatNumber> vatNumber = VatNumber.of(command.vatNumber);
        Maybe<TaxCode> taxCode = TaxCode.of(command.taxCode);
        Maybe<String> companyRegistry = command.companyRegistry != null && !command.companyRegistry.isBlank()
                ? Maybe.some(command.companyRegistry.trim())
                : Maybe.none();
        Maybe<EmailAddress> institutionalEmail = EmailAddress.of(command.institutionalEmail);
        Maybe<EmailAddress> pec = EmailAddress.of(command.pec);
        Maybe<PhoneNumber> phone = PhoneNumber.of(command.phone);
        Maybe<WebsiteUrl> website = WebsiteUrl.of(command.website);
        
        // Get existing profile or create new
        Maybe<BankProfile> existingProfile = repository.findById(command.bankId);
        
        BankProfile profile;
        if (existingProfile.isPresent()) {
            // Update existing
            profile = existingProfile.getValue().update(
                    legalNameResult.getValueOrThrow(),
                    abiCodeResult.getValueOrThrow(),
                    leiCodeResult.getValueOrThrow(),
                    groupTypeResult.getValueOrThrow(),
                    bankTypeResult.getValueOrThrow(),
                    supervisionCategoryResult.getValueOrThrow(),
                    command.legalAddress.trim(),
                    vatNumber,
                    taxCode,
                    companyRegistry,
                    institutionalEmail,
                    pec,
                    phone,
                    website,
                    command.modifiedBy
            );
        } else {
            // Create new
            profile = BankProfile.builder()
                    .bankId(command.bankId)
                    .legalName(legalNameResult.getValueOrThrow())
                    .abiCode(abiCodeResult.getValueOrThrow())
                    .leiCode(leiCodeResult.getValueOrThrow())
                    .groupType(groupTypeResult.getValueOrThrow())
                    .bankType(bankTypeResult.getValueOrThrow())
                    .supervisionCategory(supervisionCategoryResult.getValueOrThrow())
                    .legalAddress(command.legalAddress.trim())
                    .vatNumber(vatNumber)
                    .taxCode(taxCode)
                    .companyRegistry(companyRegistry)
                    .institutionalEmail(institutionalEmail)
                    .pec(pec)
                    .phone(phone)
                    .website(website)
                    .lastModified(Instant.now())
                    .lastModifiedBy(command.modifiedBy)
                    .build();
        }
        
        BankProfile saved = repository.save(profile);
        
        return Result.success(saved);
    }
}
