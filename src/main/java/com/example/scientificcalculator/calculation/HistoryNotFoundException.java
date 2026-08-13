package com.example.scientificcalculator.calculation;

public final class HistoryNotFoundException extends RuntimeException {
    public HistoryNotFoundException(long id) {
        super("Calculation history " + id + " was not found");
    }
}
