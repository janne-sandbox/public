package com.nologo2.math2;

import java.util.Arrays;

/**
 * Math2's internal arbitrary-precision decimal engine.
 *
 * <p>The implementation deliberately uses only decimal strings and arrays of base-1,000,000,000
 * limbs. It does not delegate calculations to {@code BigDecimal}, {@code BigInteger}, Apfloat, or
 * another numeric engine.</p>
 */
final class DecimalArithmetic {
    private static final int GUARD_DIGITS = 16;

    private DecimalArithmetic() {
    }

    static Number calculate(BuiltInOperator operator, Number left, Number right, int precision) {
        int scale = workingScale(operator, left.toString(), right == null ? null : right.toString(), precision);
        Decimal a = Decimal.parse(left.toString(), scale);
        Decimal b = right == null ? null : Decimal.parse(right.toString(), scale);
        Decimal result = switch (operator) {
            case ADD -> a.add(b);
            case SUBTRACT -> a.subtract(b);
            case MULTIPLY -> a.multiply(b);
            case DIVIDE -> a.divide(b);
            case POWER -> power(a, b, right.toString());
            case SQRT -> sqrt(a);
            case ABS -> a.abs();
            case MIN -> a.compareTo(b) <= 0 ? a : b;
            case MAX -> a.compareTo(b) >= 0 ? a : b;
            case LOG -> log(a);
            case SIN -> sin(a);
            case COS -> cos(a);
            case TAN -> {
                Decimal cosine = cos(a);
                if (cosine.isZero()) {
                    throw domain("Tangent is undefined for this value");
                }
                yield sin(a).divide(cosine);
            }
        };
        return result.toNumber(precision);
    }

    static Number pi(int precision) {
        int scale = Math.addExact(precision, GUARD_DIGITS);
        return piDecimal(scale).toNumber(precision);
    }

    private static Decimal piDecimal(int scale) {
        Decimal one = Decimal.one(scale);
        return atanInverse(one, 5).multiplySmall(16)
                .subtract(atanInverse(one, 239).multiplySmall(4));
    }

    private static Decimal atanInverse(Decimal one, int inverse) {
        Decimal x = one.divideSmall(inverse);
        Decimal factor = x.multiply(x);
        Decimal power = x;
        Decimal sum = x;
        boolean subtract = true;
        for (int divisor = 3; !power.isZero(); divisor += 2) {
            power = power.multiply(factor);
            if (power.isZero()) {
                break;
            }
            Decimal term = power.divideSmall(divisor);
            Decimal next = subtract ? sum.subtract(term) : sum.add(term);
            if (next.equals(sum)) {
                break;
            }
            sum = next;
            subtract = !subtract;
        }
        return sum;
    }

    private static Decimal sqrt(Decimal value) {
        if (value.signum() < 0) {
            throw domain("Square root requires a non-negative value");
        }
        if (value.isZero()) {
            return value;
        }
        int exponent = Math.floorDiv(value.decimalOrder() + 1, 2);
        Decimal estimate = Decimal.powerOfTen(exponent, value.scale);
        Decimal two = Decimal.fromLong(2, value.scale);
        for (;;) {
            Decimal next = estimate.add(value.divide(estimate)).divide(two);
            if (next.equals(estimate) || next.subtract(estimate).abs().atMostOneUlp()) {
                return next;
            }
            estimate = next;
        }
    }

    private static Decimal log(Decimal value) {
        if (value.signum() <= 0) {
            throw domain("Logarithm requires a positive value");
        }
        int exponent = value.decimalOrder();
        Decimal mantissa = value.divide(Decimal.powerOfTen(exponent, value.scale));
        Decimal logarithm = atanhLog(mantissa);
        if (exponent != 0) {
            Decimal lnTen = atanhLog(Decimal.fromLong(10, value.scale));
            logarithm = logarithm.add(lnTen.multiplyLong(exponent));
        }
        return logarithm;
    }

