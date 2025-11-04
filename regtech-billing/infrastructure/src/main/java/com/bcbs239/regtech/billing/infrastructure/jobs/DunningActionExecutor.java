package com.bcbs239.regtech.billing.infrastructure.jobs;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.dunning.DunningStep;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.bcbs239.regtech.billing.domain.dunning.DunningStep.*;

/**
 * Service responsible for executing dunning actions based on the current dunning step.
 * Handles email notifications, account suspensions, and other collection activities.
 */
@Service
public class DunningActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DunningActionExecutor.class);

    private final JpaInvoiceRepository invoiceRepository;
    private final JpaBillingAccountRepository billingAccountRepository;
    private final DunningNotificationService notificationService;

    public DunningActionExecutor(
            JpaInvoiceRepository invoiceRepository,
            JpaBillingAccountRepository billingAccountRepository,
            DunningNotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.notificationService = notificationService;
    }

    /**
     * Execute the appropriate dunning action based on the current step.
     */
    public DunningProcessScheduler.DunningActionResult executeAction(
            DunningStep step,
            InvoiceId invoiceId, 
            BillingAccountId billingAccountId) {
        
        logger.info("Executing dunning action for step {} on invoice {} (account: {})", 
            step, invoiceId, billingAccountId);

        try {
            // Load required data
            Maybe<Invoice> invoiceMaybe = invoiceRepository.invoiceFinder().apply(invoiceId);
            Maybe<BillingAccount> accountMaybe = billingAccountRepository.billingAccountFinder().apply(billingAccountId);

            if (invoiceMaybe.isEmpty()) {
                return new DunningProcessScheduler.DunningActionResult(
                    "DATA_LOAD_FAILED", 
                    "Invoice not found: " + invoiceId, 
                    false
                );
            }

            if (accountMaybe.isEmpty()) {
                return new DunningProcessScheduler.DunningActionResult(
                    "DATA_LOAD_FAILED", 
                    "Billing account not found: " + billingAccountId, 
                    false
                );
            }

            Invoice invoice = invoiceMaybe.getValue();
            BillingAccount account = accountMaybe.getValue();

            // Execute step-specific action
            return switch (step) {
                case FIRST_REMINDER -> executeFirstReminder(invoice, account);
                case SECOND_REMINDER -> executeSecondReminder(invoice, account);
                case FINAL_NOTICE -> executeFinalNotice(invoice, account);
                case COLLECTION_AGENCY, LEGAL_ACTION -> executeAccountSuspension(invoice, account); // Reuse suspension logic
            };

        } catch (Exception e) {
            logger.error("Error executing dunning action for step {}: {}", step, e.getMessage(), e);
            return new DunningProcessScheduler.DunningActionResult(
                "EXECUTION_ERROR", 
                "Unexpected error: " + e.getMessage(), 
                false
            );
        }
    }

    /**
     * Execute first reminder - gentle payment reminder email.
     */
    private DunningProcessScheduler.DunningActionResult executeFirstReminder(Invoice invoice, BillingAccount account) {
        try {
            Map<String, Object> templateData = createEmailTemplateData(invoice, account);
            templateData.put("reminderType", "first");
            templateData.put("urgencyLevel", "low");
            templateData.put("subject", "Payment Reminder - Invoice " + invoice.getInvoiceNumber().getValue());

            boolean emailSent = notificationService.sendDunningEmail(
                account.getUserId(),
                "first-payment-reminder",
                templateData
            );

            if (emailSent) {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_SENT",
                    "First payment reminder sent successfully",
                    true
                );
            } else {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_FAILED",
                    "Failed to send first payment reminder",
                    false
                );
            }

        } catch (Exception e) {
            logger.error("Error executing first reminder: {}", e.getMessage(), e);
            return new DunningProcessScheduler.DunningActionResult(
                "EMAIL_ERROR",
                "Error sending first reminder: " + e.getMessage(),
                false
            );
        }
    }

    /**
     * Execute second reminder - more urgent payment reminder with late fee warning.
     */
    private DunningProcessScheduler.DunningActionResult executeSecondReminder(Invoice invoice, BillingAccount account) {
        try {
            Map<String, Object> templateData = createEmailTemplateData(invoice, account);
            templateData.put("reminderType", "second");
            templateData.put("urgencyLevel", "medium");
            templateData.put("subject", "Urgent: Payment Overdue - Invoice " + invoice.getInvoiceNumber().getValue());
            templateData.put("lateFeeWarning", true);

            boolean emailSent = notificationService.sendDunningEmail(
                account.getUserId(),
                "second-payment-reminder",
                templateData
            );

            if (emailSent) {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_SENT",
                    "Second payment reminder sent successfully",
                    true
                );
            } else {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_FAILED",
                    "Failed to send second payment reminder",
                    false
                );
            }

        } catch (Exception e) {
            logger.error("Error executing second reminder: {}", e.getMessage(), e);
            return new DunningProcessScheduler.DunningActionResult(
                "EMAIL_ERROR",
                "Error sending second reminder: " + e.getMessage(),
                false
            );
        }
    }

    /**
     * Execute final notice - final warning before account suspension.
     */
    private DunningProcessScheduler.DunningActionResult executeFinalNotice(Invoice invoice, BillingAccount account) {
        try {
            Map<String, Object> templateData = createEmailTemplateData(invoice, account);
            templateData.put("reminderType", "final");
            templateData.put("urgencyLevel", "high");
            templateData.put("subject", "FINAL NOTICE: Account Suspension Pending - Invoice " + invoice.getInvoiceNumber().getValue());
            templateData.put("suspensionWarning", true);
            templateData.put("suspensionDate", java.time.LocalDate.now().plusDays(7).toString());

            boolean emailSent = notificationService.sendDunningEmail(
                account.getUserId(),
                "final-payment-notice",
                templateData
            );

            if (emailSent) {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_SENT",
                    "Final payment notice sent successfully",
                    true
                );
            } else {
                return new DunningProcessScheduler.DunningActionResult(
                    "EMAIL_FAILED",
                    "Failed to send final payment notice",
                    false
                );
            }

        } catch (Exception e) {
            logger.error("Error executing final notice: {}", e.getMessage(), e);
            return new DunningProcessScheduler.DunningActionResult(
                "EMAIL_ERROR",
                "Error sending final notice: " + e.getMessage(),
                false
            );
        }
    }

    /**
     * Execute account suspension - suspend account access due to non-payment.
     */
    private DunningProcessScheduler.DunningActionResult executeAccountSuspension(Invoice invoice, BillingAccount account) {
        try {
            // First, send suspension notification email
            Map<String, Object> templateData = createEmailTemplateData(invoice, account);
            templateData.put("suspensionType", "executed");
            templateData.put("subject", "Account Suspended - Payment Required");
            templateData.put("suspensionDate", java.time.LocalDate.now().toString());

            boolean emailSent = notificationService.sendDunningEmail(
                account.getUserId(),
                "account-suspension-notice",
                templateData
            );

            // Suspend the billing account
            Result<Void> suspensionResult = account.suspend("Non-payment of invoice " + invoice.getInvoiceNumber().getValue());
            
            if (suspensionResult.isSuccess()) {
                // Save the suspended account
                Result<BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(account);
                
                if (saveResult.isSuccess()) {
                    String details = String.format("Account suspended successfully. Email notification: %s", 
                        emailSent ? "sent" : "failed");
                    
                    return new DunningProcessScheduler.DunningActionResult(
                        "ACCOUNT_SUSPENDED",
                        details,
                        true
                    );
                } else {
                    return new DunningProcessScheduler.DunningActionResult(
                        "SUSPENSION_SAVE_FAILED",
                        "Failed to save suspended account: " + saveResult.getError().get().getMessage(),
                        false
                    );
                }
            } else {
                return new DunningProcessScheduler.DunningActionResult(
                    "SUSPENSION_FAILED",
                    "Failed to suspend account: " + suspensionResult.getError().get().getMessage(),
                    false
                );
            }

        } catch (Exception e) {
            logger.error("Error executing account suspension: {}", e.getMessage(), e);
            return new DunningProcessScheduler.DunningActionResult(
                "SUSPENSION_ERROR",
                "Error executing account suspension: " + e.getMessage(),
                false
            );
        }
    }

    /**
     * Create common template data for dunning emails.
     */
    private Map<String, Object> createEmailTemplateData(Invoice invoice, BillingAccount account) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("invoiceId", invoice.getId().getValue());
        data.put("invoiceNumber", invoice.getInvoiceNumber().getValue());
        data.put("invoiceAmount", invoice.getTotalAmount().amount().toString());
        data.put("invoiceCurrency", invoice.getTotalAmount().currency().getCurrencyCode());
        data.put("dueDate", invoice.getDueDate().toString());
        data.put("daysPastDue", java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), java.time.LocalDate.now()));
        data.put("billingAccountId", account.getId().getValue());
        data.put("userId", account.getUserId().getValue());
        data.put("accountStatus", account.getStatus().name());
        data.put("currentDate", java.time.LocalDate.now().toString());
        data.put("timestamp", Instant.now().toString());
        
        return data;
    }

    /**
     * Check if an account should be exempted from dunning (e.g., VIP customers, special arrangements).
     * This is a placeholder for business logic that might exempt certain accounts.
     */
    private boolean isExemptFromDunning(BillingAccount account) {
        // Placeholder for exemption logic
        // Could check account flags, customer tier, special arrangements, etc.
        return false;
    }

    /**
     * Calculate late fees based on invoice amount and days overdue.
     * This is a placeholder for late fee calculation logic.
     */
    private double calculateLateFee(Invoice invoice) {
        // Placeholder for late fee calculation
        // Could be percentage of invoice amount, flat fee, or tiered based on days overdue
        long daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), java.time.LocalDate.now());
        
        if (daysPastDue <= 7) {
            return 0.0; // No late fee for first week
        } else if (daysPastDue <= 30) {
            return 25.0; // €25 late fee for 1-4 weeks overdue
        } else {
            return 50.0; // €50 late fee for over 30 days
        }
    }
}
