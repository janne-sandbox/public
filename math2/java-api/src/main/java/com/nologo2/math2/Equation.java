package com.nologo2.math2;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable, composable mathematical expression. */
public final class Equation {
    private final Node root;

    private Equation(Node root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public static Equation of(Number value) {
        return new Equation(new ValueNode(Objects.requireNonNull(value, "value")));
    }

    public static Equation parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw invalid("Expression must not be blank", 0);
        }
        return new Equation(new Parser(expression).parse());
    }

    public Equation add(Equation other) {
        return binary(BuiltInOperator.ADD, other);
    }

    public Equation add(Number other) {
        return add(of(other));
    }

    public Equation subtract(Equation other) {
        return binary(BuiltInOperator.SUBTRACT, other);
    }

    public Equation subtract(Number other) {
        return subtract(of(other));
    }

    public Equation multiply(Equation other) {
        return binary(BuiltInOperator.MULTIPLY, other);
    }

    public Equation multiply(Number other) {
        return multiply(of(other));
    }

    public Equation divide(Equation other) {
        return binary(BuiltInOperator.DIVIDE, other);
    }

    public Equation divide(Number other) {
        return divide(of(other));
    }

    public Equation power(Equation other) {
        return binary(BuiltInOperator.POWER, other);
    }

    public Equation power(Number other) {
        return power(of(other));
    }

    public Equation min(Equation other) {
        return binary(BuiltInOperator.MIN, other);
    }

    public Equation min(Number other) {
        return min(of(other));
    }

    public Equation max(Equation other) {
        return binary(BuiltInOperator.MAX, other);
    }

    public Equation max(Number other) {
        return max(of(other));
    }

    public Equation sqrt() {
        return unary(BuiltInOperator.SQRT);
    }

    public Equation abs() {
        return unary(BuiltInOperator.ABS);
    }

    public Equation log() {
        return unary(BuiltInOperator.LOG);
    }

    public Equation sin() {
        return unary(BuiltInOperator.SIN);
    }

    public Equation cos() {
        return unary(BuiltInOperator.COS);
    }

    public Equation tan() {
        return unary(BuiltInOperator.TAN);
    }

    public Number calculate() {
        return calculate(Number.getMaxLength());
    }

    public Number calculate(int precision) {
        if (precision < 1 || precision > Number.getMaxLength()) {
            throw new UncalculableException(
                    CalculationError.PRECISION_EXCEEDED,
                    "Precision must be between 1 and " + Number.getMaxLength());
        }
        return root.evaluate(new MathContext(precision, RoundingMode.HALF_EVEN));
    }

    public String canonicalExpression() {
        return root.canonical();
    }

    @Override
    public String toString() {
        return canonicalExpression();
    }

    private Equation unary(BuiltInOperator operator) {
        return new Equation(new OperationNode(operator, List.of(root)));
    }

    private Equation binary(BuiltInOperator operator, Equation other) {
        return new Equation(new OperationNode(
                operator, List.of(root, Objects.requireNonNull(other, "other").root)));
    }

    private sealed interface Node permits ValueNode, ConstantNode, OperationNode {
        Number evaluate(MathContext context);

        String canonical();
    }

    private record ValueNode(Number value) implements Node {
        @Override
        public Number evaluate(MathContext context) {
            return value;
        }

        @Override
        public String canonical() {
            return value.toString();
        }
    }

    private enum ConstantNode implements Node {
        PI;

        @Override
        public Number evaluate(MathContext context) {
            return DecimalConstants.pi(context.getPrecision());
        }

        @Override
        public String canonical() {
            return "pi";
        }
    }

    private record OperationNode(BuiltInOperator operator, List<Node> operands) implements Node {
        private OperationNode {
            operands = List.copyOf(operands);
            if (operands.size() != operator.arity()) {
                throw new IllegalArgumentException("Wrong operand count for " + operator.symbol());
            }
        }

        @Override
        public Number evaluate(MathContext context) {
            return operator.apply(operands.stream().map(node -> node.evaluate(context)).toList(), context);
        }

        @Override
        public String canonical() {
            if (operator.arity() == 1) {
                return operator.symbol() + "(" + operands.get(0).canonical() + ")";
            }
            if (operator == BuiltInOperator.MIN || operator == BuiltInOperator.MAX) {
                return operator.symbol() + "(" + operands.get(0).canonical() + ","
                        + operands.get(1).canonical() + ")";
            }
            return "(" + operands.get(0).canonical() + operator.symbol()
                    + operands.get(1).canonical() + ")";
        }
    }

    private static final class Parser {
        private final String input;
        private int position;

        private Parser(String input) {
            this.input = input;
        }

        private Node parse() {
            Node result = parseAdditive();
            skipWhitespace();
            if (!atEnd()) {
                throw invalid("Unexpected token", position);
            }
            return result;
        }

        private Node parseAdditive() {
            Node left = parseMultiplicative();
            while (true) {
                if (consume('+')) {
                    left = operation(BuiltInOperator.ADD, left, parseMultiplicative());
                } else if (consume('-')) {
                    left = operation(BuiltInOperator.SUBTRACT, left, parseMultiplicative());
                } else {
                    return left;
                }
            }
        }

        private Node parseMultiplicative() {
            Node left = parseUnary();
            while (true) {
                if (consume('*')) {
                    left = operation(BuiltInOperator.MULTIPLY, left, parseUnary());
                } else if (consume('/')) {
                    left = operation(BuiltInOperator.DIVIDE, left, parseUnary());
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            if (consume('+')) {
                return parseUnary();
            }
            if (consume('-')) {
                return operation(BuiltInOperator.SUBTRACT, new ValueNode(Number.zero()), parseUnary());
            }
            return parsePower();
        }

        private Node parsePower() {
            Node left = parsePrimary();
            if (consume('^')) {
                return operation(BuiltInOperator.POWER, left, parseUnary());
            }
            return left;
        }

        private Node parsePrimary() {
            skipWhitespace();
            int start = position;
            if (consume('(')) {
                Node nested = parseAdditive();
                if (!consume(')')) {
                    throw invalid("Expected closing parenthesis", position);
                }
                return nested;
            }
            if (!atEnd() && Character.isLetter(input.charAt(position))) {
                return parseFunction();
            }
            return parseNumber(start);
        }

        private Node parseFunction() {
            int start = position;
            while (!atEnd() && Character.isLetter(input.charAt(position))) {
                position++;
            }
            String name = input.substring(start, position).toLowerCase(Locale.ROOT);
            if (name.equals("pi")) {
                return ConstantNode.PI;
            }
            BuiltInOperator operator = switch (name) {
                case "sqrt" -> BuiltInOperator.SQRT;
                case "abs" -> BuiltInOperator.ABS;
                case "min" -> BuiltInOperator.MIN;
                case "max" -> BuiltInOperator.MAX;
                case "log" -> BuiltInOperator.LOG;
                case "sin" -> BuiltInOperator.SIN;
                case "cos" -> BuiltInOperator.COS;
                case "tan" -> BuiltInOperator.TAN;
                default -> throw invalid("Unknown function: " + name, start);
            };
            if (!consume('(')) {
                throw invalid("Expected opening parenthesis after " + name, position);
            }
            Node first = parseAdditive();
            if (operator.arity() == 1) {
                if (!consume(')')) {
                    throw invalid("Expected closing parenthesis", position);
                }
                return new OperationNode(operator, List.of(first));
            }
            if (!consume(',')) {
                throw invalid("Expected comma in " + name, position);
            }
            Node second = parseAdditive();
            if (!consume(')')) {
                throw invalid("Expected closing parenthesis", position);
            }
            return operation(operator, first, second);
        }

        private Node parseNumber(int start) {
            skipWhitespace();
            start = position;
            boolean digits = consumeDigits();
            if (consume('.')) {
                digits = consumeDigits() || digits;
            }
            if (!digits) {
                throw invalid("Expected number", start);
            }
            if (peek('e') || peek('E')) {
                position++;
                if (peek('+') || peek('-')) {
                    position++;
                }
                int exponentStart = position;
                if (!consumeDigits()) {
                    throw invalid("Expected exponent digits", exponentStart);
                }
            }
            try {
                return new ValueNode(Number.fromString(input.substring(start, position)));
            } catch (UncalculableException exception) {
                throw invalid(exception.getMessage(), start);
            }
        }

        private boolean consumeDigits() {
            int start = position;
            while (!atEnd() && Character.isDigit(input.charAt(position))) {
                position++;
            }
            return position > start;
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (peek(expected)) {
                position++;
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            return !atEnd() && input.charAt(position) == expected;
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean atEnd() {
            return position >= input.length();
        }

        private static Node operation(BuiltInOperator operator, Node left, Node right) {
            return new OperationNode(operator, List.of(left, right));
        }
    }

    private static UncalculableException invalid(String message, int position) {
        return new UncalculableException(
                CalculationError.INVALID_EXPRESSION, message + " at position " + position, position);
    }
}
