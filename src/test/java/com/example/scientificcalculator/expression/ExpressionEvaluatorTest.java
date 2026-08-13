package com.example.scientificcalculator.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.scientificcalculator.calculation.AngleUnit;

class ExpressionEvaluatorTest {
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Test
    void respectsPrecedenceAndParentheses() {
        assertEquals(7, evaluator.evaluate("1 + 2 * 3", AngleUnit.RADIAN), 1e-12);
        assertEquals(9, evaluator.evaluate("(1 + 2) * 3", AngleUnit.RADIAN), 1e-12);
    }

    @Test
    void supportsRightAssociativePowerAndUnarySigns() {
        assertEquals(512, evaluator.evaluate("2^3^2", AngleUnit.RADIAN), 1e-12);
        assertEquals(-4, evaluator.evaluate("-2^2", AngleUnit.RADIAN), 1e-12);
        assertEquals(0.25, evaluator.evaluate("2^-2", AngleUnit.RADIAN), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({
            "'.5 + 1.5e2', 150.5, RADIAN",
            "'sqrt(16) + ln(e)', 5.0, RADIAN",
            "'sin(pi / 2)', 1.0, RADIAN",
            "'sin(90)', 1.0, DEGREE",
            "'cos(180)', -1.0, DEGREE",
            "'tan(45)', 1.0, DEGREE",
            "'abs(-3) + log(100)', 5.0, RADIAN",
            "'exp(1)', 2.718281828459045, RADIAN"
    })
    void evaluatesSupportedNumbersConstantsAndFunctions(
            String expression, double expected, AngleUnit angleUnit) {
        assertEquals(expected, evaluator.evaluate(expression, angleUnit), 1e-10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 +", "1; system()", "foo(1)", "x + 1", "2pi", "2(3+4)",
            "1 + 2 abc", "1e", "1..2", "sqrt(1,2)"})
    void rejectsInvalidSyntax(String expression) {
        assertCode("EXPRESSION_SYNTAX_ERROR", expression);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1/0", "1%0"})
    void rejectsDivisionByZero(String expression) {
        assertCode("DIVISION_BY_ZERO", expression);
    }

    @Test
    void rejectsBlankExpressionAsInvalidArgument() {
        assertCode("INVALID_ARGUMENT", "   ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqrt(-1)", "ln(0)", "log(-1)", "(-1)^0.5"})
    void rejectsDomainErrors(String expression) {
        assertCode("DOMAIN_ERROR", expression);
    }

    @ParameterizedTest
    @ValueSource(strings = {"exp(1000)", "1e309", "1e308*1e308"})
    void rejectsNonFiniteResults(String expression) {
        assertCode("NON_FINITE_RESULT", expression);
    }

    @Test
    void rejectsExpressionLengthTokenAndDepthLimits() {
        assertCode("EXPRESSION_LIMIT_EXCEEDED", "1".repeat(1001));
        assertCode("EXPRESSION_LIMIT_EXCEEDED", "1+".repeat(250) + "1");
        assertCode("EXPRESSION_LIMIT_EXCEEDED", "(".repeat(101) + "1" + ")".repeat(101));
    }

    @Test
    void keepsJavaFloatingPointBehaviorNearTangentSingularity() {
        assertTrue(Double.isFinite(evaluator.evaluate("tan(pi/2)", AngleUnit.RADIAN)));
    }

    private void assertCode(String expectedCode, String expression) {
        ExpressionException exception = assertThrows(ExpressionException.class,
                () -> evaluator.evaluate(expression, AngleUnit.RADIAN));
        assertEquals(expectedCode, exception.code());
    }
}
