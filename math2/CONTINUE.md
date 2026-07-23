# Math2 delivery tracker

## Goal

Build a Maven-based arbitrary-precision calculation library, a Spring Boot REST service with a bounded persistent H2 cache, and matching client libraries for C++, C#, JavaScript, Rust, and TypeScript.

## Working decisions

- Java 21 and Maven are the baseline.
- `com.nologo2.math2.Number` is an immutable decimal value. Its default maximum precision is 1000 significant decimal digits and is globally configurable.
- Operators are represented by library types and composed in `Equation`; raw expression parsing is supported at system boundaries.
- Transcendental functions use a precision-aware decimal implementation rather than `double` internally.
- The REST API accepts one equation or a comma-separated batch and returns results in input order.
- Conversion methods reject lossy conversions with an exception that carries the nearest representable value.
- Work proceeds in dependency order: specifications, Java library, REST service, then non-Java clients.

## Progress

### 1. Specifications

- [x] Define `Number` value and conversion behavior.
- [x] Define `Equation`, operator model, parsing, and evaluation.
- [x] Define errors and precision rules.
- [x] Define REST endpoints, request syntax, and response/error contracts.
- [x] Define persistent bounded cache behavior.
- [x] Define non-Java client contract.
- [x] Define packaging, local tooling, documentation, and quality requirements.

### 2. Java calculation library (`java-api`)

- [x] Configure the Maven reactor and module build.
- [x] Implement `Number`, precision configuration, and exact conversions.
- [x] Implement operator abstraction and supported operators.
- [x] Implement immutable `Equation` composition and expression parsing.
- [x] Implement arbitrary-precision evaluation for arithmetic and functions.
- [x] Add boundary, malformed-input, precision, and operator unit tests.
- [x] Refactor `Number` to retain its canonical decimal value as a `String` rather than storing a `BigDecimal`, including 1000-digit inputs.
- [x] Add fluent calculation methods that build immutable `Equation` instances from `Number` values and verify 1000-digit arithmetic.
- [x] Remove `BigInteger` conversion bounds and implementation details from `Number`; perform primitive range and nearest-value handling on canonical strings.
- [x] Remove static integer/long boundary literals from `Number`; primitive limits exist only inside explicit primitive conversion behavior.
- [x] Add configurable-precision `Number.pi()` and a canonical `pi` expression constant usable through Java equations and the REST API, including 1000+ digit coverage.
- [x] Remove `BigDecimal` and `BigDecimalMath` from the calculation engine and mathematical constants; use an arbitrary-precision engine without a 32-bit decimal scale.
- [x] Add regression tests proving the Java calculation path is free of `BigDecimal` and still supports arithmetic, functions, pi, and 1000+ configured digits.
- [x] Remove Apfloat and every third-party calculation engine; implement arbitrary-precision decimal arithmetic and functions inside `java-api`.
- [x] Add regression tests proving the calculation bytecode and dependencies are free of Apfloat and external numeric engines while retaining 1000+ digit arithmetic and pi.

### 3. Spring Boot REST service (`java-rest-service`)

- [x] Implement calculation endpoints and OpenAPI/Swagger UI.
- [x] Bind to `127.0.0.1` by default with configuration override.
- [x] Add H2 file storage, Hibernate, and Liquibase schema management.
- [x] Implement deterministic cache keys and configurable 100-entry LRU eviction.
- [x] Add controller, persistence, cache, validation, and integration tests.
- [x] Add Dockerfile and ignore H2 runtime files.
- [x] Record calculation duration in milliseconds in every persisted cache entry and Liquibase schema.
- [x] Make database-backed caching explicitly optional; disabled mode calculates without initializing or using datasource, Hibernate, repositories, or Liquibase.
- [x] Build and run-verify the `java-rest-service` Docker image, including health and calculation endpoints.
- [x] Add a database index for `calculation_cache.canonical_expression` without changing previously applied Liquibase changesets.
- [x] Add Liquibase table and column remarks documenting the calculation-cache schema without changing previously applied changesets.

### 4. Developer experience and documentation

- [x] Add `run-dev-local.sh`, `stop-dev-local.sh`, `open-tabs.sh`, and test scripts.
- [x] Replace the existing documentation site with Math2 content and subpages.
- [x] Complete root and Java README files.
- [x] Keep `AGENTS.md`, `CLAUDE.md`, and `DEEPSEEK.md` aligned with project workflow.
- [x] Rebuild `documentation/index.html` from the preferred `previous_index.html` visual direction and split the expanded guide into chapter/subtopic pages.
- [x] Document Java and REST client library usage, REST examples, Mermaid architecture, configuration, operations, and measured H2-backed performance evidence.
- [x] Update the root README to link and summarize the expanded documentation.

