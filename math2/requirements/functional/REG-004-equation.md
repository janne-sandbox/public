# REG-004 — Equation

## Requirements

1. `Equation` is an immutable ordered composition of `Number` operands and `Operator` nodes.
2. Builder methods on both `Number` and `Equation` cover unary and binary operations so an equation can be constructed and chained without parsing text or manually wrapping every operand.
3. `Equation.parse(String)` parses the grammar defined in REG-003, including nested parentheses and unary signs.
4. `calculate()` returns one `Number` and may be invoked repeatedly with the same result.
5. `calculate(int precision)` evaluates with the requested positive number of significant decimal digits, capped by `Number.getMaxLength()`.
6. Whitespace has no semantic meaning.
7. Empty input, missing operands, unmatched parentheses, unknown operators/functions, extra tokens, and invalid numbers throw `UncalculableException` with a useful input position when available.
8. The canonical expression representation is deterministic and suitable as a cache-key input.
9. The canonical `pi` node is retained until evaluation so its value uses the precision requested by `calculate(int)`.

## Acceptance examples

- `Equation.parse("2 + 3 * 4").calculate()` returns `14`.
- `Equation.parse("(2 + 3) * 4").calculate()` returns `20`.
- `Equation.parse("sqrt(9) + abs(-2)").calculate()` returns `5`.
- `Equation.parse("2 ^ 3 ^ 2").calculate()` returns `512`.
- `Equation.parse("2 * pi").calculate(1000)` evaluates pi at 1000 significant digits.
- `Equation.parse("(1 + 2").calculate()` fails as uncalculable.
