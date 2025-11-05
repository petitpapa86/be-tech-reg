package com.bcbs239.regtech.core.capabilities.sharedinfrastructure;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Utility class for setting router function attributes consistently.
 * Provides helper methods for common attributes like permissions, tags, etc.
 */
public class RouterAttributes {

    public static final String PERMISSIONS_ATTR = "permissions";
    public static final String TAGS_ATTR = "tags";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String PUBLIC_ATTR = "public";

    /**
     * Set required permissions for a router function.
     */
    public static RouterFunction<ServerResponse> withPermissions(
            RouterFunction<ServerResponse> routerFunction, String... permissions) {
        return routerFunction.withAttribute(PERMISSIONS_ATTR, permissions);
    }

    /**
     * Set tags for API documentation.
     */
    public static RouterFunction<ServerResponse> withTags(
            RouterFunction<ServerResponse> routerFunction, String... tags) {
        return routerFunction.withAttribute(TAGS_ATTR, tags);
    }

    /**
     * Set description for API documentation.
     */
    public static RouterFunction<ServerResponse> withDescription(
            RouterFunction<ServerResponse> routerFunction, String description) {
        return routerFunction.withAttribute(DESCRIPTION_ATTR, description);
    }

    /**
     * Mark endpoint as public (no authentication required).
     */
    public static RouterFunction<ServerResponse> asPublic(
            RouterFunction<ServerResponse> routerFunction) {
        return routerFunction.withAttribute(PUBLIC_ATTR, true);
    }

    /**
     * Set multiple attributes at once.
     */
    public static RouterFunction<ServerResponse> withAttributes(
            RouterFunction<ServerResponse> routerFunction,
            String[] permissions,
            String[] tags,
            String description) {
        
        RouterFunction<ServerResponse> result = routerFunction;
        
        if (permissions != null && permissions.length > 0) {
            result = result.withAttribute(PERMISSIONS_ATTR, permissions);
        }
        
        if (tags != null && tags.length > 0) {
            result = result.withAttribute(TAGS_ATTR, tags);
        }
        
        if (description != null) {
            result = result.withAttribute(DESCRIPTION_ATTR, description);
        }
        
        return result;
    }
}

