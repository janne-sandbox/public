package com.nologo.math2;

import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Immutable, composable mathematical expression. */
public final class Equation {

    /**
     * Maximum bit length for a rational numerator or denominator before the exact rational
     * reduction path is abandoned in favour of the decimal engine.  Bounds memory and time
     * for deep expressions with large intermediate values while covering all typical cases.
     * Approximately 12 000 decimal digits (≈ 40 000 bits / 3.32 bits-per-digit).
     */
    private static final int MAX_RATIONAL_BITS = 40_000;

    /**
     * Rounding mode codes accepted by {@code round()}.  Index 0 (HALF_EVEN) is the default
     * so the rest of the engine and the round function share the same behaviour when no mode
     * is specified.
     */
    private static final RoundingMode[] ROUNDING_MODE_TABLE = {
        RoundingMode.HALF_EVEN,  // 0 — banker's rounding (default)
        RoundingMode.HALF_UP,    // 1 — round half away from zero
        RoundingMode.HALF_DOWN,  // 2 — round half toward zero
        RoundingMode.CEILING,    // 3 — toward positive infinity
        RoundingMode.FLOOR,      // 4 — toward negative infinity
        RoundingMode.UP,         // 5 — always away from zero
        RoundingMode.DOWN,       // 6 — always toward zero (truncate)
    };

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
        return calculate(CalculationContext.builder().build());
    }

    public Number calculate(int precision) {
        validatePrecision(precision);
        return calculate(CalculationContext.builder().precision(precision).build());
    }

    public Number calculate(CalculationContext context) {
        ExecutionPlan plan = ExecutionPlan.create(
                canonicalExpression(), Objects.requireNonNull(context, "context"));
        return ExecutionPlanner.system().execute(plan).results().getFirst();
    }

    Number calculateReference(int precision) {
        validatePrecision(precision);
        return root.evaluate(new MathContext(precision, RoundingMode.HALF_EVEN));
    }

    private static void validatePrecision(int precision) {
        if (precision < 1 || precision > Number.getMaxLength()) {
            throw new UncalculableException(
                    CalculationError.PRECISION_EXCEEDED,
                    "Precision must be between 1 and " + Number.getMaxLength());
        }
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

    private sealed interface Node permits ValueNode, ConstantNode, OperationNode, RoundNode {
        Number evaluate(MathContext context);

        /**
         * Attempts to evaluate this node as an exact rational number (numerator/denominator).
         * Returns {@code Optional.empty()} for any subtree that contains an operation not closed
         * over the rationals (sqrt, log, sin, cos, tan, fractional power, or the constant pi).
         */
        Optional<Rational> rationalize();

        String canonical();
    }

    private record ValueNode(Number value) implements Node {
        @Override
        public Number evaluate(MathContext context) {
            return value;
        }

        @Override
        public Optional<Rational> rationalize() {
            return Rational.fromDecimalString(value.toString());
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
        public Optional<Rational> rationalize() {
            return Optional.empty(); // pi is transcendental
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
            // Attempt exact rational reduction first to eliminate intermediate precision loss
            Optional<Rational> rational = rationalize();
            if (rational.isPresent()) {
                return rational.get().toNumber(context.getPrecision());
            }
            return operator.apply(operands.stream().map(node -> node.evaluate(context)).toList(), context);
        }

        @Override
        public Optional<Rational> rationalize() {
            // Collect rational values for all operands; bail out if any operand is non-rational
            List<Optional<Rational>> rationalOperands = operands.stream()
                    .map(Node::rationalize)
                    .toList();
            if (rationalOperands.stream().anyMatch(Optional::isEmpty)) {
                return Optional.empty();
            }
            List<Rational> rs = rationalOperands.stream().map(Optional::get).toList();
            return switch (operator) {
                case ADD -> rs.get(0).add(rs.get(1));
                case SUBTRACT -> rs.get(0).subtract(rs.get(1));
                case MULTIPLY -> rs.get(0).multiply(rs.get(1));
                case DIVIDE -> rs.get(0).divide(rs.get(1));
                case POWER -> rs.get(0).power(rs.get(1));
                case ABS -> Optional.of(rs.get(0).abs());
                case MIN -> Optional.of(rs.get(0).compareTo(rs.get(1)) <= 0 ? rs.get(0) : rs.get(1));
                case MAX -> Optional.of(rs.get(0).compareTo(rs.get(1)) >= 0 ? rs.get(0) : rs.get(1));
                // SQRT, LOG, SIN, COS, TAN are not closed over the rationals in general
                default -> Optional.empty();
            };
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

    // ==== Rational arithmetic reduction ====

    /**
     * Exact rational number {@code p/q} in reduced form with positive denominator, backed by
     * {@link BigInteger} components.
     *
     * <p>The rational reduction pass evaluates expression subtrees using this type before
     * falling back to the decimal engine.  This eliminates intermediate precision loss for
     * expressions whose components cancel exactly – for example {@code (1/3 + 2/3)} reduces
     * to the exact integer {@code 1} regardless of the configured decimal precision, rather
     * than producing a rounded decimal approximation for each third that accumulates error.</p>
     *
     * <p>Operations that are not closed over the rationals – sqrt, log, sin, cos, tan,
     * fractional-exponent power, and the transcendental constant pi – cause
     * {@link Node#rationalize()} to return {@code Optional.empty()} for that subtree, at which
     * point the decimal engine evaluates it.  Sibling subtrees that <em>are</em> fully rational
     * are still reduced before being passed to the decimal computation.</p>
     */
    private record Rational(BigInteger numerator, BigInteger denominator) {

        private static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);

        /**
         * Returns a fully reduced rational with positive denominator, or {@code empty()} when
         * {@code d == 0} or either component would exceed {@link Equation#MAX_RATIONAL_BITS}.
         */
        static Optional<Rational> of(BigInteger n, BigInteger d) {
            if (d.signum() == 0) {
                return Optional.empty(); // let the decimal engine produce the correct error
            }
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            BigInteger g = n.abs().gcd(d);
            n = n.divide(g);
            d = d.divide(g);
            if (tooBig(n) || tooBig(d)) {
                return Optional.empty();
            }
            return Optional.of(new Rational(n, d));
        }

        private static boolean tooBig(BigInteger v) {
            return v.bitLength() > MAX_RATIONAL_BITS;
        }

        boolean isZero() {
            return numerator.signum() == 0;
        }

        Optional<Rational> add(Rational other) {
            // a/b + c/d = (a·d + b·c) / (b·d)
            return of(
                    numerator.multiply(other.denominator).add(denominator.multiply(other.numerator)),
                    denominator.multiply(other.denominator));
        }

        Optional<Rational> subtract(Rational other) {
            return of(
                    numerator.multiply(other.denominator).subtract(denominator.multiply(other.numerator)),
                    denominator.multiply(other.denominator));
        }

        Optional<Rational> multiply(Rational other) {
            return of(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
        }

        Optional<Rational> divide(Rational other) {
            if (other.isZero()) {
                return Optional.empty(); // let the decimal engine produce the correct error
            }
            return of(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
        }

        Optional<Rational> power(Rational exponent) {
            // Only exact for integer exponents
            if (!exponent.denominator.equals(BigInteger.ONE)) {
                return Optional.empty(); // fractional exponent → irrational in general
            }
            BigInteger expMagnitude = exponent.numerator.abs();
            if (expMagnitude.bitLength() > 30) {
                return Optional.empty(); // exponent > ~10^9; result would be astronomically large
            }
            boolean negativeExp = exponent.numerator.signum() < 0;
            if (negativeExp && isZero()) {
                return Optional.empty(); // 0^-n is undefined
            }
            int exp = expMagnitude.intValueExact();
            BigInteger newNum = numerator.pow(exp);
            BigInteger newDen = denominator.pow(exp);
            if (tooBig(newNum) || tooBig(newDen)) {
                return Optional.empty();
            }
            return negativeExp ? of(newDen, newNum) : of(newNum, newDen);
        }

        Rational abs() {
            return numerator.signum() >= 0 ? this : new Rational(numerator.negate(), denominator);
        }

        int compareTo(Rational other) {
            return numerator.multiply(other.denominator)
                    .compareTo(other.numerator.multiply(denominator));
        }

        /**
         * Converts this rational to a {@link Number} at the requested significant-digit precision.
         * An integer result (denominator == 1) is returned as an exact string; a non-terminating
         * result is computed by the decimal engine at the requested precision.
         */
        Number toNumber(int precision) {
            if (denominator.equals(BigInteger.ONE)) {
                return Number.fromString(numerator.toString());
            }
            if (isZero()) {
                return Number.zero();
            }
            return DecimalArithmetic.calculate(
                    BuiltInOperator.DIVIDE,
                    Number.fromString(numerator.toString()),
                    Number.fromString(denominator.toString()),
                    precision);
        }

        /**
         * Parses a canonical decimal string (as produced by {@link Number#toString()}) into an
         * exact rational, or returns empty for strings with exponents too large to handle.
         */
        static Optional<Rational> fromDecimalString(String s) {
            if (s.equals("0")) {
                return Optional.of(ZERO);
            }
            boolean negative = s.startsWith("-");
            String abs = negative ? s.substring(1) : s;
            int eIdx = Math.max(abs.indexOf('e'), abs.indexOf('E'));
            String mantissa = eIdx >= 0 ? abs.substring(0, eIdx) : abs;
            int exponent = parseExponentPart(abs, eIdx);
            if (exponent == Integer.MIN_VALUE) {
                return Optional.empty(); // exponent overflows int
            }
            int dotIdx = mantissa.indexOf('.');
            String intPart  = dotIdx >= 0 ? mantissa.substring(0, dotIdx) : mantissa;
            String fracPart = dotIdx >= 0 ? mantissa.substring(dotIdx + 1) : "";
            String digits = intPart + fracPart;
            if (digits.isEmpty()) {
                return Optional.of(ZERO);
            }
            BigInteger num;
            try {
                num = new BigInteger(digits);
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
            if (negative) {
                num = num.negate();
            }
            return scaleByExponent(num, exponent - fracPart.length());
        }

        private static int parseExponentPart(String abs, int eIdx) {
            if (eIdx < 0) {
                return 0;
            }
            try {
                return Integer.parseInt(abs.substring(eIdx + 1));
            } catch (NumberFormatException ignored) {
                return Integer.MIN_VALUE; // sentinel for overflow
            }
        }

        private static Optional<Rational> scaleByExponent(BigInteger num, int effectiveExp) {
            if (effectiveExp >= 0) {
                if (effectiveExp > 10_000) {
                    return Optional.empty();
                }
                return of(num.multiply(BigInteger.TEN.pow(effectiveExp)), BigInteger.ONE);
            }
            int absExp = -effectiveExp;
            if (absExp > 10_000) {
                return Optional.empty();
            }
            return of(num, BigInteger.TEN.pow(absExp));
        }
    }

    // ==== round() function ====

    /**
     * Rounds its operand to {@code scale} decimal places using the mode selected by
     * {@code modeCode}:
     * <ul>
     *   <li>0 — HALF_EVEN (banker's rounding, default)</li>
     *   <li>1 — HALF_UP (round half away from zero)</li>
     *   <li>2 — HALF_DOWN (round half toward zero)</li>
     *   <li>3 — CEILING (toward positive infinity)</li>
     *   <li>4 — FLOOR (toward negative infinity)</li>
     *   <li>5 — UP (always away from zero)</li>
     *   <li>6 — DOWN (always toward zero, truncate)</li>
     * </ul>
     *
     * <p>When the operand reduces to an exact rational the rounding is applied with
     * {@link BigInteger} arithmetic, so no precision is lost from intermediate decimal
     * approximations.  For irrational operands the decimal-engine result is rounded instead.
     * The canonical form always uses three arguments ({@code round(x,scale,mode)}) for
     * deterministic cache-key generation.</p>
     */
    private record RoundNode(Node operand, Node scale, Node mode) implements Node {

        @Override
        public Number evaluate(MathContext context) {
            int scaleInt = resolveIntArg(scale.evaluate(context), "scale");
            int modeInt  = resolveIntArg(mode.evaluate(context),  "mode");
            if (scaleInt < 0) {
                throw new UncalculableException(CalculationError.DOMAIN_ERROR,
                        "round() scale must be non-negative, got " + scaleInt);
            }
            RoundingMode roundingMode = resolveModeCode(modeInt);
            // Prefer exact rational rounding to avoid operand precision loss
            Optional<Rational> operandRational = operand.rationalize();
            if (operandRational.isPresent()) {
                Optional<BigInteger> rounded = computeRoundedInt(operandRational.get(), scaleInt, roundingMode);
                if (rounded.isPresent()) {
                    return buildRoundedNumber(rounded.get(), scaleInt);
                }
            }
            // Fall back: evaluate with decimal engine and round that result
            Rational operandR = Rational.fromDecimalString(operand.evaluate(context).toString())
                    .orElseThrow(() -> new UncalculableException(
                            CalculationError.DOMAIN_ERROR, "Cannot round the computed operand"));
            BigInteger rounded = computeRoundedInt(operandR, scaleInt, roundingMode)
                    .orElseThrow(() -> new UncalculableException(
                            CalculationError.DOMAIN_ERROR, "Cannot round to " + scaleInt + " decimal places"));
            return buildRoundedNumber(rounded, scaleInt);
        }

        @Override
        public Optional<Rational> rationalize() {
            Optional<Rational> operandR = operand.rationalize();
            Optional<Rational> scaleR   = scale.rationalize();
            Optional<Rational> modeR    = mode.rationalize();
            if (operandR.isEmpty() || scaleR.isEmpty() || modeR.isEmpty()) {
                return Optional.empty();
            }
            Rational s = scaleR.get();
            Rational m = modeR.get();
            if (!s.denominator().equals(BigInteger.ONE)
                    || s.numerator().signum() < 0
                    || s.numerator().bitLength() > 14) { // scale ≤ 16383
                return Optional.empty();
            }
            if (!m.denominator().equals(BigInteger.ONE)) {
                return Optional.empty();
            }
            int scaleInt;
            int modeInt;
            try {
                scaleInt = s.numerator().intValueExact();
                modeInt  = m.numerator().intValueExact();
            } catch (ArithmeticException ignored) {
                return Optional.empty();
            }
            if (modeInt < 0 || modeInt >= ROUNDING_MODE_TABLE.length) {
                return Optional.empty();
            }
            Optional<BigInteger> rounded =
                    computeRoundedInt(operandR.get(), scaleInt, ROUNDING_MODE_TABLE[modeInt]);
            if (rounded.isEmpty()) {
                return Optional.empty();
            }
            return Rational.of(rounded.get(), BigInteger.TEN.pow(scaleInt));
        }

        @Override
        public String canonical() {
            return "round(" + operand.canonical() + "," + scale.canonical() + "," + mode.canonical() + ")";
        }

        private static int resolveIntArg(Number value, String argName) {
            String s = value.toString();
            if (s.contains(".")) {
                throw new UncalculableException(CalculationError.DOMAIN_ERROR,
                        "round() " + argName + " must be a whole number, got " + s);
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                throw new UncalculableException(CalculationError.DOMAIN_ERROR,
                        "round() " + argName + " is out of int range: " + s);
            }
        }

        private static RoundingMode resolveModeCode(int code) {
            if (code < 0 || code >= ROUNDING_MODE_TABLE.length) {
                throw new UncalculableException(CalculationError.DOMAIN_ERROR,
                        "Unknown rounding mode " + code + "; valid range 0\u2013"
                                + (ROUNDING_MODE_TABLE.length - 1));
            }
            return ROUNDING_MODE_TABLE[code];
        }

        /**
         * Core algorithm: returns the rounded value &times;&nbsp;10<sup>scale</sup> as an exact
         * integer, or empty when scale exceeds the safe limit.
         */
        private static Optional<BigInteger> computeRoundedInt(Rational r, int scale, RoundingMode mode) {
            if (scale > 10_000) {
                return Optional.empty();
            }
            BigInteger shift     = BigInteger.TEN.pow(scale);
            BigInteger scaledNum = r.numerator().multiply(shift); // num × 10^scale
            BigInteger scaledDen = r.denominator();               // always positive
            int signum           = scaledNum.signum();
            BigInteger[] qr      = scaledNum.abs().divideAndRemainder(scaledDen);
            BigInteger quotient  = qr[0]; // ≥ 0
            BigInteger remainder = qr[1]; // 0 ≤ remainder < scaledDen
            int halfCmp = remainder.shiftLeft(1).compareTo(scaledDen); // sign(2·rem − den)
            boolean roundUp = shouldRoundUp(quotient, remainder, signum, halfCmp, mode);
            BigInteger absResult = roundUp ? quotient.add(BigInteger.ONE) : quotient;
            return Optional.of(signum >= 0 ? absResult : absResult.negate());
        }

        private static boolean shouldRoundUp(BigInteger quotient, BigInteger remainder,
                int signum, int halfCmp, RoundingMode mode) {
            boolean hasRemainder = remainder.signum() > 0;
            return switch (mode) {
                case UP        -> hasRemainder;
                case DOWN      -> false;
                case CEILING   -> signum > 0 && hasRemainder;
                case FLOOR     -> signum < 0 && hasRemainder;
                case HALF_UP   -> halfCmp >= 0;
                case HALF_DOWN -> halfCmp > 0;
                case HALF_EVEN -> halfCmp > 0 || (halfCmp == 0 && quotient.testBit(0));
                default -> throw new UncalculableException(CalculationError.DOMAIN_ERROR,
                        "Unsupported rounding mode: " + mode);
            };
        }

        /**
         * Converts the rounded integer back to a {@link Number} with exactly {@code scale}
         * decimal places (before {@link Number#fromString} canonicalises trailing zeros).
         */
        private static Number buildRoundedNumber(BigInteger signedResult, int scale) {
            if (scale == 0) {
                return Number.fromString(signedResult.toString());
            }
            boolean negative = signedResult.signum() < 0;
            StringBuilder digits = new StringBuilder(signedResult.abs().toString());
            while (digits.length() <= scale) {
                digits.insert(0, '0');
            }
            int dotPos = digits.length() - scale;
            String decStr = digits.substring(0, dotPos) + "." + digits.substring(dotPos);
            return Number.fromString(negative ? "-" + decStr : decStr);
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
            if (name.equals("round")) {
                return parseRound();
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

        /**
         * Parses {@code round(x)}, {@code round(x,scale)}, or {@code round(x,scale,mode)}.
         * Missing trailing arguments default to 0 (scale=0, mode=HALF_EVEN).
         */
        private Node parseRound() {
            if (!consume('(')) {
                throw invalid("Expected '(' after round", position);
            }
            Node operand   = parseAdditive();
            Node scaleNode = new ValueNode(Number.fromInteger(0));
            Node modeNode  = new ValueNode(Number.fromInteger(Number.getDefaultRoundingModeCode()));
            if (!consume(')')) {
                if (!consume(',')) {
                    throw invalid("Expected ',' or ')' in round", position);
                }
                scaleNode = parseAdditive();
                if (!consume(')')) {
                    if (!consume(',')) {
                        throw invalid("Expected ',' or ')' in round", position);
                    }
                    modeNode = parseAdditive();
                    if (!consume(')')) {
                        throw invalid("Expected closing parenthesis", position);
                    }
                }
            }
            return new RoundNode(operand, scaleNode, modeNode);
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
