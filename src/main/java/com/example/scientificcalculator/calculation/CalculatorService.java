package com.example.scientificcalculator.calculation;

import java.util.List;

import com.example.scientificcalculator.expression.ExpressionEvaluator;

public final class CalculatorService {
    private final ExpressionEvaluator evaluator;
    private final CalculationHistory history;

    public CalculatorService(ExpressionEvaluator evaluator, CalculationHistory history) {
        this.evaluator = evaluator;
        this.history = history;
    }

    public CalculationRecord calculate(String expression, AngleUnit angleUnit) {
        double result = evaluator.evaluate(expression, angleUnit);
        return history.add(expression, angleUnit, result);
    }

    public List<CalculationRecord> history(int limit) {
        return history.list(limit);
    }

    public CalculationRecord history(long id) {
        return history.find(id).orElseThrow(() -> new HistoryNotFoundException(id));
    }

    public void clearHistory() {
        history.clear();
    }
}
