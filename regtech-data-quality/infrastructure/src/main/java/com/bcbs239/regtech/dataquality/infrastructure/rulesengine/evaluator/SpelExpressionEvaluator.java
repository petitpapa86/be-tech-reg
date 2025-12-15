package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluationException;
import com.bcbs239.regtech.dataquality.rulesengine.evaluator.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Expression Language (SpEL) implementation of ExpressionEvaluator.
 * 
 * <p>Uses Spring's SpEL for flexible, powerful expression evaluation.</p>
 */
@Slf4j
@Component
public class SpelExpressionEvaluator implements ExpressionEvaluator {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Override
    public boolean evaluateBoolean(String expression, RuleContext context) {
        try {
            Object result = evaluate(expression, context);
            
            if (result instanceof Boolean boolResult) {
                return boolResult;
            }
            
            // Convert truthy values
            if (result == null) {
                return false;
            }
            
            if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0.0;
            }
            
            if (result instanceof String) {
                return !((String) result).isEmpty();
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to evaluate boolean expression: {}", expression, e);
            throw new ExpressionEvaluationException(
                "Failed to evaluate expression as boolean: " + expression, e);
        }
    }
    
    @Override
    public Object evaluate(String expression, RuleContext context) {
        try {
            Expression expr = parser.parseExpression(expression);
            EvaluationContext evalContext = createEvaluationContext(context);
            return expr.getValue(evalContext);
            
        } catch (Exception e) {
            log.error("Failed to evaluate expression: {}", expression, e);
            throw new ExpressionEvaluationException(
                "Failed to evaluate expression: " + expression, e);
        }
    }
    
    @Override
    public <T> T evaluate(String expression, RuleContext context, Class<T> resultType) {
        try {
            Expression expr = parser.parseExpression(expression);
            EvaluationContext evalContext = createEvaluationContext(context);
            return expr.getValue(evalContext, resultType);
            
        } catch (Exception e) {
            log.error("Failed to evaluate expression with type {}: {}", resultType.getName(), expression, e);
            throw new ExpressionEvaluationException(
                "Failed to evaluate expression with type " + resultType.getName() + ": " + expression, e);
        }
    }
    
    @Override
    public boolean isValidExpression(String expression) {
        try {
            parser.parseExpression(expression);
            return true;
        } catch (Exception e) {
            log.debug("Invalid expression: {}", expression, e);
            return false;
        }
    }
    
    /**
     * Creates a SpEL evaluation context from a rule context.
     * 
     * @param context The rule context
     * @return SpEL evaluation context
     */
    private EvaluationContext createEvaluationContext(RuleContext context) {
        AliasAwareEvaluationContext evalContext = new AliasAwareEvaluationContext(context);
        
        // Register custom functions if needed
        registerCustomFunctions(evalContext);
        
        return evalContext;
    }

    /**
     * Evaluation context that resolves variables in a tolerant way:
     * - Case-insensitive
     * - Underscore-insensitive (snake_case vs camelCase)
     *
     * This avoids duplicating variables (and Map entries) in the rule context.
     */
    private static final class AliasAwareEvaluationContext extends StandardEvaluationContext {

        private final Map<String, Object> normalizedVariables;

        private AliasAwareEvaluationContext(RuleContext ruleContext) {
            this.normalizedVariables = new HashMap<>();

            if (ruleContext == null || ruleContext.getAllData() == null) {
                return;
            }

            // Register original variables (exact matches)
            ruleContext.getAllData().forEach(this::setVariable);

            // Build a normalized index for tolerant lookups
            for (Map.Entry<String, Object> entry : ruleContext.getAllData().entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (entry.getValue() == null) {
                    continue;
                }
                String normalizedKey = normalizeVariableName(entry.getKey());
                // Preserve first-seen to keep behavior deterministic if collisions exist.
                normalizedVariables.putIfAbsent(normalizedKey, entry.getValue());
            }
        }

        @Override
        public Object lookupVariable(String name) {
            Object direct = super.lookupVariable(name);
            if (direct != null || name == null) {
                return direct;
            }

            return normalizedVariables.get(normalizeVariableName(name));
        }

        private static String normalizeVariableName(String name) {
            // Normalize variable names so exposure_id, exposureId, exposure_Id all match.
            // Keep only letters/digits and lowercase.
            StringBuilder sb = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(Character.toLowerCase(c));
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * Registers custom functions for use in expressions.
     * 
     * @param context The evaluation context
     */
    private void registerCustomFunctions(StandardEvaluationContext context) {
        try {
            // Register custom utility functions
            context.registerFunction("DAYS_BETWEEN", 
                SpelExpressionEvaluator.class.getDeclaredMethod("daysBetween", 
                    java.time.LocalDate.class, java.time.LocalDate.class));
            
            context.registerFunction("NOW", 
                SpelExpressionEvaluator.class.getDeclaredMethod("now"));
            
            context.registerFunction("TODAY", 
                SpelExpressionEvaluator.class.getDeclaredMethod("today"));
                
        } catch (NoSuchMethodException e) {
            log.warn("Failed to register custom functions", e);
        }
    }
    
    // ====================================================================
    // Custom Functions for SpEL
    // ====================================================================
    
    /**
     * Calculates days between two dates.
     */
    public static long daysBetween(java.time.LocalDate start, java.time.LocalDate end) {
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * Gets the current instant.
     */
    public static java.time.Instant now() {
        return java.time.Instant.now();
    }
    
    /**
     * Gets today's date.
     */
    public static java.time.LocalDate today() {
        return java.time.LocalDate.now();
    }
}