### 5. REST client libraries (only after all Java tests pass)

- [x] JavaScript client and tests.
- [x] TypeScript client and tests.
- [x] C# client and tests.
- [x] C++ client and tests.
- [x] Rust client and tests.
- [x] Complete each client README.
- [x] Verify every REST client against one live packaged Math2 service, including single and ordered batch calculations.

### 6. Local toolchains

- [x] Verify or repair the existing .NET installation and run the C# test suite.
- [x] Install Rust/Cargo and run the Rust test suite.

## Current focus

All tracked delivery work is implemented and verified. Future work is optional hardening, packaging, and release automation rather than an open requirement in this tracker.

## Verification log

- 2026-07-21: Initial repository inventory complete. The project is a file scaffold and is not currently a Git worktree; all Maven and README files except the existing documentation page are empty.
- 2026-07-21: Functional, non-functional, and per-class Java API specifications created.
- 2026-07-21: `mvn verify` passes the three-module reactor with 18 Java API tests, 0 failures, 0 errors, and 0 skipped tests. Coverage includes factories, canonical values, precision limits, exact/lossy conversions, comparison, every operator, invalid domains, parsing precedence and functions, malformed expressions, fluent composition, and repeat evaluation.
- 2026-07-21: Added Liquibase changeset `002-add-calculation-duration`, the matching JPA field, and miss timing in `CalculationService`; integration tests verify persisted durations are non-negative.
- 2026-07-21: Added `MATH2_CACHE_ENABLED=false` support. The no-cache integration test and packaged loopback smoke test pass without datasource, Hibernate, repository, or Liquibase initialization.
- 2026-07-21: Existing global .NET SDK 10 lacked the runtime required by the `net8.0` client. Homebrew `dotnet@8` 8.0.129 was installed and the C# suite passes 2/2. Homebrew Rust/Cargo 1.97.1 was installed and the Rust suite passes 1/1.
- 2026-07-21: Built `math2:local` from `java-rest-service/Dockerfile`. A fresh non-root container reported actuator health `UP` and returned `{"result":"11"}` for `sqrt(9)+2^3`; the temporary container was then removed.
- 2026-07-21: `./test-all.sh` passes end-to-end: Java 27/27, JavaScript 4/4, TypeScript 2/2, C++ 1/1, C# 2/2, and Rust 1/1. The script selects the installed Homebrew .NET 8 runtime when available.
- 2026-07-21: Added Liquibase changeset `003-index-canonical-expression`, which creates `idx_cache_canonical_expression` without altering earlier changesets. `mvn -q -pl java-rest-service -am test -Ddebug=false` passes all 27 Java tests and confirms all three changesets apply successfully.
- 2026-07-21: Added Liquibase changeset `004-document-calculation-cache` with a table remark and remarks for all eight cache columns. The Java integration suite passes and confirms all remarks and all four changesets apply successfully to H2.
- 2026-07-21: Refactored Java `Number` so its sole value field is a canonical `String` and `Number.java` has no `BigDecimal` dependency. Added deferred fluent operations on `Number`, direct `Number` overloads on `Equation`, string-based canonicalization/comparison/conversion checks, and aligned requirements and documentation.
- 2026-07-21: `mvn -q -pl java-rest-service -am test -Ddebug=false` passes 31/31 Java tests. Coverage now includes adding one to a 1000-digit all-nines operand through both the Java fluent API and REST endpoint, returning the exact 1001-digit string without JSON numeric conversion.
- 2026-07-21: Removed the remaining `BigInteger` dependency from `Number`. Explicit `toInt()`/`toLong()` range checks, half-even rounding, and clamping now operate on canonical strings; boundary and overflow tests pass as part of the 31/31 Java suite.
- 2026-07-21: Removed the static `INT_MIN`, `INT_MAX`, `LONG_MIN`, and `LONG_MAX` literals from `Number`. Primitive parsing now occurs only inside `toInt()`/`toLong()`; overflow nearest values use the corresponding primitive constants without limiting stored or calculated decimal strings. The 31/31 Java suite passes.
- 2026-07-21: Added `Number.pi()` at the configured maximum precision and a case-insensitive, canonical `pi` equation node evaluated at each request's precision. Added service configuration `MATH2_MAX_LENGTH`, changed cached `result_value` to an unbounded CLOB through Liquibase changeset `005-unbound-result-length`, and documented Java/REST usage.
- 2026-07-21: `mvn -q -pl java-rest-service -am test -Ddebug=false` passes 34/34 Java tests. Tests verify 1000 significant digits from `Number.pi()` and REST, a configured 1200-digit Java/REST result, `2*pi` evaluation, persistence, and all five Liquibase changesets.
- 2026-07-21: Replaced `big-math`, `BigDecimal`, and `BigDecimalMath` with Apfloat 1.16.0. The evaluator uses Apfloat long precision, 12 guard digits, and explicit HALF_EVEN result rounding; pi uses the same engine. Scientific exponents are parsed as `long`, and cache engine version 2 prevents reuse of results from the previous evaluator.
- 2026-07-21: `mvn -q -pl java-rest-service -am test -Ddebug=false` passes 36/36 Java tests. Production `java-api` sources and dependencies contain no `BigDecimal`, `BigDecimalMath`, or `big-math`; a bytecode architecture test enforces this. Numerical coverage includes every operator/function, 1000/1200-digit pi, and exact 5000-digit carry arithmetic.
- 2026-07-21: Removed Apfloat and its Maven dependency. `DecimalArithmetic` now implements signed fixed-point values, base-1,000,000,000 limb addition/subtraction/multiplication, decimal long division, dynamic working scale, HALF_EVEN rounding, integer/fractional powers, Newton square root, logarithm/exponential and trigonometric series, and Machin-formula pi entirely inside Math2. REST cache engine version 3 isolates results from both earlier evaluators.
- 2026-07-21: `mvn -q -pl java-rest-service -am test -Ddebug=false` passes 38/38 Java API and REST tests with the internal engine. Regression coverage includes known 20-digit function values, 1000/1200-digit pi, exact 5000-digit carry arithmetic, and significant digits at decimal exponents down to `1e-2000`; recursive bytecode checks reject Apfloat, `BigDecimal`, `BigInteger`, and `BigDecimalMath` references from the engine and all its nested implementation classes.
- 2026-07-21: Rebuilt `documentation/index.html` using the preferred `previous_index.html` visual direction and expanded the static site into 17 Math2 pages: getting started; system, engine, and REST/cache architecture; operations; REST reference/examples; an overview plus separate Java, JavaScript, TypeScript, .NET, C++, and Rust usage pages; performance; and deployment. Four embedded Mermaid diagrams describe system context, request sequence, engine flow, and service persistence.
- 2026-07-21: Added `DocumentationPerformanceTest`, which warms the engine, calculates four precision-1000 samples through `CalculationService`, and reads the resulting H2 cache entities. The verified full-suite run recorded 1 ms for `(2+(3*4))`, 8 ms for `1/7`, 36 ms for `pi`, and 125 ms for `sqrt(2)` on Microsoft OpenJDK 25.0.1, Apple arm64/macOS 26.5.1, H2 2.4.240, and engine 3. The documentation labels these evaluator-only single-run examples rather than benchmark guarantees and publishes the raw CSV.
- 2026-07-21: `mvn -q -pl java-rest-service -am test -Ddebug=false` passes 39/39 tests with 0 failures, 0 errors, and 0 skipped. A local-link audit resolves every relative `href` in the expanded documentation. Root `README.md` links all chapters and the reproducible H2 collection command.
- 2026-07-21: Added opt-in live-service cases to JavaScript, TypeScript, C++, .NET, and Rust and added `test-client-integration.sh`. The runner starts the current packaged service with cache disabled on isolated loopback port 18080, verifies `sqrt(9)+2^3 = 11` and the ordered batch `1+1,max(2,3),sqrt(16) = [2,3,4]` through every native client, and always stops its child service.
- 2026-07-21: The updated `./test-all.sh` passes end-to-end with live REST transport: Java API/service 39/39, JavaScript 5/5, TypeScript 3/3, C++ 1/1, .NET 3/3, and Rust 2/2, with no failures or skipped tests. The restricted sandbox run required loopback permission; the approved loopback run completed successfully and left port 18080 closed.
- 2026-07-23: Replaced Math2's proprietary license with the canonical Apache License 2.0 text and aligned Maven, npm, Cargo, and NuGet package metadata on the `Apache-2.0` identifier. Repository-level licensing now maps Math2 to Apache-2.0 and the example project to LGPL-3.0-only, with a repository-wide `WARRANTY` disclaimer. License and metadata consistency checks passed; no behavioral tests were needed because runtime code was unchanged.

## Blockers

None.
