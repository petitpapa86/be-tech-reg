package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.evaluator;

import com.bcbs239.regtech.dataquality.rulesengine.engine.DefaultRuleContext;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpelExpressionEvaluator alias lookup")
class SpelExpressionEvaluatorTest {

    @Test
    @DisplayName("Resolves camelCase and mixed-case aliases for snake_case context keys")
    void resolvesAliasesWithoutDuplicatingContextKeys() {
        RuleContext ctx = new DefaultRuleContext(Map.of(
            "exposure_id", "EXP-001",
            "product_type", "EQUITY"
        ));

        SpelExpressionEvaluator evaluator = new SpelExpressionEvaluator();

        assertFalse(ctx.containsKey("exposureId"));
        assertFalse(ctx.containsKey("productType"));

        assertTrue(evaluator.evaluateBoolean("#exposureId != null", ctx));
        assertTrue(evaluator.evaluateBoolean("#exposure_Id != null", ctx));
        assertTrue(evaluator.evaluateBoolean("#exposure_id != null", ctx));

        assertTrue(evaluator.evaluateBoolean("#productType == 'EQUITY'", ctx));
        assertTrue(evaluator.evaluateBoolean("#product_type == 'EQUITY'", ctx));
        assertTrue(evaluator.evaluateBoolean("#Product_Type == 'EQUITY'", ctx));
    }
}
