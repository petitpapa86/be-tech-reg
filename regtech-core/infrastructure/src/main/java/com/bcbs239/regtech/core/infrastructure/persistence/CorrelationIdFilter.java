package com.bcbs239.regtech.core.infrastructure.persistence;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.infrastructure.systemservices.CorrelationId;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that ensures a correlation id is available for the whole HTTP request.
 * It binds the id into the ScopedValue-based CorrelationContext and also puts it
 * into MDC so logging and domain code can access the same correlation id.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = CorrelationId.generate().id();
        }

        // expose header for downstream services
        response.setHeader("X-Correlation-ID", correlationId);

        final String cid = correlationId;

        try {
            CorrelationContext.runWith(cid, null, () -> {
                MDC.put("correlationId", cid);
                try {
                    filterChain.doFilter(request, response);
                } catch (ServletException | IOException e) {
                    // Wrap checked exceptions so they can propagate out of the Runnable
                    throw new RuntimeException(e);
                } finally {
                    MDC.remove("correlationId");
                }
            });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServletException) {
                throw (ServletException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }
    }
}
