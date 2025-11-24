package com.bcbs239.regtech.iam.domain.banks;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.BankId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Bank Aggregate - represents a financial institution in the system
 * Requirements: 13.1, 13.2, 13.3, 13.4, 13.5
 */
@Getter
public class Bank extends Entity {
    private final BankId id;
    private BankName name;
    private BankStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private Bank(
        BankId id,
        BankName name,
        BankStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Factory method to create a new bank
     */
    public static Result<Bank> create(BankId id, String name) {
        // Validate name
        Result<BankName> nameResult = BankName.create(name);
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.getError().get());
        }
        
        Instant now = Instant.now();
        return Result.success(new Bank(
            id,
            nameResult.getValue().get(),
            BankStatus.ACTIVE,
            now,
            now
        ));
    }
    
    /**
     * Factory method for persistence layer reconstruction
     */
    public static Bank createFromPersistence(
        BankId id,
        BankName name,
        BankStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new Bank(id, name, status, createdAt, updatedAt);
    }
    
    /**
     * Activates the bank
     */
    public Result<Void> activate() {
        if (this.status == BankStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of(
                "BANK_ALREADY_ACTIVE",
                ErrorType.BUSINESS_RULE_ERROR,
                "Bank is already active",
                "bank.already_active"
            ));
        }
        
        this.status = BankStatus.ACTIVE;
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    /**
     * Deactivates the bank
     */
    public Result<Void> deactivate() {
        if (this.status == BankStatus.INACTIVE) {
            return Result.failure(ErrorDetail.of(
                "BANK_ALREADY_INACTIVE",
                ErrorType.BUSINESS_RULE_ERROR,
                "Bank is already inactive",
                "bank.already_inactive"
            ));
        }
        
        this.status = BankStatus.INACTIVE;
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    /**
     * Checks if the bank is active
     */
    public boolean isActive() {
        return this.status == BankStatus.ACTIVE;
    }
    
    /**
     * Updates the bank name
     */
    public Result<Void> updateName(String newName) {
        Result<BankName> nameResult = BankName.create(newName);
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.getError().get());
        }
        
        this.name = nameResult.getValue().get();
        this.updatedAt = Instant.now();
        return Result.success(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bank bank = (Bank) o;
        return Objects.equals(id, bank.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Bank{" +
                "id=" + id +
                ", name=" + name +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
