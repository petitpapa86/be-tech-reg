package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.repositories.BillingAccountRepository;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.stereotype.Component;

@Component
public class FinalizeBillingAccountCommandHandler {

    private final BillingAccountRepository billingAccountRepository;

    public FinalizeBillingAccountCommandHandler(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    public Result<Void> handle(FinalizeBillingAccountCommand command) {
        String billingAccountIdStr = (String) command.payload().get("billingAccountId");
        BillingAccountId billingAccountId = BillingAccountId.fromString(billingAccountIdStr).getValue().get();
        
        Maybe<BillingAccount> maybeAccount = billingAccountRepository.billingAccountFinder().apply(billingAccountId);
        if (maybeAccount.isPresent()) {
            BillingAccount billingAccount = maybeAccount.getValue();
            billingAccount.finalizeAccount();
            Result<BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(billingAccount);
            if (saveResult.isSuccess()) {
                return Result.success(null);
            } else {
                return Result.failure(saveResult.getError().get());
            }
        } else {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", "Billing account not found: " + billingAccountIdStr, "billing.account.not.found"));
        }
    }
}
