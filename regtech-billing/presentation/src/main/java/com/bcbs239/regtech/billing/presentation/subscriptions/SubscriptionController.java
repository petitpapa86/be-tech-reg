package com.bcbs239.regtech.billing.presentation.subscriptions;


import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for subscription management operations.
 * Handles subscription retrieval, cancellation, and status management.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController extends BaseController {
//
//    private final GetSubscriptionCommandHandler getSubscriptionCommandHandler;
//    private final CancelSubscriptionCommandHandler cancelSubscriptionCommandHandler;
//
//    public SubscriptionController(
//            GetSubscriptionCommandHandler getSubscriptionCommandHandler,
//            CancelSubscriptionCommandHandler cancelSubscriptionCommandHandler) {
//        this.getSubscriptionCommandHandler = getSubscriptionCommandHandler;
//        this.cancelSubscriptionCommandHandler = cancelSubscriptionCommandHandler;
//    }
//
//    /**
//     * Get subscription details by ID.
//     * Returns complete subscription information including status, tier, and billing details.
//     *
//     * @param subscriptionId The subscription ID
//     * @return ResponseEntity with GetSubscriptionResponse or error details
//     */
//    @GetMapping("/{subscriptionId}")
//    public ResponseEntity<? extends ApiResponse<?>> getSubscription(
//            @PathVariable String subscriptionId) {
//
//        try {
//            // Create and validate command
//            Result<GetSubscriptionCommand> commandResult = GetSubscriptionCommand.create(subscriptionId);
//
//            if (commandResult.isFailure()) {
//                return handleError(commandResult.getError().get());
//            }
//
//            // Execute command
//            Result<GetSubscriptionResponse> result = getSubscriptionCommandHandler.handle(
//                commandResult.getValue().get()
//            );
//
//            return handleResult(result,
//                "Subscription details retrieved successfully",
//                "subscription.details.retrieved");
//
//        } catch (Exception e) {
//            return handleSystemError(e);
//        }
//    }
//
//    /**
//     * Cancel a subscription.
//     * Cancels the subscription immediately or at the specified date.
//     *
//     * @param subscriptionId The subscription ID to cancel
//     * @param request The cancellation request (optional cancellation date)
//     * @return ResponseEntity with CancelSubscriptionResponse or error details
//     */
//    @PostMapping("/{subscriptionId}/cancel")
//    public ResponseEntity<? extends ApiResponse<?>> cancelSubscription(
//            @PathVariable String subscriptionId,
//            @Valid @RequestBody(required = false) CancelSubscriptionRequest request) {
//
//        try {
//            // Create and validate command
//            LocalDate cancellationDate = request != null ? request.cancellationDate() : null;
//            Result<CancelSubscriptionCommand> commandResult = CancelSubscriptionCommand.create(
//                subscriptionId,
//                cancellationDate
//            );
//
//            if (commandResult.isFailure()) {
//                return handleError(commandResult.getError().get());
//            }
//
//            // Execute command
//            Result<CancelSubscriptionResponse> result = cancelSubscriptionCommandHandler.handle(
//                commandResult.getValue().get()
//            );
//
//            return handleResult(result,
//                "Subscription cancelled successfully",
//                "subscription.cancelled");
//
//        } catch (Exception e) {
//            return handleSystemError(e);
//        }
//    }
//
//    /**
//     * Request DTO for subscription cancellation endpoint
//     */
//    public record CancelSubscriptionRequest(
//        LocalDate cancellationDate
//    ) {}
}
