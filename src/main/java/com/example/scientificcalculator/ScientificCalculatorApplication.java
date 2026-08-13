package com.example.scientificcalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.scientificcalculator.calculation.CalculationHistory;
import com.example.scientificcalculator.calculation.CalculatorService;
import com.example.scientificcalculator.expression.ExpressionEvaluator;

@SpringBootApplication
public class ScientificCalculatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScientificCalculatorApplication.class, args);
    }

    @Bean
    ExpressionEvaluator expressionEvaluator() {
        return new ExpressionEvaluator();
    }

    @Bean
    CalculationHistory calculationHistory() {
        return new CalculationHistory(1000);
    }

    @Bean
    CalculatorService calculatorService(ExpressionEvaluator evaluator, CalculationHistory history) {
        return new CalculatorService(evaluator, history);
    }
}
