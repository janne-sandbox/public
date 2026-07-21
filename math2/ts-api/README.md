# Math2 TypeScript client

Strongly typed, zero-dependency client exposing immutable `Number`, `Equation`, and `Math2Client` classes.

```ts
const equation = Equation.of(Number.fromInteger(9)).sqrt();
const result = await new Math2Client().calculate(equation);
```

The source can be consumed directly by modern TypeScript-aware runtimes or compiled by a project toolchain. Node.js 22.6+ can execute the included tests directly with `npm test`.
