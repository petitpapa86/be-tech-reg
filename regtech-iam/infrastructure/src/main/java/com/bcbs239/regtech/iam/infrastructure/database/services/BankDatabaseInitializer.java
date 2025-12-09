package com.bcbs239.regtech.iam.infrastructure.database.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.banks.Bank;
import com.bcbs239.regtech.iam.domain.banks.IBankRepository;
import com.bcbs239.regtech.iam.domain.users.BankId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * Initializes the database with default banks on application startup.
 * This service ensures that essential banks are available for user authentication.
 */
@Service
public class BankDatabaseInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(BankDatabaseInitializer.class);
    
    private final IBankRepository bankRepository;
    
    public BankDatabaseInitializer(IBankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database with default banks...");
        
        initializeBanks();
        
        logger.info("Database initialization completed successfully");
    }
    
    private void initializeBanks() {
        // Initialize Italian banks
        createBankIfNotExists("Intesa Sanpaolo");
        createBankIfNotExists("UniCredit");
        
        // Initialize other European banks for testing
        createBankIfNotExists("Deutsche Bank");
        createBankIfNotExists("BNP Paribas");
        createBankIfNotExists("Santander");
    }
    
    private void createBankIfNotExists(String bankName) {
        try {
            // Generate a new bank ID
            BankId bankId = BankId.generate();
            
            // Check if bank already exists by checking all banks
            boolean exists = bankRepository.findAll().stream()
                .anyMatch(bank -> bank.getName().value().equals(bankName));
            
            if (!exists) {
                Result<Bank> bankResult = Bank.create(bankId, bankName);
                if (bankResult.isSuccess()) {
                    Bank bank = bankResult.getValue().get();
                    Result<BankId> saveResult = bankRepository.save(bank);
                    if (saveResult.isSuccess()) {
                        logger.info("Created bank: {}", bankName);
                    } else {
                        logger.error("Failed to save bank: {}. Error: {}", 
                            bankName, saveResult.getError().get().getMessage());
                    }
                } else {
                    logger.error("Failed to create bank: {}. Error: {}", 
                        bankName, bankResult.getError().get().getMessage());
                }
            } else {
                logger.debug("Bank already exists: {}", bankName);
            }
        } catch (Exception e) {
            logger.error("Failed to create bank: {}. Error: {}", bankName, e.getMessage());
        }
    }
}