    /** ln(x) = 2 * (y + y^3/3 + y^5/5 + ...), where y=(x-1)/(x+1). */
    private static Decimal atanhLog(Decimal value) {
        Decimal one = Decimal.one(value.scale);
        Decimal y = value.subtract(one).divide(value.add(one));
        Decimal factor = y.multiply(y);
        Decimal power = y;
        Decimal sum = y;
        for (int divisor = 3; !power.isZero(); divisor += 2) {
            power = power.multiply(factor);
            if (power.isZero()) {
                break;
            }
            Decimal next = sum.add(power.divideSmall(divisor));
            if (next.equals(sum)) {
                break;
            }
            sum = next;
        }
        return sum.multiplySmall(2);
    }

    private static Decimal power(Decimal base, Decimal exponent, String exponentText) {
        if (isInteger(exponentText)) {
            boolean negativeExponent = exponentText.charAt(0) == '-';
            String magnitude = negativeExponent ? exponentText.substring(1) : exponentText;
            Decimal result = Decimal.one(base.scale);
            Decimal factor = base;
            while (!magnitude.equals("0")) {
                Halved halved = halve(magnitude);
                if (halved.odd) {
                    result = result.multiply(factor);
                }
                magnitude = halved.quotient;
                if (!magnitude.equals("0")) {
                    factor = factor.multiply(factor);
                }
            }
            if (negativeExponent) {
                if (result.isZero()) {
                    throw new UncalculableException(CalculationError.DIVISION_BY_ZERO, "Division by zero");
                }
                return Decimal.one(base.scale).divide(result);
            }
            return result;
        }
        if (base.signum() <= 0) {
            throw domain("A fractional power requires a positive base");
        }
        return exp(log(base).multiply(exponent));
    }

    private static Decimal exp(Decimal value) {
        Decimal reduced = value;
        Decimal threshold = Decimal.parse("0.125", value.scale);
        int squarings = 0;
        while (reduced.abs().compareTo(threshold) > 0) {
            reduced = reduced.divideSmall(2);
            squarings++;
        }
        Decimal term = Decimal.one(value.scale);
        Decimal sum = term;
        for (int divisor = 1; !term.isZero(); divisor++) {
            term = term.multiply(reduced).divideSmall(divisor);
            Decimal next = sum.add(term);
            if (next.equals(sum)) {
                break;
            }
            sum = next;
        }
        for (int index = 0; index < squarings; index++) {
            sum = sum.multiply(sum);
        }
        return sum;
    }

    private static Decimal sin(Decimal value) {
        Decimal x = reduceRadians(value);
        Decimal negativeSquare = x.multiply(x).negate();
        Decimal term = x;
        Decimal sum = x;
        for (int index = 1; !term.isZero(); index++) {
            term = term.multiply(negativeSquare)
                    .divideSmall(Math.multiplyExact(2 * index, 2 * index + 1));
            Decimal next = sum.add(term);
            if (next.equals(sum)) {
                break;
            }
            sum = next;
        }
        return sum;
    }

    private static Decimal cos(Decimal value) {
        Decimal x = reduceRadians(value);
        Decimal negativeSquare = x.multiply(x).negate();
        Decimal term = Decimal.one(value.scale);
        Decimal sum = term;
        for (int index = 1; !term.isZero(); index++) {
            term = term.multiply(negativeSquare)
                    .divideSmall(Math.multiplyExact(2 * index - 1, 2 * index));
            Decimal next = sum.add(term);
            if (next.equals(sum)) {
                break;
            }
            sum = next;
        }
        return sum;
    }

    private static Decimal reduceRadians(Decimal value) {
        Decimal pi = piDecimal(value.scale);
        Decimal twoPi = pi.multiplySmall(2);
        Decimal reduced = value.remainder(twoPi);
        if (reduced.compareTo(pi) > 0) {
            reduced = reduced.subtract(twoPi);
        } else if (reduced.compareTo(pi.negate()) < 0) {
            reduced = reduced.add(twoPi);
        }
        return reduced;
    }

