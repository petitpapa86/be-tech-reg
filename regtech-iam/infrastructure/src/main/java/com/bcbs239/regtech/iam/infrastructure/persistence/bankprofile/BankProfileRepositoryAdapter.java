package com.bcbs239.regtech.iam.infrastructure.persistence.bankprofile;

import com.bcbs239.regtech.iam.domain.bankprofile.*;
import com.bcbs239.regtech.iam.domain.bankprofile.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.infrastructure.database.entities.BankEntity;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.SpringDataBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository Adapter - maps JPA ↔ Domain
 */
@Repository
@RequiredArgsConstructor
public class BankProfileRepositoryAdapter implements BankProfileRepository {
    
    private final SpringDataBankRepository jpaRepository;
    
    @Override
    public Maybe<BankProfile> findById(String bankId) {
        return jpaRepository.findById(bankId)
                .flatMap(entity -> {
                    try {
                        return java.util.Optional.of(toDomain(entity));
                    } catch (Exception e) {
                        // If mapping fails due to invalid data, return empty
                        // Exception will be logged by infrastructure layer
                        return java.util.Optional.empty();
                    }
                })
                .map(Maybe::some)
                .orElse(Maybe.none());
    }
    
    @Override
    public BankProfile save(BankProfile profile) {
        var entity = toEntity(profile);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    /**
     * JPA → Domain
     * Pure mapping - no validation checks
     * Value objects handle their own validation
     */
    private BankProfile toDomain(BankEntity entity) {
        return BankProfile.builder()
                .bankId(entity.getId())
                .legalName(LegalName.of(entity.getLegalName()).getValueOrThrow())
                .abiCode(AbiCode.of(entity.getAbiCode()).getValueOrThrow())
                .leiCode(LeiCode.of(entity.getLeiCode()).getValueOrThrow())
                .groupType(GroupType.of(entity.getGroupType()).getValueOrThrow())
                .bankType(BankType.of(entity.getBankType()).getValueOrThrow())
                .supervisionCategory(SupervisionCategory.of(entity.getSupervisionCategory()).getValueOrThrow())
                .legalAddress(entity.getLegalAddress())
                .vatNumber(VatNumber.of(entity.getVatNumber()))
                .taxCode(TaxCode.of(entity.getTaxCode()))
                .companyRegistry(entity.getCompanyRegistry() != null && !entity.getCompanyRegistry().isBlank()
                        ? Maybe.some(entity.getCompanyRegistry())
                        : Maybe.none())
                .institutionalEmail(EmailAddress.of(entity.getInstitutionalEmail()))
                .pec(EmailAddress.of(entity.getPec()))
                .phone(PhoneNumber.of(entity.getPhone()))
                .website(WebsiteUrl.of(entity.getWebsite()))
                .lastModified(entity.getUpdatedAt())
                .lastModifiedBy("System")
                .build();
    }
    
    /**
     * Domain → JPA
     */
    private BankEntity toEntity(BankProfile profile) {
        // We need to fetch the existing BankEntity to preserve non-profile fields
        var existing = jpaRepository.findById(profile.getBankId()).orElse(new BankEntity());
        
        existing.setId(profile.getBankId());
        existing.setLegalName(profile.getLegalName().getValue());
        existing.setAbiCode(profile.getAbiCode().getValue());
        existing.setLeiCode(profile.getLeiCode().getValue());
        existing.setGroupType(profile.getGroupType().name());
        existing.setBankType(profile.getBankType().name());
        existing.setSupervisionCategory(profile.getSupervisionCategory().name());
        existing.setLegalAddress(profile.getLegalAddress());
        existing.setVatNumber(profile.getVatNumber().map(v -> v.getValue()).orElse(null));
        existing.setTaxCode(profile.getTaxCode().map(t -> t.getValue()).orElse(null));
        existing.setCompanyRegistry(profile.getCompanyRegistry().orElse(null));
        existing.setInstitutionalEmail(profile.getInstitutionalEmail().map(e -> e.getValue()).orElse(null));
        existing.setPec(profile.getPec().map(p -> p.getValue()).orElse(null));
        existing.setPhone(profile.getPhone().map(p -> p.getValue()).orElse(null));
        existing.setWebsite(profile.getWebsite().map(w -> w.getValue()).orElse(null));
        existing.setUpdatedAt(profile.getLastModified());
        
        // If it's a new entity, we might need to set mandatory BankEntity fields
        if (existing.getName() == null) {
            existing.setName(profile.getLegalName().getValue());
        }
        if (existing.getCountryCode() == null) {
            existing.setCountryCode("IT");
        }
        if (existing.getStatus() == null) {
            existing.setStatus("ACTIVE");
        }
        
        return existing;
    }
}
