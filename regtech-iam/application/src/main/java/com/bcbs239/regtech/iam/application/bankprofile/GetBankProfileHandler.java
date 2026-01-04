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
 * Application layer = Coordinator
 * Orchestrates the query use case
 */
@Service
@RequiredArgsConstructor
public class GetBankProfileHandler {
    
    private final BankProfileRepository repository;
    
    @Transactional(readOnly = true)
    public Maybe<BankProfile> handle(Long bankId) {
        return repository.findById(bankId);
    }
}
