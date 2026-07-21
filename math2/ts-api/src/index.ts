export class Math2Error extends Error {}
export class ValidationError extends Math2Error {}
export class TransportError extends Math2Error {}
export class ProtocolError extends Math2Error {}

export class ServiceError extends Math2Error {
  readonly status: number;
  readonly code: string;
  readonly position: number | null;

  constructor(status: number, code: string, message: string, position: number | null = null) {
    super(message);
    this.status = status;
    this.code = code;
    this.position = position;
  }
}

export class Number {
  readonly #value: string;

  private constructor(value: string) { this.#value = canonicalDecimal(value); }
  static fromString(value: string): Number { return new Number(value); }
  static fromInteger(value: number): Number {
    if (!globalThis.Number.isSafeInteger(value)) throw new ValidationError("Integer must be safe");
    return new Number(String(value));
  }
  static fromFloat(value: number): Number {
    if (!globalThis.Number.isFinite(value)) throw new ValidationError("Number must be finite");
    return new Number(String(value));
  }
  static zero(): Number { return new Number("0"); }
  toString(): string { return this.#value; }
  equals(other: unknown): boolean { return other instanceof Number && this.#value === other.#value; }
}

export class Equation {
  readonly #expression: string;

  private constructor(expression: string) {
    if (expression.trim() === "") throw new ValidationError("Expression must not be blank");
    this.#expression = expression.trim();
  }
  static parse(expression: string): Equation { return new Equation(expression); }
  static of(number: Number): Equation {
    if (!(number instanceof Number)) throw new ValidationError("Expected a Math2 Number");
    return new Equation(number.toString());
  }
  add(other: Equation): Equation { return this.binary("+", other); }
  subtract(other: Equation): Equation { return this.binary("-", other); }
  multiply(other: Equation): Equation { return this.binary("*", other); }
  divide(other: Equation): Equation { return this.binary("/", other); }
  power(other: Equation): Equation { return this.binary("^", other); }
  min(other: Equation): Equation { return this.fn("min", other); }
  max(other: Equation): Equation { return this.fn("max", other); }
  sqrt(): Equation { return this.unary("sqrt"); }
  abs(): Equation { return this.unary("abs"); }
  log(): Equation { return this.unary("log"); }
  sin(): Equation { return this.unary("sin"); }
  cos(): Equation { return this.unary("cos"); }
  tan(): Equation { return this.unary("tan"); }
  toString(): string { return this.#expression; }

  private binary(operator: string, other: Equation): Equation {
    requireEquation(other);
    return new Equation(`(${this}${operator}${other})`);
  }
  private fn(name: string, other: Equation): Equation {
    requireEquation(other);
    return new Equation(`${name}(${this},${other})`);
  }
  private unary(name: string): Equation { return new Equation(`${name}(${this})`); }
}

export interface Math2ClientOptions {
  baseUrl?: string;
  timeoutMs?: number;
  fetchImpl?: typeof fetch;
}

export class Math2Client {
  readonly baseUrl: string;
  readonly timeoutMs: number;
  readonly #fetch: typeof fetch;

  constructor(options: Math2ClientOptions = {}) {
    this.baseUrl = (options.baseUrl ?? "http://127.0.0.1:8080").replace(/\/$/, "");
    this.timeoutMs = options.timeoutMs ?? 10_000;
    this.#fetch = options.fetchImpl ?? globalThis.fetch;
    if (typeof this.#fetch !== "function") throw new ValidationError("fetch is required");
    if (!globalThis.Number.isInteger(this.timeoutMs) || this.timeoutMs < 1) {
      throw new ValidationError("timeoutMs must be positive");
    }
  }

  async calculate(equation: Equation, precision?: number): Promise<Number> {
    requireEquation(equation);
    const data = await this.get("/api/v1/calculate", { equation: equation.toString(), precision });
    if (!isRecord(data) || typeof data.result !== "string") throw new ProtocolError("Missing result");
    return Number.fromString(data.result);
  }

  async calculateBatch(equations: readonly Equation[], precision?: number): Promise<readonly Number[]> {
    if (equations.length === 0) throw new ValidationError("Equations must not be empty");
    equations.forEach(requireEquation);
    const data = await this.get("/api/v1/calculate/batch", {
      equations: equations.map(String).join(","), precision
    });
    if (!isRecord(data) || !Array.isArray(data.results) || data.results.length !== equations.length
        || !data.results.every(value => typeof value === "string")) {
      throw new ProtocolError("Response result count does not match request");
    }
    return Object.freeze(data.results.map(value => Number.fromString(value as string)));
  }

  private async get(path: string, parameters: Record<string, string | number | undefined>): Promise<unknown> {
    const url = new URL(this.baseUrl + path);
    Object.entries(parameters).forEach(([key, value]) => {
      if (value !== undefined) url.searchParams.set(key, String(value));
    });
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    let response: Response;
    try {
      response = await this.#fetch(url, { signal: controller.signal, headers: { accept: "application/json" } });
    } catch (error) {
      const cause = error as Error;
      throw new TransportError(cause.name === "AbortError" ? "Request timed out" : `Request failed: ${cause.message}`);
    } finally {
      clearTimeout(timer);
    }
    let data: unknown;
    try { data = await response.json(); } catch { throw new ProtocolError("Response is not valid JSON"); }
    if (!response.ok) {
      const body = isRecord(data) ? data : {};
      throw new ServiceError(response.status,
        typeof body.code === "string" ? body.code : "UNKNOWN",
        typeof body.message === "string" ? body.message : "Service error",
        typeof body.position === "number" ? body.position : null);
    }
    return data;
  }
}

function requireEquation(value: unknown): asserts value is Equation {
  if (!(value instanceof Equation)) throw new ValidationError("Expected an Equation");
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function canonicalDecimal(input: string): string {
  const match = /^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$/.exec(input.trim());
  if (!match || (!match[2] && !match[3])) throw new ValidationError(`Invalid decimal number: ${input}`);
  const sign = match[1] === "-" ? "-" : "";
  const integer = match[2] ?? "";
  const fraction = match[3] ?? "";
  const exponent = globalThis.Number.parseInt(match[4] ?? "0", 10);
  const digits = (integer + fraction).replace(/^0+/, "");
  if (!digits || /^0+$/.test(digits)) return "0";
  const decimalIndex = integer.length + exponent - (integer + fraction).length + digits.length;
  let result: string;
  if (decimalIndex <= 0) result = "0." + "0".repeat(-decimalIndex) + digits;
  else if (decimalIndex >= digits.length) result = digits + "0".repeat(decimalIndex - digits.length);
  else result = digits.slice(0, decimalIndex) + "." + digits.slice(decimalIndex);
  result = result.replace(/(\.\d*?)0+$/, "$1").replace(/\.$/, "");
  return sign + result;
}
