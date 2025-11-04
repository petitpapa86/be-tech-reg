package com.bcbs239.regtech.billing.presentation.webhooks;


import com.bcbs239.regtech.billing.application.integration.ProcessWebhookCommandHandler;
import com.bcbs239.regtech.billing.domain.shared.validation.BillingValidationUtils;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling Stripe webhook events.
 * Processes webhook events with signature verification and idempotency handling.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class WebhookController extends BaseController {

//    private final ProcessWebhookCommandHandler processWebhookCommandHandler;
//    private final ObjectMapper objectMapper;
//
//    public WebhookController(
//            ProcessWebhookCommandHandler processWebhookCommandHandler,
//            ObjectMapper objectMapper) {
//        this.processWebhookCommandHandler = processWebhookCommandHandler;
//        this.objectMapper = objectMapper;
//    }
//
//    /**
//     * Process Stripe webhook events.
//     * Verifies webhook signature, checks for idempotency, and processes the event.
//     *
//     * @param payload The raw webhook payload
//     * @param signatureHeader The Stripe signature header for verification
//     * @return ResponseEntity with ProcessWebhookResponse or error details
//     */
//    @PostMapping
//    public ResponseEntity<? extends ApiResponse<?>> processStripeWebhook(
//            @RequestBody String payload,
//            @RequestHeader("Stripe-Signature") String signatureHeader) {
//
//        try {
//            // Pre-validate webhook signature format
//            Result<Void> signatureValidation = BillingValidationUtils.validateWebhookSignature(signatureHeader);
//            if (signatureValidation.isFailure()) {
//                return handleError(signatureValidation.getError().get());
//            }
//
//            // Pre-validate webhook payload structure
//            Result<JsonNode> payloadValidation = BillingValidationUtils.validateWebhookPayload(payload);
//            if (payloadValidation.isFailure()) {
//                return handleError(payloadValidation.getError().get());
//            }
//
//            JsonNode eventJson = payloadValidation.getValue().get();
//            String eventId = eventJson.get("id").asText();
//            String eventType = eventJson.get("type").asText();
//
//            // Check if event type is supported
//            if (!BillingValidationUtils.isSupportedWebhookEvent(eventType)) {
//                return ResponseEntity.ok(
//                        ApiResponse.success(null, "Webhook event type not supported: " + eventType)
//                );
//            }
//
//            // Create and validate command
//            Result<ProcessWebhookCommand> commandResult = ProcessWebhookCommand.create(
//                eventId,
//                eventType,
//                payload,
//                signatureHeader
//            );
//
//            if (commandResult.isFailure()) {
//                return handleError(commandResult.getError().get());
//            }
//
//            // Execute command
//            Result<ProcessWebhookResponse> result = processWebhookCommandHandler.handle(
//                commandResult.getValue().get()
//            );
//
//            if (result.isSuccess()) {
//                ProcessWebhookResponse response = result.getValue().get();
//
//                // Return appropriate HTTP status based on processing result
//                return switch (response.result()) {
//                    case SUCCESS -> ResponseEntity.ok(
//                        ApiResponse.success(response, "Webhook processed successfully")
//                    );
//                    case ALREADY_PROCESSED -> ResponseEntity.ok(
//                        ApiResponse.success(response, "Webhook was already processed")
//                    );
//                    case IGNORED -> ResponseEntity.ok(
//                        ApiResponse.success(response, "Webhook event ignored")
//                    );
//                    case FAILED -> ResponseEntity.badRequest().body(
//                        ApiResponse.businessRuleError("Webhook processing failed: " + response.message())
//                    );
//                };
//            } else {
//                return handleError(result.getError().get());
//            }
//
//        } catch (Exception e) {
//            return handleSystemError(e);
//        }
//    }
//
//    /**
//     * Health check endpoint for webhook endpoint
//     */
//    @GetMapping("/stripe/health")
//    public ResponseEntity<? extends ApiResponse<?>> webhookHealth() {
//        return ResponseEntity.ok(
//            ApiResponse.success("Webhook endpoint is healthy", "Stripe webhook endpoint is operational")
//        );
//    }
}
