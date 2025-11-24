package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.domain.banks.Bank;
import com.bcbs239.regtech.iam.domain.banks.BankName;
import com.bcbs239.regtech.iam.domain.banks.BankStatus;
import com.bcbs239.regtech.iam.domain.users.BankId;
import com.bcbs239.regtech.iam.infrastructure.database.entities.BankEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Bank domain objects and BankEntity database entities.
 */
@Component
public class BankMapper {
    
    /**
     * Convert a Bank domain object to a BankEntity.
     * 
     * @param bank the domain bank
     * @return the bank entity
     */
    public BankEntity toEntity(Bank bank) {
        if (bank == null) {
            return null;
        }
        
        BankEntity entity = new BankEntity(
            bank.getId().value(),
            bank.getName().value(),
            "IT", // Default country code - should be added to Bank domain
            bank.getStatus().name()
        );
        
        entity.setCreatedAt(bank.getCreatedAt());
        entity.setUpdatedAt(bank.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert a BankEntity to a Bank domain object.
     * 
     * @param entity the bank entity
     * @return the domain bank
     */
    public Bank toDomain(BankEntity entity) {
        if (entity == null) {
            return null;
        }
        
        BankId bankId = BankId.fromString(entity.getId()).getValue().get();
        BankName bankName = BankName.create(entity.getName()).getValue().get();
        BankStatus status = BankStatus.valueOf(entity.getStatus());
        
        return Bank.createFromPersistence(
            bankId,
            bankName,
            status,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
