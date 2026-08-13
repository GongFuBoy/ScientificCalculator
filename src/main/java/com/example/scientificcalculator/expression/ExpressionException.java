package com.example.scientificcalculator.expression;

public class ExpressionException extends RuntimeException {
    private final String code;

    public ExpressionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
