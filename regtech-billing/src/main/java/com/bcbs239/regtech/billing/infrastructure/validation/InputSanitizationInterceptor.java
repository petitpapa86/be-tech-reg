package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * Interceptor that performs additional input validation and sanitization
 * for incoming HTTP requests to billing endpoints.
 */
public class InputSanitizationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationInterceptor.class);

    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|<script|</script|<iframe|</iframe|<object|</object|<embed|</embed)"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(javascript:|vbscript:|data:text/html|onload=|onerror=|onclick=|onmouseover=|onfocus=|onblur=|onchange=|onsubmit=)"
    );

    private final InputSanitizer inputSanitizer;

    public InputSanitizationInterceptor(InputSanitizer inputSanitizer) {
        this.inputSanitizer = inputSanitizer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Check request parameters
            if (request.getParameterMap() != null) {
                for (String paramName : request.getParameterMap().keySet()) {
                    String[] paramValues = request.getParameterValues(paramName);
                    if (paramValues != null) {
                        for (String paramValue : paramValues) {
                            if (containsSuspiciousContent(paramValue)) {
                                logger.warn("Suspicious content detected in parameter '{}': {}", paramName, paramValue);
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                return false;
                            }
                        }
                    }
                }
            }

            // Check headers for suspicious content
            if (request.getHeaderNames() != null) {
                var headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    
                    // Skip common headers that might contain legitimate special characters
                    if (isSkippableHeader(headerName)) {
                        continue;
                    }
                    
                    if (containsSuspiciousContent(headerValue)) {
                        logger.warn("Suspicious content detected in header '{}': {}", headerName, headerValue);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return false;
                    }
                }
            }

            // Check URL path for suspicious content
            String requestURI = request.getRequestURI();
            if (containsSuspiciousContent(requestURI)) {
                logger.warn("Suspicious content detected in URL path: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }

            // Check query string
            String queryString = request.getQueryString();
            if (queryString != null && containsSuspiciousContent(queryString)) {
                logger.warn("Suspicious content detected in query string: {}", queryString);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Error during input sanitization check", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    private boolean containsSuspiciousContent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Check for SQL injection patterns
        if (SUSPICIOUS_PATTERN.matcher(input).find()) {
            return true;
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(input).find()) {
            return true;
        }

        // Check for null bytes
        if (input.contains("\0")) {
            return true;
        }

        // Check for excessive length (potential DoS)
        if (input.length() > 10000) {
            return true;
        }

        return false;
    }

    private boolean isSkippableHeader(String headerName) {
        if (headerName == null) {
            return true;
        }
        
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.equals("authorization") ||
               lowerHeaderName.equals("user-agent") ||
               lowerHeaderName.equals("accept") ||
               lowerHeaderName.equals("accept-encoding") ||
               lowerHeaderName.equals("accept-language") ||
               lowerHeaderName.equals("cache-control") ||
               lowerHeaderName.equals("connection") ||
               lowerHeaderName.equals("content-type") ||
               lowerHeaderName.equals("content-length") ||
               lowerHeaderName.equals("host") ||
               lowerHeaderName.equals("referer") ||
               lowerHeaderName.equals("x-forwarded-for") ||
               lowerHeaderName.equals("x-real-ip");
    }
}