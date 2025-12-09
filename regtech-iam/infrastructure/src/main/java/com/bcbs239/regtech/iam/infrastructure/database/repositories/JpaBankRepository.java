package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.banks.Bank;
import com.bcbs239.regtech.iam.domain.banks.BankStatus;
import com.bcbs239.regtech.iam.domain.banks.IBankRepository;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.infrastructure.database.entities.BankEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA implementation of the IBankRepository interface.
 */
@Repository
@Transactional
public class JpaBankRepository implements IBankRepository {
    
    private final SpringDataBankRepository springDataRepository;
    private final BankMapper mapper;
    
    public JpaBankRepository(SpringDataBankRepository springDataRepository, BankMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Result<BankId> save(Bank bank) {
        try {
            BankEntity entity = mapper.toEntity(bank);
            springDataRepository.save(entity);
            return Result.success(bank.getId());
        } catch (Exception e) {
            return Result.failure(com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                "BANK_SAVE_FAILED",
                com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                "Failed to save bank: " + e.getMessage(),
                "bank.save.failed"
            ));
        }
    }
    
    @Override
    public Maybe<Bank> findById(BankId id) {
        return springDataRepository.findById(id.value())
                .map(mapper::toDomain)
                .map(Maybe::some)
                .orElse(Maybe.none());
    }
    
    @Override
    public List<Bank> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Bank> findByStatus(BankStatus status) {
        return springDataRepository.findAll()
                .stream()
                .filter(entity -> entity.getStatus().equals(status.name()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(BankId id) {
        return springDataRepository.existsById(id.value());
    }
}
