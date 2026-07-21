import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import http from "node:http";
import { Equation, Math2Client, Number, ServiceError, ValidationError } from "../src/index.js";

let server;
let baseUrl;

before(async () => {
  server = http.createServer((request, response) => {
    const url = new URL(request.url, "http://localhost");
    response.setHeader("content-type", "application/json");
    if (url.searchParams.get("equation") === "bad") {
      response.statusCode = 400;
      response.end(JSON.stringify({ code: "INVALID_EXPRESSION", message: "bad", position: 0 }));
    } else if (url.pathname.endsWith("/batch")) {
      response.end(JSON.stringify({ results: ["2", "3"] }));
    } else {
      response.end(JSON.stringify({ result: "14" }));
    }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});
after(() => new Promise(resolve => {
  server.closeAllConnections();
  server.close(resolve);
}));

test("Number canonicalizes decimal strings", () => {
  assert.equal(Number.fromString("001.2300e2").toString(), "123");
  assert(Number.zero().equals(Number.fromString("-0.0")));
  assert.throws(() => Number.fromString("nope"), ValidationError);
});

test("Equation composes immutably", () => {
  const two = Equation.of(Number.fromInteger(2));
  assert.equal(two.add(Equation.of(Number.fromInteger(3))).sqrt().toString(), "sqrt((2+3))");
  assert.equal(two.toString(), "2");
});

test("client calculates single and batch responses", async () => {
  const client = new Math2Client({ baseUrl });
  assert.equal((await client.calculate(Equation.parse("2+3*4"))).toString(), "14");
  const values = await client.calculateBatch([Equation.parse("1+1"), Equation.parse("max(2,3)")]);
  assert.deepEqual(values.map(String), ["2", "3"]);
});

test("client exposes structured service errors", async () => {
  const client = new Math2Client({ baseUrl });
  await assert.rejects(() => client.calculate(Equation.parse("bad")), error =>
    error instanceof ServiceError && error.status === 400 && error.position === 0);
});
