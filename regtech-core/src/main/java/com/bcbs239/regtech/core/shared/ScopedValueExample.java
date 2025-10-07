package com.bcbs239.regtech.core.shared;

import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating ScopedValue usage in Java 25.
 * ScopedValue is ideal for passing immutable context through method calls
 * without the threading issues of ThreadLocal.
 */
public class ScopedValueExample {

    // Define a ScopedValue for user context
    private static final ScopedValue<String> USER_CONTEXT = ScopedValue.newInstance();

    public static void demonstrateScopedValue() {
        // Set the context and run operations within that scope
        ScopedValue.where(USER_CONTEXT, "user123").run(() -> {
            System.out.println("User context: " + getCurrentUser());

            // Context is available in child methods
            processUserData();

            // Context is available in async operations
            CompletableFuture.runAsync(() -> {
                System.out.println("Async user context: " + getCurrentUser());
            }).join();
        });

        // Outside the scope, context is not available
        System.out.println("Outside scope: " + USER_CONTEXT.orElse("no context"));
    }

    private static void processUserData() {
        System.out.println("Processing data for user: " + getCurrentUser());
    }

    private static String getCurrentUser() {
        return USER_CONTEXT.orElse("anonymous");
    }
}