package com.bcbs239.regtech.app.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts business context headers and attaches them to:
 * - MDC (so logs include bank/user/batch ids)
 * - current span tags (so traces carry the same context)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String HEADER_BANK_ID = "X-Bank-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_BATCH_ID = "X-Batch-Id";

    private static final String MDC_BANK_ID = "bank-id";
    private static final String MDC_USER_ID = "user-id";
    private static final String MDC_BATCH_ID = "batch-id";

    private final Tracer tracer;

    public RequestContextFilter(@Autowired(required = false) Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bankId = request.getHeader(HEADER_BANK_ID);
        String userId = request.getHeader(HEADER_USER_ID);
        String batchId = request.getHeader(HEADER_BATCH_ID);

        putIfPresent(MDC_BANK_ID, bankId);
        putIfPresent(MDC_USER_ID, userId);
        putIfPresent(MDC_BATCH_ID, batchId);

        // Add to current span as tags (so they show up in Tempo/OTel as attributes)
        Span span = tracer != null ? tracer.currentSpan() : null;
        if (span != null) {
            tagIfPresent(span, MDC_BANK_ID, bankId);
            tagIfPresent(span, MDC_USER_ID, userId);
            tagIfPresent(span, MDC_BATCH_ID, batchId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            removeIfPresent(MDC_BANK_ID, bankId);
            removeIfPresent(MDC_USER_ID, userId);
            removeIfPresent(MDC_BATCH_ID, batchId);
        }
    }

    private static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private static void removeIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.remove(key);
        }
    }

    private static void tagIfPresent(Span span, String key, String value) {
        if (value != null && !value.isBlank()) {
            span.tag(key, value);
        }
    }
}
