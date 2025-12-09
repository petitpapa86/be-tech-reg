package com.bcbs239.regtech.dataquality.rulesengine.evaluator;

/**
 * Exception thrown when expression evaluation fails.
 */
public class ExpressionEvaluationException extends RuntimeException {
    
    public ExpressionEvaluationException(String message) {
        super(message);
    }
    
    public ExpressionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