    private static boolean isInteger(String value) {
        return value.indexOf('.') < 0;
    }

    private static int workingScale(
            BuiltInOperator operator, String left, String right, int precision) {
        long leftOrder = decimalOrder(left);
        long lowestOrder = leftOrder;
        if (right != null) {
            long rightOrder = decimalOrder(right);
            Long integerExponent = integerAsLong(right);
            lowestOrder = Math.min(lowestOrder, rightOrder);
            long resultOrder = switch (operator) {
                case MULTIPLY -> leftOrder + rightOrder;
                case DIVIDE -> leftOrder - rightOrder;
                case POWER -> integerExponent == null
                        ? leftOrder : leftOrder * integerExponent;
                default -> Math.min(leftOrder, rightOrder);
            };
            lowestOrder = Math.min(lowestOrder, resultOrder);
        }
        long fractionalRange = Math.max(0, -lowestOrder);
        return Math.toIntExact(Math.addExact(Math.addExact(precision, GUARD_DIGITS), fractionalRange));
    }

    private static long decimalOrder(String canonical) {
        String value = canonical.charAt(0) == '-' ? canonical.substring(1) : canonical;
        int point = value.indexOf('.');
        if (point < 0) return value.length() - 1L;
        if (point > 1) return point - 1L;
        int first = 2;
        while (first < value.length() && value.charAt(first) == '0') first++;
        return 1L - first;
    }

