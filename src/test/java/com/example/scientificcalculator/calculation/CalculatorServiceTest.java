package com.example.scientificcalculator.calculation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.scientificcalculator.expression.ExpressionException;
import com.example.scientificcalculator.expression.ExpressionEvaluator;

class CalculatorServiceTest {
    @Test
    void failedEvaluationDoesNotWriteHistory() {
        CalculationHistory history = new CalculationHistory(1000);
        CalculatorService service = new CalculatorService(new ExpressionEvaluator(), history);

        assertThrows(ExpressionException.class, () -> service.calculate("1/0", AngleUnit.RADIAN));
        assertTrue(history.list(100).isEmpty());
    }
}
