package com.bcbs239.regtech.core.config;

import com.bcbs239.regtech.core.shared.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class LoggingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CorrelationIdInterceptor());
    }

    private static class CorrelationIdInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = CorrelationId.generate().getId();
            }
            MDC.put("correlationId", correlationId);
            response.setHeader("X-Correlation-ID", correlationId);
            logger.debug("Set correlation ID: {}", correlationId);
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            MDC.remove("correlationId");
        }
    }

    /**
     * Get the current correlation ID from MDC
     */
    public static String getCurrentCorrelationId() {
        return MDC.get("correlationId");
    }
}