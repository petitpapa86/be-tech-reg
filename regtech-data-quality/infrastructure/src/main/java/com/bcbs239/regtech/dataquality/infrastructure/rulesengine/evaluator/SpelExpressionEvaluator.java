package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring Expression Language (SpEL) implementation of ExpressionEvaluator.
 * 
 * <p>Uses Spring's SpEL for flexible, powerful expression evaluation.</p>
 */
@Slf4j
@Component
public class SpelExpressionEvaluator implements ExpressionEvaluator {
    
    private final ExpressionParser parser = new SpelExpressionParser();

    // Matches SpEL variable references like #exposureId, #product_type, etc.
    private static final Pattern SPEL_VARIABLE = Pattern.compile("#([A-Za-z_][A-Za-z0-9_]*)");

    // SpEL built-in context variables (should not be rewritten).
    private static final Set<String> RESERVED_VARIABLES = Set.of("root", "this");
    
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
           // String rewritten = rewriteVariableAliases(expression, context);
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
          //  String rewritten = rewriteVariableAliases(expression, context);
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
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        if (context != null && context.getAllData() != null) {
            // Register the exact context keys as variables.
            // Alias support is handled by rewriting the expression, not by mutating the RuleContext.
            context.getAllData().forEach(evalContext::setVariable);
        }

        registerCustomFunctions(evalContext);
        return evalContext;
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

    private static String rewriteVariableAliases(String expression, RuleContext context) {
        if (expression == null || context == null) {
            return expression;
        }

        Map<String, Object> data = context.getAllData();
        if (data == null || data.isEmpty()) {
            return expression;
        }

        Matcher matcher = SPEL_VARIABLE.matcher(expression);
        StringBuffer out = new StringBuffer(expression.length());

        while (matcher.find()) {
            String varName = matcher.group(1);
            if (varName == null || varName.isBlank()) {
                continue;
            }

            String lower = varName.toLowerCase();
            if (RESERVED_VARIABLES.contains(lower)) {
                continue;
            }

            String normalized = toSnakeLower(varName);
            if (normalized.equals(varName)) {
                continue;
            }

            // Only rewrite when it maps to an existing snake_case key and the original key doesn't exist.
            if (data.containsKey(normalized) && !data.containsKey(varName)) {
                matcher.appendReplacement(out, Matcher.quoteReplacement("#" + normalized));
            }
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String toSnakeLower(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        // 1) Convert camelCase / PascalCase to snake_case.
        String s = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2");

        // 2) Normalize underscores and casing.
        s = s.replaceAll("_+", "_");
        s = s.toLowerCase();

        return s;
    }
}

