package com.example.scientificcalculator.calculation;

import java.time.Instant;

public record CalculationRecord(
        long id,
        String expression,
        AngleUnit angleUnit,
        double result,
        Instant createdAt) {
}
