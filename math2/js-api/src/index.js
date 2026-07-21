export class Math2Error extends Error {}
export class ValidationError extends Math2Error {}
export class TransportError extends Math2Error {}
export class ProtocolError extends Math2Error {}

export class ServiceError extends Math2Error {
  constructor(status, code, message, position = null) {
    super(message);
    this.status = status;
    this.code = code;
    this.position = position;
  }
}

export class Number {
  #value;

  constructor(value) {
    this.#value = canonicalDecimal(String(value));
    Object.freeze(this);
  }

  static fromString(value) { return new Number(value); }
  static fromInteger(value) {
    if (!globalThis.Number.isSafeInteger(value)) throw new ValidationError("Integer must be safe");
    return new Number(String(value));
  }
  static fromFloat(value) {
    if (!globalThis.Number.isFinite(value)) throw new ValidationError("Number must be finite");
    return new Number(String(value));
  }
  static zero() { return new Number("0"); }
  toString() { return this.#value; }
  equals(other) { return other instanceof Number && this.#value === other.#value; }
}

export class Equation {
  #expression;

  constructor(expression) {
    if (typeof expression !== "string" || expression.trim() === "") {
      throw new ValidationError("Expression must not be blank");
    }
    this.#expression = expression.trim();
    Object.freeze(this);
  }

  static parse(expression) { return new Equation(expression); }
  static of(number) {
    if (!(number instanceof Number)) throw new ValidationError("Expected a Math2 Number");
    return new Equation(number.toString());
  }
  add(other) { return this.#binary("+", other); }
  subtract(other) { return this.#binary("-", other); }
  multiply(other) { return this.#binary("*", other); }
  divide(other) { return this.#binary("/", other); }
  power(other) { return this.#binary("^", other); }
  min(other) { return this.#function("min", other); }
  max(other) { return this.#function("max", other); }
  sqrt() { return this.#unary("sqrt"); }
  abs() { return this.#unary("abs"); }
  log() { return this.#unary("log"); }
  sin() { return this.#unary("sin"); }
  cos() { return this.#unary("cos"); }
  tan() { return this.#unary("tan"); }
  toString() { return this.#expression; }

  #binary(operator, other) {
    requireEquation(other);
    return new Equation(`(${this},${operator},${other})`.replace(`,${operator},`, operator));
  }
  #function(name, other) {
    requireEquation(other);
    return new Equation(`${name}(${this},${other})`);
  }
  #unary(name) { return new Equation(`${name}(${this})`); }
}

export class Math2Client {
  constructor({ baseUrl = "http://127.0.0.1:8080", timeoutMs = 10_000, fetchImpl = globalThis.fetch } = {}) {
    if (typeof fetchImpl !== "function") throw new ValidationError("fetch is required");
    if (!globalThis.Number.isInteger(timeoutMs) || timeoutMs < 1) throw new ValidationError("timeoutMs must be positive");
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.timeoutMs = timeoutMs;
    this.fetchImpl = fetchImpl;
  }

  async calculate(equation, precision) {
    requireEquation(equation);
    const data = await this.#get("/api/v1/calculate", { equation: equation.toString(), precision });
    if (typeof data?.result !== "string") throw new ProtocolError("Response does not contain a result");
    return Number.fromString(data.result);
  }

  async calculateBatch(equations, precision) {
    if (!Array.isArray(equations) || equations.length === 0) throw new ValidationError("Equations must not be empty");
    equations.forEach(requireEquation);
    const data = await this.#get("/api/v1/calculate/batch", {
      equations: equations.map(String).join(","), precision
    });
    if (!Array.isArray(data?.results) || data.results.length !== equations.length) {
      throw new ProtocolError("Response result count does not match request");
    }
    return data.results.map(Number.fromString);
  }

  async #get(path, parameters) {
    const url = new URL(this.baseUrl + path);
    for (const [key, value] of Object.entries(parameters)) {
      if (value !== undefined) url.searchParams.set(key, String(value));
    }
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    let response;
    try {
      response = await this.fetchImpl(url, { signal: controller.signal, headers: { accept: "application/json" } });
    } catch (error) {
      throw new TransportError(error.name === "AbortError" ? "Request timed out" : `Request failed: ${error.message}`);
    } finally {
      clearTimeout(timer);
    }
    let data;
    try { data = await response.json(); } catch { throw new ProtocolError("Response is not valid JSON"); }
    if (!response.ok) {
      throw new ServiceError(response.status, data?.code ?? "UNKNOWN", data?.message ?? "Service error", data?.position ?? null);
    }
    return data;
  }
}

function requireEquation(value) {
  if (!(value instanceof Equation)) throw new ValidationError("Expected an Equation");
}

function canonicalDecimal(input) {
  const match = /^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$/.exec(input.trim());
  if (!match || (!match[2] && !match[3])) throw new ValidationError(`Invalid decimal number: ${input}`);
  const sign = match[1] === "-" ? "-" : "";
  const integer = match[2] || "";
  const fraction = match[3] || "";
  const exponent = globalThis.Number.parseInt(match[4] || "0", 10);
  let digits = (integer + fraction).replace(/^0+/, "");
  if (!digits || /^0+$/.test(digits)) return "0";
  let decimalIndex = integer.length + exponent - (integer + fraction).length + digits.length;
  let result;
  if (decimalIndex <= 0) result = "0." + "0".repeat(-decimalIndex) + digits;
  else if (decimalIndex >= digits.length) result = digits + "0".repeat(decimalIndex - digits.length);
  else result = digits.slice(0, decimalIndex) + "." + digits.slice(decimalIndex);
  result = result.replace(/(\.\d*?)0+$/, "$1").replace(/\.$/, "");
  return sign + result;
}
