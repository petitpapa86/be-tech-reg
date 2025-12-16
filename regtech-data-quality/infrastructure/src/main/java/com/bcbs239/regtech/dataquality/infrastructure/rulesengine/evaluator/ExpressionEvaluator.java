package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator;

import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;

/**
 * Interface for evaluating expressions within a rule context.
 */
public interface ExpressionEvaluator {
    
    /**
     * Evaluates an expression and returns a boolean result.
     * 
     * @param expression The expression to evaluate
     * @param context The context containing variables
     * @return true if the expression evaluates to true
     * @throws ExpressionEvaluationException if evaluation fails
     */
    boolean evaluateBoolean(String expression, RuleContext context);
    
    /**
     * Evaluates an expression and returns an object result.
     * 
     * @param expression The expression to evaluate
     * @param context The context containing variables
     * @return The evaluation result
     * @throws ExpressionEvaluationException if evaluation fails
     */
    Object evaluate(String expression, RuleContext context);
    
    /**
     * Evaluates an expression and casts the result to a specific type.
     * 
     * @param expression The expression to evaluate
     * @param context The context containing variables
     * @param resultType The expected result type
     * @param <T> The type parameter
     * @return The evaluation result cast to the specified type
     * @throws ExpressionEvaluationException if evaluation fails or type mismatch
     */
    <T> T evaluate(String expression, RuleContext context, Class<T> resultType);
    
    /**
     * Validates if an expression is syntactically correct.
     * 
     * @param expression The expression to validate
     * @return true if the expression is valid
     */
    boolean isValidExpression(String expression);
}
