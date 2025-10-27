package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.application.commands.FinalizeBillingAccountCommand;
import com.bcbs239.regtech.billing.domain.model.BillingAccount;
import com.bcbs239.regtech.billing.domain.model.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.model.BillingAccountStatus;
import com.bcbs239.regtech.core.saga.CommandHandler;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.stereotype.Component;

@Component
public class FinalizeBillingAccountCommandHandler implements CommandHandler<FinalizeBillingAccountCommand> {

    private final BillingAccountRepository billingAccountRepository;

    public FinalizeBillingAccountCommandHandler(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    @Override
    public Result<Void> handle(FinalizeBillingAccountCommand command) {
        return billingAccountRepository.findById(command.billingAccountId())
            .map(billingAccount -> {
                BillingAccount finalizedAccount = billingAccount.finalize();
                billingAccountRepository.save(finalizedAccount);
                return Result.success(null);
            })
            .orElse(Result.failure("Billing account not found: " + command.billingAccountId()));
    }
}
