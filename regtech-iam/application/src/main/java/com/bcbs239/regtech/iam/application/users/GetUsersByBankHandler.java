package com.bcbs239.regtech.iam.application.users;

import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Handler: Get users by bank
 * 
 * Uses EXISTING User domain model with proper value object validation
 */
@Service
@RequiredArgsConstructor
public class GetUsersByBankHandler {
    
    private final UserRepository userRepository;
    
    @Value
    public static class Query {
        String bankId;
        String filter; // "all", "active", "pending"
    }
    
    @Transactional(readOnly = true)
    public Result<List<User>> handle(Query query) {
        List<FieldError> validationErrors = new ArrayList<>();
        
        // 1. Validate and create BankId value object with numeric validation
        Result<BankId> bankIdResult = BankId.createNumeric(query.bankId);

        if (bankIdResult.isFailure()) {
            validationErrors.add(new FieldError("bankId", bankIdResult.getError().get().getMessage(), bankIdResult.getError().get().getMessageKey()));
        }
        
        // 2. Validate filter parameter
        String filter = query.filter != null ? query.filter.toLowerCase() : "all";
        if (!List.of("all", "active", "pending").contains(filter)) {
            validationErrors.add(new FieldError("filter", "Filter must be one of: all, active, pending", "validation.invalid_filter"));
        }

        // Return validation errors if any
        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
        }

        BankId bankId = bankIdResult.getValue().get();
        Long bankIdLong = bankId.getAsLong();
        List<User> users = switch (filter) {
            case "active" -> userRepository.findActiveByBankId(bankIdLong);
            case "pending" -> userRepository.findPendingByBankId(bankIdLong);
            default -> userRepository.findByBankId(bankIdLong);
        };
        
        return Result.success(users);
    }
}
