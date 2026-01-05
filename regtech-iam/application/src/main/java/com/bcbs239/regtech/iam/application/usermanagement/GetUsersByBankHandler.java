package com.bcbs239.regtech.iam.application.usermanagement;

import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Query Handler: Get users by bank
 * 
 * Uses EXISTING User domain model
 */
@Service
@RequiredArgsConstructor
public class GetUsersByBankHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Query {
        Long bankId;
        String filter; // "all", "active", "pending"
    }
    
    @Transactional(readOnly = true)
    public Result<List<User>> handle(Query query) {
        if (query.bankId == null || query.bankId <= 0) {
            return Result.failure(
                ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR, 
                    "Invalid bank ID", "usermanagement.invalid.bank.id")
            );
        }
        
        List<User> users = switch (query.filter.toLowerCase()) {
            case "active" -> userRepository.findActiveByBankId(query.bankId);
            case "pending" -> userRepository.findPendingByBankId(query.bankId);
            default -> userRepository.findByBankId(query.bankId);
        };
        
        return Result.success(users);
    }
}
