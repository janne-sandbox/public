# Math2 JavaScript client

Zero-dependency ES module client for the Math2 REST API. Requires Node.js 18+ or a browser with `fetch`.

```js
import { Equation, Math2Client, Number } from "@nologo2/math2";

const equation = Equation.of(Number.fromInteger(9)).sqrt();
const result = await new Math2Client().calculate(equation);
console.log(result.toString()); // 3
```

Configure `baseUrl`, `timeoutMs`, or a custom `fetchImpl` in the client constructor. Run tests with `npm test`.