    private static Long integerAsLong(String canonical) {
        if (!isInteger(canonical)) return null;
        try {
            return Long.parseLong(canonical);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Halved halve(String decimal) {
        StringBuilder quotient = new StringBuilder(decimal.length());
        int carry = 0;
        for (int index = 0; index < decimal.length(); index++) {
            int current = carry * 10 + decimal.charAt(index) - '0';
            if (!quotient.isEmpty() || current / 2 != 0) {
                quotient.append(current / 2);
            }
            carry = current % 2;
        }
        return new Halved(quotient.isEmpty() ? "0" : quotient.toString(), carry != 0);
    }

    private static UncalculableException domain(String message) {
        return new UncalculableException(CalculationError.DOMAIN_ERROR, message);
    }

    private record Halved(String quotient, boolean odd) {
    }

    private static final class Decimal implements Comparable<Decimal> {
        private final int sign;
        private final Unsigned magnitude;
        private final int scale;

        private Decimal(int sign, Unsigned magnitude, int scale) {
            this.sign = magnitude.isZero() ? 0 : sign;
            this.magnitude = magnitude;
            this.scale = scale;
        }

        static Decimal parse(String value, int scale) {
            boolean negative = value.charAt(0) == '-';
            String text = negative ? value.substring(1) : value;
            int point = text.indexOf('.');
            int fractionDigits = point < 0 ? 0 : text.length() - point - 1;
            String digits = point < 0 ? text : text.substring(0, point) + text.substring(point + 1);
            Unsigned magnitude = Unsigned.parse(digits);
            if (fractionDigits < scale) {
                magnitude = magnitude.multiplyPowerOfTen(scale - fractionDigits);
            } else if (fractionDigits > scale) {
                magnitude = magnitude.dividePowerOfTenRounded(fractionDigits - scale);
            }
            return new Decimal(negative ? -1 : 1, magnitude, scale);
        }

        static Decimal one(int scale) {
            return powerOfTen(0, scale);
        }

        static Decimal fromLong(long value, int scale) {
            return parse(Long.toString(value), scale);
        }

        static Decimal powerOfTen(int exponent, int scale) {
            long zeroCount = (long) scale + exponent;
            if (zeroCount < 0) {
                return new Decimal(0, Unsigned.ZERO, scale);
            }
            return new Decimal(1, Unsigned.powerOfTen(Math.toIntExact(zeroCount)), scale);
        }

        int signum() {
            return sign;
        }

        boolean isZero() {
            return sign == 0;
        }

        Decimal add(Decimal other) {
            requireScale(other);
            if (sign == 0) return other;
            if (other.sign == 0) return this;
            if (sign == other.sign) {
                return new Decimal(sign, magnitude.add(other.magnitude), scale);
            }
            int comparison = magnitude.compareTo(other.magnitude);
            if (comparison == 0) return new Decimal(0, Unsigned.ZERO, scale);
            return comparison > 0
                    ? new Decimal(sign, magnitude.subtract(other.magnitude), scale)
                    : new Decimal(other.sign, other.magnitude.subtract(magnitude), scale);
        }

        Decimal subtract(Decimal other) {
            return add(other.negate());
        }

        Decimal negate() {
            return new Decimal(-sign, magnitude, scale);
        }

        Decimal abs() {
            return sign < 0 ? negate() : this;
        }

        Decimal multiply(Decimal other) {
            requireScale(other);
            return new Decimal(sign * other.sign,
                    magnitude.multiply(other.magnitude).dividePowerOfTenRounded(scale), scale);
        }

        Decimal multiplySmall(int factor) {
            if (factor == 0 || isZero()) return new Decimal(0, Unsigned.ZERO, scale);
            int resultSign = factor < 0 ? -sign : sign;
            return new Decimal(resultSign, magnitude.multiplySmall(Math.abs(factor)), scale);
        }

        Decimal multiplyLong(long factor) {
            if (factor >= Integer.MIN_VALUE && factor <= Integer.MAX_VALUE) {
                return multiplySmall((int) factor);
            }
            return multiply(parse(Long.toString(factor), scale));
        }

        Decimal divide(Decimal other) {
            requireScale(other);
            if (other.isZero()) {
                throw new UncalculableException(CalculationError.DIVISION_BY_ZERO, "Division by zero");
            }
            Unsigned.Division division = magnitude.multiplyPowerOfTen(scale).divide(other.magnitude);
            Unsigned quotient = roundQuotient(division.quotient, division.remainder, other.magnitude);
            return new Decimal(sign * other.sign, quotient, scale);
        }

        Decimal divideSmall(int divisor) {
            if (divisor == 0) {
                throw new UncalculableException(CalculationError.DIVISION_BY_ZERO, "Division by zero");
            }
            Unsigned.SmallDivision division = magnitude.divideSmall(Math.abs(divisor));
            Unsigned quotient = division.quotient;
            long doubled = (long) division.remainder * 2;
            int absoluteDivisor = Math.abs(divisor);
            if (doubled > absoluteDivisor
                    || doubled == absoluteDivisor && quotient.isOdd()) {
                quotient = quotient.addSmall(1);
            }
            return new Decimal(divisor < 0 ? -sign : sign, quotient, scale);
        }

        Decimal remainder(Decimal divisor) {
            requireScale(divisor);
            if (divisor.isZero()) {
                throw new UncalculableException(CalculationError.DIVISION_BY_ZERO, "Division by zero");
            }
            return new Decimal(sign, magnitude.divide(divisor.magnitude).remainder, scale);
        }

        int decimalOrder() {
            return magnitude.decimalDigits() - scale - 1;
        }

        boolean atMostOneUlp() {
            return magnitude.compareTo(Unsigned.ONE) <= 0;
        }

        Number toNumber(int precision) {
            Unsigned rounded = magnitude.roundToSignificantDigits(precision);
            String digits = rounded.toDecimal();
            String plain;
            if (scale == 0) {
                plain = digits;
            } else if (digits.length() <= scale) {
                plain = "0." + "0".repeat(scale - digits.length()) + digits;
            } else {
                int point = digits.length() - scale;
                plain = digits.substring(0, point) + "." + digits.substring(point);
            }
            return Number.fromString(sign < 0 && !rounded.isZero() ? "-" + plain : plain);
        }

        @Override
        public int compareTo(Decimal other) {
            requireScale(other);
            if (sign != other.sign) return Integer.compare(sign, other.sign);
            return sign == 0 ? 0 : sign * magnitude.compareTo(other.magnitude);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Decimal other
                    && sign == other.sign && scale == other.scale && magnitude.equals(other.magnitude);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * sign + scale) + magnitude.hashCode();
        }

        private void requireScale(Decimal other) {
            if (scale != other.scale) throw new IllegalArgumentException("Decimal scales differ");
        }

        private static Unsigned roundQuotient(
                Unsigned quotient, Unsigned remainder, Unsigned divisor) {
            int comparison = remainder.multiplySmall(2).compareTo(divisor);
            return comparison > 0 || comparison == 0 && quotient.isOdd()
                    ? quotient.addSmall(1) : quotient;
        }
    }

    /** Non-negative integer implemented as little-endian base-1,000,000,000 limbs. */
    private static final class Unsigned implements Comparable<Unsigned> {
        private static final int BASE = 1_000_000_000;
        private static final Unsigned ZERO = new Unsigned(new int[0]);
        private static final Unsigned ONE = new Unsigned(new int[] {1});
        private final int[] limbs;

        private Unsigned(int[] limbs) {
            int length = limbs.length;
            while (length > 0 && limbs[length - 1] == 0) length--;
            this.limbs = length == limbs.length ? limbs : Arrays.copyOf(limbs, length);
        }

        static Unsigned parse(String decimal) {
            int first = 0;
            while (first < decimal.length() && decimal.charAt(first) == '0') first++;
            if (first == decimal.length()) return ZERO;
            int count = (decimal.length() - first + 8) / 9;
            int[] limbs = new int[count];
            int limb = 0;
            for (int end = decimal.length(); end > first; end -= 9) {
                int start = Math.max(first, end - 9);
                limbs[limb++] = Integer.parseInt(decimal.substring(start, end));
            }
            return new Unsigned(limbs);
        }

        static Unsigned powerOfTen(int exponent) {
            return parse("1" + "0".repeat(exponent));
        }

        boolean isZero() {
            return limbs.length == 0;
        }

        boolean isOdd() {
            return !isZero() && (limbs[0] & 1) != 0;
        }

        int decimalDigits() {
            if (isZero()) return 1;
            return (limbs.length - 1) * 9 + Integer.toString(limbs[limbs.length - 1]).length();
        }

        String toDecimal() {
            if (isZero()) return "0";
            StringBuilder result = new StringBuilder(decimalDigits());
            result.append(limbs[limbs.length - 1]);
            for (int index = limbs.length - 2; index >= 0; index--) {
                String part = Integer.toString(limbs[index]);
                result.append("0".repeat(9 - part.length())).append(part);
            }
            return result.toString();
        }

        Unsigned add(Unsigned other) {
            int length = Math.max(limbs.length, other.limbs.length);
            int[] result = new int[length + 1];
            long carry = 0;
            for (int index = 0; index < length; index++) {
                long sum = carry + limb(index) + other.limb(index);
                result[index] = (int) (sum % BASE);
                carry = sum / BASE;
            }
            result[length] = (int) carry;
            return new Unsigned(result);
        }

        Unsigned addSmall(int value) {
            return add(new Unsigned(new int[] {value}));
        }

        Unsigned subtract(Unsigned other) {
            int[] result = new int[limbs.length];
            long borrow = 0;
            for (int index = 0; index < limbs.length; index++) {
                long difference = (long) limb(index) - other.limb(index) - borrow;
                if (difference < 0) {
                    difference += BASE;
                    borrow = 1;
                } else {
                    borrow = 0;
                }
                result[index] = (int) difference;
            }
            return new Unsigned(result);
        }

        Unsigned multiply(Unsigned other) {
            if (isZero() || other.isZero()) return ZERO;
            int[] result = new int[limbs.length + other.limbs.length];
            for (int left = 0; left < limbs.length; left++) {
                long carry = 0;
                for (int right = 0; right < other.limbs.length; right++) {
                    int position = left + right;
                    long product = (long) limbs[left] * other.limbs[right]
                            + result[position] + carry;
                    result[position] = (int) (product % BASE);
                    carry = product / BASE;
                }
                int position = left + other.limbs.length;
                while (carry != 0) {
                    long sum = (long) result[position] + carry;
                    result[position] = (int) (sum % BASE);
                    carry = sum / BASE;
                    position++;
                }
            }
            return new Unsigned(result);
        }

        Unsigned multiplySmall(int factor) {
            if (factor == 0 || isZero()) return ZERO;
            int[] result = new int[limbs.length + 1];
            long carry = 0;
            for (int index = 0; index < limbs.length; index++) {
                long product = (long) limbs[index] * factor + carry;
                result[index] = (int) (product % BASE);
                carry = product / BASE;
            }
            result[limbs.length] = (int) carry;
            return new Unsigned(result);
        }

        Unsigned multiplyPowerOfTen(int exponent) {
            if (isZero() || exponent == 0) return this;
            return parse(toDecimal() + "0".repeat(exponent));
        }

        Unsigned dividePowerOfTenRounded(int exponent) {
            if (exponent == 0 || isZero()) return this;
            String decimal = toDecimal();
            int cut = decimal.length() - exponent;
            if (cut < 0) return ZERO;
            String kept = cut == 0 ? "0" : decimal.substring(0, cut);
            String discarded = decimal.substring(Math.max(0, cut));
            char first = discarded.isEmpty() ? '0' : discarded.charAt(0);
            boolean later = discarded.length() > 1
                    && discarded.substring(1).chars().anyMatch(character -> character != '0');
            Unsigned quotient = parse(kept);
            if (first > '5' || first == '5' && (later || quotient.isOdd())) {
                quotient = quotient.addSmall(1);
            }
            return quotient;
        }

        Unsigned roundToSignificantDigits(int precision) {
            int discarded = decimalDigits() - precision;
            return discarded <= 0 ? this
                    : dividePowerOfTenRounded(discarded).multiplyPowerOfTen(discarded);
        }

        SmallDivision divideSmall(int divisor) {
            int[] result = new int[limbs.length];
            long remainder = 0;
            for (int index = limbs.length - 1; index >= 0; index--) {
                long current = remainder * BASE + limbs[index];
                result[index] = (int) (current / divisor);
                remainder = current % divisor;
            }
            return new SmallDivision(new Unsigned(result), (int) remainder);
        }

        Division divide(Unsigned divisor) {
            if (divisor.isZero()) throw new ArithmeticException("Division by zero");
            if (compareTo(divisor) < 0) return new Division(ZERO, this);
            String dividend = toDecimal();
            StringBuilder quotient = new StringBuilder(dividend.length());
            Unsigned remainder = ZERO;
            for (int index = 0; index < dividend.length(); index++) {
                remainder = remainder.multiplySmall(10).addSmall(dividend.charAt(index) - '0');
                int low = 0;
                int high = 9;
                while (low < high) {
                    int middle = (low + high + 1) >>> 1;
                    if (divisor.multiplySmall(middle).compareTo(remainder) <= 0) low = middle;
                    else high = middle - 1;
                }
                quotient.append((char) ('0' + low));
                if (low != 0) remainder = remainder.subtract(divisor.multiplySmall(low));
            }
            return new Division(parse(quotient.toString()), remainder);
        }

        private int limb(int index) {
            return index < limbs.length ? limbs[index] : 0;
        }

        @Override
        public int compareTo(Unsigned other) {
            if (limbs.length != other.limbs.length) {
                return Integer.compare(limbs.length, other.limbs.length);
            }
            for (int index = limbs.length - 1; index >= 0; index--) {
                if (limbs[index] != other.limbs[index]) {
                    return Integer.compare(limbs[index], other.limbs[index]);
                }
            }
            return 0;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Unsigned other && Arrays.equals(limbs, other.limbs);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(limbs);
        }

        private record SmallDivision(Unsigned quotient, int remainder) {
        }

        private record Division(Unsigned quotient, Unsigned remainder) {
        }
    }
}
