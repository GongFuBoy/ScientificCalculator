package com.example.scientificcalculator.expression;

import java.util.ArrayList;
import java.util.List;

import com.example.scientificcalculator.calculation.AngleUnit;

public final class ExpressionEvaluator {
    private static final int MAX_EXPRESSION_LENGTH = 1000;
    private static final int MAX_TOKENS = 500;
    private static final int MAX_DEPTH = 100;

    public double evaluate(String expression, AngleUnit angleUnit) {
        if (expression == null || expression.isBlank()) {
            throw new ExpressionException("INVALID_ARGUMENT", "Expression must not be blank");
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw limit("Expression exceeds 1000 characters");
        }
        Parser parser = new Parser(tokenize(expression), angleUnit);
        double result = parser.parseExpression();
        parser.expect(TokenType.EOF);
        return result == 0 ? 0 : result;
    }

    private List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        while (position < input.length()) {
            char current = input.charAt(position);
            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }
            int start = position;
            Token token;
            if (Character.isDigit(current) || current == '.') {
                position = scanNumber(input, position);
                String text = input.substring(start, position);
                try {
                    token = new Token(TokenType.NUMBER, text, Double.parseDouble(text), start);
                } catch (NumberFormatException exception) {
                    throw syntax("Invalid number", start);
                }
            } else if (current >= 'a' && current <= 'z') {
                position++;
                while (position < input.length()) {
                    char next = input.charAt(position);
                    if ((next >= 'a' && next <= 'z') || Character.isDigit(next)) {
                        position++;
                    } else {
                        break;
                    }
                }
                token = new Token(TokenType.IDENTIFIER, input.substring(start, position), 0, start);
            } else {
                position++;
                token = switch (current) {
                    case '+' -> new Token(TokenType.PLUS, "+", 0, start);
                    case '-' -> new Token(TokenType.MINUS, "-", 0, start);
                    case '*' -> new Token(TokenType.MULTIPLY, "*", 0, start);
                    case '/' -> new Token(TokenType.DIVIDE, "/", 0, start);
                    case '%' -> new Token(TokenType.MODULO, "%", 0, start);
                    case '^' -> new Token(TokenType.POWER, "^", 0, start);
                    case '(' -> new Token(TokenType.LEFT_PAREN, "(", 0, start);
                    case ')' -> new Token(TokenType.RIGHT_PAREN, ")", 0, start);
                    default -> throw syntax("Unexpected character", start);
                };
            }
            tokens.add(token);
            if (tokens.size() > MAX_TOKENS) {
                throw limit("Expression exceeds 500 tokens");
            }
        }
        tokens.add(new Token(TokenType.EOF, "", 0, input.length()));
        return tokens;
    }

    private int scanNumber(String input, int position) {
        int start = position;
        boolean integerDigits = false;
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            integerDigits = true;
            position++;
        }
        if (position < input.length() && input.charAt(position) == '.') {
            position++;
            int fractionStart = position;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
            if (!integerDigits && position == fractionStart) {
                throw syntax("Invalid number", start);
            }
        } else if (!integerDigits) {
            throw syntax("Invalid number", start);
        }
        if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            position++;
            if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                position++;
            }
            int exponentStart = position;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
            if (position == exponentStart) {
                throw syntax("Invalid exponent", start);
            }
        }
        return position;
    }

    private static ExpressionException syntax(String message, int position) {
        return new ExpressionException("EXPRESSION_SYNTAX_ERROR", message + " at position " + position);
    }

    private static ExpressionException limit(String message) {
        return new ExpressionException("EXPRESSION_LIMIT_EXCEEDED", message);
    }

    private enum TokenType {
        NUMBER, IDENTIFIER, PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER, LEFT_PAREN, RIGHT_PAREN, EOF
    }

    private record Token(TokenType type, String text, double number, int position) {
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final AngleUnit angleUnit;
        private int position;
        private int depth;

        private Parser(List<Token> tokens, AngleUnit angleUnit) {
            this.tokens = tokens;
            this.angleUnit = angleUnit;
        }

        private double parseExpression() {
            return parseAdditive();
        }

        private double parseAdditive() {
            double value = parseMultiplicative();
            while (true) {
                if (match(TokenType.PLUS)) {
                    value = finite(value + parseMultiplicative());
                } else if (match(TokenType.MINUS)) {
                    value = finite(value - parseMultiplicative());
                } else {
                    return value;
                }
            }
        }

        private double parseMultiplicative() {
            double value = parseUnary();
            while (true) {
                if (match(TokenType.MULTIPLY)) {
                    value = finite(value * parseUnary());
                } else if (match(TokenType.DIVIDE)) {
                    double divisor = parseUnary();
                    if (divisor == 0) {
                        throw new ExpressionException("DIVISION_BY_ZERO", "Division by zero");
                    }
                    value = finite(value / divisor);
                } else if (match(TokenType.MODULO)) {
                    double divisor = parseUnary();
                    if (divisor == 0) {
                        throw new ExpressionException("DIVISION_BY_ZERO", "Modulo by zero");
                    }
                    value = finite(value % divisor);
                } else {
                    return value;
                }
            }
        }

        private double parseUnary() {
            if (match(TokenType.PLUS)) {
                return parseUnary();
            }
            if (match(TokenType.MINUS)) {
                return finite(-parseUnary());
            }
            return parsePower();
        }

        private double parsePower() {
            double value = parsePrimary();
            if (match(TokenType.POWER)) {
                value = checked(Math.pow(value, parseUnary()));
            }
            return value;
        }

        private double parsePrimary() {
            if (match(TokenType.NUMBER)) {
                return finite(previous().number());
            }
            if (match(TokenType.LEFT_PAREN)) {
                enterDepth();
                try {
                    double value = parseExpression();
                    expect(TokenType.RIGHT_PAREN);
                    return value;
                } finally {
                    depth--;
                }
            }
            if (match(TokenType.IDENTIFIER)) {
                String identifier = previous().text();
                if (identifier.equals("pi")) {
                    return Math.PI;
                }
                if (identifier.equals("e")) {
                    return Math.E;
                }
                expect(TokenType.LEFT_PAREN);
                enterDepth();
                try {
                    double argument = parseExpression();
                    expect(TokenType.RIGHT_PAREN);
                    return applyFunction(identifier, argument);
                } finally {
                    depth--;
                }
            }
            throw syntax("Expected number, constant, function, or parenthesis", current().position());
        }

        private double applyFunction(String name, double argument) {
            double radians = angleUnit == AngleUnit.DEGREE ? Math.toRadians(argument) : argument;
            return switch (name) {
                case "sin" -> checked(Math.sin(radians));
                case "cos" -> checked(Math.cos(radians));
                case "tan" -> checked(Math.tan(radians));
                case "sqrt" -> argument < 0 ? domain("sqrt domain") : checked(Math.sqrt(argument));
                case "abs" -> checked(Math.abs(argument));
                case "ln" -> argument <= 0 ? domain("ln domain") : checked(Math.log(argument));
                case "log" -> argument <= 0 ? domain("log domain") : checked(Math.log10(argument));
                case "exp" -> checked(Math.exp(argument));
                default -> throw syntax("Unknown function " + name, previous().position());
            };
        }

        private double domain(String message) {
            throw new ExpressionException("DOMAIN_ERROR", message);
        }

        private void enterDepth() {
            depth++;
            if (depth > MAX_DEPTH) {
                throw limit("Expression exceeds nesting depth 100");
            }
        }

        private double checked(double value) {
            if (Double.isNaN(value)) {
                throw new ExpressionException("DOMAIN_ERROR", "Value is outside the function domain");
            }
            return finite(value);
        }

        private double finite(double value) {
            if (!Double.isFinite(value)) {
                throw new ExpressionException("NON_FINITE_RESULT", "Calculation produced a non-finite result");
            }
            return value;
        }

        private boolean match(TokenType type) {
            if (current().type() == type) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(TokenType type) {
            if (!match(type)) {
                throw syntax("Expected " + type, current().position());
            }
        }

        private Token current() {
            return tokens.get(position);
        }

        private Token previous() {
            return tokens.get(position - 1);
        }
    }
}
