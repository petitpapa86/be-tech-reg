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
    public Maybe<BankProfile> findById(Long bankId) {
        return jpaRepository.findByBankId(bankId)
                .map(this::toDomain)
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
     */
    private BankProfile toDomain(BankProfileJpaEntity entity) {
        return BankProfile.builder()
                .bankId(entity.getBankId())
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
                .lastModified(entity.getLastModified())
                .lastModifiedBy(entity.getLastModifiedBy())
                .build();
    }
    
    /**
     * Domain → JPA
     */
    private BankProfileJpaEntity toEntity(BankProfile profile) {
        return BankProfileJpaEntity.builder()
                .bankId(profile.getBankId())
                .legalName(profile.getLegalName().getValue())
                .abiCode(profile.getAbiCode().getValue())
                .leiCode(profile.getLeiCode().getValue())
                .groupType(profile.getGroupType().name())
                .bankType(profile.getBankType().name())
                .supervisionCategory(profile.getSupervisionCategory().name())
                .legalAddress(profile.getLegalAddress())
                .vatNumber(profile.getVatNumber().map(v -> v.getValue()).orElse(null))
                .taxCode(profile.getTaxCode().map(t -> t.getValue()).orElse(null))
                .companyRegistry(profile.getCompanyRegistry().orElse(null))
                .institutionalEmail(profile.getInstitutionalEmail().map(e -> e.getValue()).orElse(null))
                .pec(profile.getPec().map(p -> p.getValue()).orElse(null))
                .phone(profile.getPhone().map(p -> p.getValue()).orElse(null))
                .website(profile.getWebsite().map(w -> w.getValue()).orElse(null))
                .lastModified(profile.getLastModified())
                .lastModifiedBy(profile.getLastModifiedBy())
                .build();
    }
}
