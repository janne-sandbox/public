import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import http from "node:http";
import { Equation, Math2Client, Number, ServiceError } from "../src/index.ts";

let server: http.Server;
let baseUrl: string;

before(async () => {
  server = http.createServer((request, response) => {
    const url = new URL(request.url ?? "/", "http://localhost");
    response.setHeader("content-type", "application/json");
    if (url.searchParams.get("equation") === "bad") {
      response.statusCode = 400;
      response.end(JSON.stringify({ code: "INVALID_EXPRESSION", message: "bad", position: 0 }));
    } else if (url.pathname.endsWith("/batch")) {
      response.end(JSON.stringify({ results: ["2", "3"] }));
    } else response.end(JSON.stringify({ result: "14" }));
  });
  await new Promise<void>(resolve => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  if (!address || typeof address === "string") throw new Error("No test address");
  baseUrl = `http://127.0.0.1:${address.port}`;
});
after(() => new Promise<void>(resolve => {
  server.closeAllConnections();
  server.close(() => resolve());
}));

test("typed values and equations compose", () => {
  assert.equal(Number.fromString("001.2300e2").toString(), "123");
  const equation = Equation.of(Number.fromInteger(2)).power(Equation.of(Number.fromInteger(8)));
  assert.equal(equation.toString(), "(2^8)");
});

test("typed client handles success and service errors", async () => {
  const client = new Math2Client({ baseUrl });
  assert.equal((await client.calculate(Equation.parse("2+3*4"))).toString(), "14");
  assert.deepEqual((await client.calculateBatch([Equation.parse("1+1"), Equation.parse("1+2")])).map(String), ["2", "3"]);
  await assert.rejects(() => client.calculate(Equation.parse("bad")),
    error => error instanceof ServiceError && error.code === "INVALID_EXPRESSION");
});

test("typed client calculates against a live Math2 service", {
  skip: !process.env.MATH2_TEST_BASE_URL
}, async () => {
  const client = new Math2Client({ baseUrl: process.env.MATH2_TEST_BASE_URL, timeoutMs: 30_000 });
  assert.equal((await client.calculate(Equation.parse("sqrt(9)+2^3"), 1000)).toString(), "11");
  const values = await client.calculateBatch([
    Equation.parse("1+1"), Equation.parse("max(2,3)"), Equation.parse("sqrt(16)")
  ], 1000);
  assert.deepEqual(values.map(String), ["2", "3", "4"]);
});
