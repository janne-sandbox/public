# Math2

Math2 is an arbitrary-precision decimal calculation toolkit. It contains a standalone Java JAR, a local-first Spring Boot REST service with a persistent bounded cache, and client libraries for JavaScript, TypeScript, C#, C++, and Rust.

The complete multi-page guide starts at [documentation/index.html](documentation/index.html). It includes Mermaid architecture diagrams, engine internals, REST examples, per-language usage, deployment configuration, and reproducible H2-backed performance samples.

## Quick start

Requirements: Java 21+, Maven 3.9+, and `curl`.

```bash
./run-dev-local.sh
./open-tabs.sh
```

The service listens on `127.0.0.1:8080` by default. Swagger UI is at `http://127.0.0.1:8080/swagger-ui.html`.

```bash
curl --get --data-urlencode 'equation=sqrt(9) + 2^3' \
  http://127.0.0.1:8080/api/v1/calculate
```

Stop it with `./stop-dev-local.sh`.

## Java API

```java
Equation equation = Number.fromInteger(9).sqrt()
    .add(Number.fromInteger(2).power(Number.fromInteger(3)));
Number result = equation.calculate(100);
```

`Number` retains its canonical value as a decimal string, and its operation methods build immutable equations for deferred arbitrary-precision evaluation. The evaluator is implemented by Math2 itself with decimal strings and base-1,000,000,000 limb arrays; it does not delegate calculations to Apfloat, `BigDecimal`, `BigInteger`, or another numeric engine. `Number.pi()` returns pi at the configured maximum precision, while parsed Java and REST expressions accept `pi` and evaluate it at the request precision. Supported operations are `+`, `-`, `*`, `/`, `^`, `sqrt`, `abs`, `min`, `max`, `log`, `sin`, `cos`, and `tan`. The default significant-digit limit is 1000 and can be changed through `Number.setMaxLength`.

## REST API

- `GET /api/v1/calculate?equation=...&precision=...`
- `GET /api/v1/calculate/batch?equations=...,...&precision=...`
- `GET /actuator/health`
- `GET /v3/api-docs`

The batch splitter preserves commas inside nested functions such as `max(2,3)`. Success values are decimal strings so JSON parsers cannot lose precision.

Configuration uses environment variables including `MATH2_SERVER_ADDRESS`, `MATH2_SERVER_PORT`, `MATH2_DATASOURCE_URL`, `MATH2_CACHE_MAX_ENTRIES`, `MATH2_MAX_BATCH_SIZE`, `MATH2_MAX_EXPRESSION_LENGTH`, and `MATH2_MAX_LENGTH` for the maximum significant-digit precision.

## Verification

```bash
./test-java.sh
./run-smoke-test.sh
./test-client-integration.sh
./test-all.sh
```

`test-client-integration.sh` starts the packaged service on isolated loopback port 18080 with caching disabled, runs single and ordered-batch requests through the JavaScript, TypeScript, C++, .NET, and Rust clients, and stops the service afterward. `test-all.sh` runs the Java reactor followed by this live cross-language verification and explicitly reports unavailable .NET or Rust tooling. See [CONTINUE.md](CONTINUE.md) for current executed evidence and remaining blockers.

The documentation performance sample is produced by an H2-backed Spring integration test:

```bash
mvn -q -pl java-rest-service -am \
  -Dtest=DocumentationPerformanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test -Ddebug=false
```

On the recorded Apple-arm64 development environment, representative 1,000-digit engine-version-3 misses took 1 ms for integer arithmetic, 8 ms for `1/7`, 36 ms for `pi`, and 125 ms for `sqrt(2)`. These are evaluator durations persisted in H2 from one warmed test run, not HTTP latency or benchmark guarantees. See [performance evidence](documentation/performance.html) for the method and complete environment.

## Documentation

[Math2](https://janne-sandbox.github.io/public/math2/documentation/)

- [Getting started](documentation/getting-started.html)
- [Architecture overview](documentation/architecture.html), [calculation engine](documentation/architecture-engine.html), and [REST/cache](documentation/architecture-service.html)
- [Operations and precision](documentation/operations.html)
- [REST reference](documentation/api.html) and [worked examples](documentation/rest-examples.html)
- [Client library overview](documentation/clients.html): [Java](documentation/client-java.html), [JavaScript](documentation/client-javascript.html), [TypeScript](documentation/client-typescript.html), [.NET](documentation/client-dotnet.html), [C++](documentation/client-cpp.html), and [Rust](documentation/client-rust.html)
- [Performance](documentation/performance.html) and [configuration/Docker](documentation/deployment.html)

## Modules

- `java-api`: calculation value types, operators, parser, and evaluator.
- `java-rest-service`: Spring Boot API, Swagger UI, H2/Hibernate/Liquibase cache, and Dockerfile.
- `js-api`, `ts-api`, `dotnet-api`, `cpp-api`, `rust-api`: REST clients with native `Number` and `Equation` types.
- `requirements`: normative functional, class, and non-functional specifications.
- `documentation`: static project documentation site.

Build the container from the repository root:

```bash
docker build -f java-rest-service/Dockerfile -t math2:local .
docker run --rm -p 127.0.0.1:8080:8080 -v math2-data:/app/data math2:local
```
