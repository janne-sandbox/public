# Math2 delivery tracker

## Goal

Build a Maven-based arbitrary-precision calculation library, a Spring Boot REST service with a bounded persistent H2 cache, and matching client libraries for C++, C#, JavaScript, Python, Rust, and TypeScript.

## Working decisions

- Java 21 and Maven are the baseline.
- Maven coordinates use group `com.nologo`, Java packages use root `com.nologo.math2`, npm packages use scope `@nologo`, C++ uses namespace `nologo::math2`, and the Rust and Python distribution/import names use `nologo-math2` / `nologo_math2`.
- `com.nologo.math2.Number` is an immutable decimal value. Its default maximum precision is 1000 significant decimal digits and is globally configurable.
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
- [x] Add rational arithmetic reduction pass to `Equation`: each operation node attempts exact `BigInteger`-fraction evaluation before the decimal engine; operations not closed over the rationals (sqrt, log, trig, fractional power, pi) fall back to decimal. Eliminates intermediate precision loss for expressions such as `(1/3 + 2/3)` and `(1/3)*3` regardless of configured precision. Add `RationalReductionTest` (19 tests). Verification: `mvn verify` → 58 tests, 0 failures.
- [x] Add `round(x)`, `round(x, scale)`, and `round(x, scale, mode)` function to `Equation`. Supports 7 rounding modes (HALF_EVEN/0 default, HALF_UP/1, HALF_DOWN/2, CEILING/3, FLOOR/4, UP/5, DOWN/6). Rounding is applied to the exact rational when the operand reduces rationally, making `round(1/3, 2)` precision-independent. Canonical form always uses three arguments. Add `RoundTest` (18 tests). Verification: `mvn verify` → 76 tests, 0 failures.
- [x] Add `Number.getDefaultRoundingModeCode()` / `Number.setDefaultRoundingModeCode(int)` / `Number.getRoundingModeName(int)` / `Number.ROUNDING_MODE_COUNT`. Default captured at parse time so existing `Equation` instances are immutable. Configurable via `math2.rounding.default-mode-code` in application.properties. Published via `GET /api/v1/rounding` and `POST /api/v1/rounding/{code}` (REST controller). Add `DefaultRoundingModeTest` (11 tests, java-api) and `RoundingApiIntegrationTest` (6 tests, service). Verification: `mvn verify` → 93 tests, 0 failures.

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
- [x] Python client, examples, packaging, and tests.
- [x] Complete each client README.
- [x] Verify every REST client against one live packaged Math2 service, including single and ordered batch calculations.

### 6. Local toolchains

- [x] Verify or repair the existing .NET installation and run the C# test suite.
- [x] Install Rust/Cargo and run the Rust test suite.

### 7. Target GPU execution and UDP transport

- [x] Convert `GPU.txt` into a coherent target architecture in `GPU.md`.
- [x] Add normative execution-planner/backend/GPU requirements in `REG-009`.
- [x] Add normative optional UDP transport requirements in `REG-010`.
- [x] Specify exact CPU/CUDA result parity, configurable precision and rounding, ordered batch behavior, non-GPU CI, hardware release gates, and bounded UDP safety behavior.
- [x] Add a discoverable documentation page and README links that report the exact implementation boundary.
- [x] Phase 1: implement `CalculationContext`, `GpuMode`, immutable execution-plan model, and backend SPI without changing the active CPU calculation path.
- [x] Phase 2: route equations through the execution planner and reference CPU backend while preserving existing API/results.
- [x] Phase 3: add ordered batch planning, capability/cost selection, deterministic fake accelerated-backend coverage, and REST-service integration.
- [x] Phase 4 foundation: add the optional CUDA artifact, guarded JNI/device discovery, empty production operation capabilities, service registration, lifecycle tests, and deterministic non-hardware conformance fixtures.
- [x] Add a root Docker installer and dedicated operator guide for the digest-pinned CUDA 11.7/GPGPU-Sim environment, including AMD64 emulation, storage, overrides, smoke checking, cleanup, and evidence boundaries.
- [ ] Add a pinned GPGPU-Sim/Accel-Sim functional-simulation tier for compiled kernel probes; record simulator/CUDA/PTX versions and never count it as physical-hardware verification.
- [ ] Complete phase 4 with bounded CUDA memory pooling and resource cleanup tests.
- [ ] Implement exact CUDA execution kernels without breaking the completed optional artifact, deterministic non-hardware fixtures, or existing CPU behavior.
- [ ] Run and record CUDA hardware conformance tests for every advertised operation, precision, rounding mode, and ordered batch; exact CPU/CUDA canonical equality is required and skips do not count as CUDA verification.
- [x] Specify a vendor-neutral accelerated-backend policy: Docker and CUDA simulation are development tools, not runtime prerequisites; on a Metal-capable macOS laptop, `DETECT` may discover and select a conforming Metal provider, while `ENABLED` may use a conforming CUDA or Metal provider and otherwise fails closed.
- [x] Add the optional `java-metal-backend` foundation with guarded macOS/JNI loading, immutable device metadata, lazy service registration, empty production capabilities, lifecycle ownership, and focused specifications/tests.
- [x] Implement the discovery-only `math2_metal` JNI bridge and strict root build script; live-test opening, reporting, and releasing the default Apple Metal device without advertising calculation operations.
- [x] Add deterministic no-Metal fixtures covering exact reference parity, rounding modes, failures, fallback, fail-closed behavior, and ordered batches.
- [ ] Complete the Metal backend with bounded buffers and cleanup, custom exact-limb compute kernels, and declared Apple hardware conformance for every advertised operation, precision, rounding mode, failure, and ordered batch. Do not substitute native floating-point or tolerance-based comparisons for Math2 arbitrary precision.
- [x] Document macOS backend selection, current implementation boundary, build/device-test commands, and packaging; include `DISABLED` CPU-only behavior, `DETECT` fallback, `ENABLED` failure when no conforming accelerator exists, and deterministic selection if multiple accelerated providers are installed.
- [x] Keep CPU as the standard/default/reference option: `DISABLED` performs no accelerated-provider discovery, normal CI and the complete existing suite pass without native GPU libraries, and `DETECT` falls back safely while `ENABLED` fails closed.
- [ ] Implement and live-verify the optional bounded UDP adapter (GPU roadmap phase 6).

### 8. Repository identity

- [x] Use `com.nologo` Maven coordinates and `com.nologo.math2` Java packages/Spring registrations.
- [x] Use `@nologo` npm scopes, `nologo::math2` C++ namespace, and `nologo-math2` / `nologo_math2` Rust identities.
- [x] Update requirements, documentation, and examples consistently.
- [x] Compile, test, package, and live-regression verify the renamed service and every transport client.

## Current focus

Repository-wide identity refactoring to `nologo` is complete and verified.
GPU roadmap phases 2 and 3 are complete and verified. Phase 4 is in progress:
the optional artifact, guarded JNI/device lifecycle, provider registration, and
non-hardware conformance fixtures are implemented. The pinned simulator
installer, Docker build definition, post-build toolchain smoke check, and
operator guide are also implemented; the multi-gigabyte image and a Math2
kernel have not completed simulation locally. The optional Metal Java
foundation and discovery-only native bridge are now implemented and the
default Apple M3 Pro Metal device was opened and released in an explicit
hardware test. Production Metal capabilities remain empty because device
buffers and exact calculation kernels do not exist. Next add bounded native
memory pooling and a minimal exact-limb accelerator probe, then exact CUDA and
Metal kernels one operation family at a time. No
operation may be advertised until unchanged CPU/backend hardware conformance
passes. CPU remains the default/reference path and works without Docker, CUDA,
or Metal.

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
- 2026-07-26: Reworked the draft `GPU.txt` into the 757-line `GPU.md` target architecture. Added `REG-009` for deterministic execution planning, optional CPU/CUDA backends, exact canonical result parity, precision/rounding preservation, ordered batches, discovery/fallback semantics, resource bounds, conformance tests, and hardware release evidence. Added `REG-010` for an opt-in loopback UDP calculation protocol with strict datagram bounds, request correlation, replay safety, amplification controls, shared HTTP semantics, and real-datagram tests. Updated `NFR-001`, requirements navigation, root README, and the static documentation with an explicit unimplemented-status boundary.
- 2026-07-26: Documentation validation passes: all Markdown fences are balanced across `GPU.md` and 33 requirement Markdown files, and all local `href` targets resolve across 21 documentation HTML pages. The first `mvn verify` attempt encountered a transient Surefire `ClassNotFoundException` for the compiled `EquationBatchParserTest`; an immediate focused reactor test passed 93/93 tests, and the final fresh `mvn verify -Ddebug=false` passed the complete three-module reactor with 93 tests, 0 failures, 0 errors, and 0 skips.
- 2026-07-26: Completed GPU roadmap phase 1 in `java-api` without changing active equation execution. Added immutable, validated `CalculationContext` and `GpuMode`; deterministic versioned `ExecutionPlan` with ordered expressions and length-prefixed SHA-256 identity; immutable `BackendCapabilities`, `PlanSupport`, and `ExecutionResult`; and lazy `ComputeBackend`/`ComputeBackendProvider` SPI boundaries. Added one class specification and focused tests for each of the eight production types. `mvn -pl java-api test -Ddebug=false` passes 91 tests. Final `mvn verify -Ddebug=false` passes the complete three-module reactor with 109 tests (91 Java API and 18 REST), 0 failures, 0 errors, and 0 skips. Markdown fence audit passes across 43 files and all local links resolve across 21 documentation HTML pages.
- 2026-07-26: Completed the repository identity migration to `nologo`: Maven group `com.nologo`, Java root `com.nologo.math2`, npm scope `@nologo`, C++ namespace `nologo::math2`, and Rust crate/import `nologo-math2` / `nologo_math2`. Updated source paths, imports, Spring factory registration, package metadata/lockfile, requirements, examples, and documentation. `mvn clean verify -Ddebug=false` passes 109 Java tests with 0 failures/errors/skips. The first sandboxed `./test-all.sh` run passed Java but could not bind loopback (`SocketException: Operation not permitted`); the approved loopback rerun passed Java 109/109, JavaScript 5/5, TypeScript 3/3, C++ 1/1, .NET 3/3, and Rust 2/2 with no failures or skips, and reported `Math2 live REST client integration tests passed`.
- 2026-07-26: Completed GPU roadmap phases 2 and 3. All `Equation.calculate` overloads now create deterministic plans and execute through `ExecutionPlanner`/`ReferenceCpuBackend`; `CalculationContext` is a public calculation overload. `DISABLED` never enumerates providers, `DETECT` applies deterministic batch/work suitability and pre-execution fallback, and `ENABLED` fails closed. The REST service submits ordered cache misses as one plan and restores mixed hit/miss results in request order. Focused `mvn -pl java-rest-service -am test -Ddebug=false` passed 122 tests before the final REST assertions were added.
- 2026-07-26: Started CUDA roadmap phase 4 with the optional `math2-java-cuda-backend` artifact. It registers a lazy `CudaBackendProvider`, guards `math2_cuda` JNI loading until backend creation, owns idempotent device lifecycle, and advertises no executable plan until exact kernels conform. Ten deterministic tests require no CUDA hardware: default `DISABLED` CPU execution with the provider JAR present; service discovery; device/runtime validation and cleanup; missing-native `DETECT` fallback; `ENABLED` fail-closed behavior; and exact accelerated-double parity for every current operation across precision 20/50, all seven rounding modes, failures, and ordered batches.
- 2026-07-26: Final `mvn clean verify -Ddebug=false` passes the four-module reactor with 136 Java tests (106 API, 10 CUDA foundation, 20 REST), 0 failures, 0 errors, and 0 skips. The approved final `./test-all.sh` loopback run passes Java 136/136, JavaScript 5/5, TypeScript 3/3, C++ 1/1, .NET 3/3, and Rust 2/2 and reports `Math2 live REST client integration tests passed`. A full text/filename audit finds only the required `nologo` identity; obsolete Rust incremental artifacts from the former identity were removed with `cargo clean`, and the rebuilt Rust suite passes 2/2. Markdown fences are balanced and all local links resolve across 21 documentation HTML files.
- 2026-07-26: Investigated no-GPU CUDA simulation rather than assuming it was unavailable. Docker Server 29.1.3 is available as `linux/arm64`. A shallow clone of GPGPU-Sim at commit `a4ce3feac901c97a4b4601f679e43cf3589c79de` (2025-02-15) shows that its current `short-tests.sh` requires a CUDA toolkit through `CUDA_INSTALL_PATH`, a simulator `CONFIG`, and precompiled applications through `GPUAPPS_ROOT`; cloning the repository in an Ubuntu image alone does not build or execute CUDA simulation. Executing `env -u CUDA_INSTALL_PATH -u CONFIG -u GPUAPPS_ROOT bash ./short-tests.sh` exits 1 with `ERROR ** Install CUDA Toolkit and set CUDA_INSTALL_PATH.` Current upstream CI pins `tgrogers/accel-sim_regress:Ubuntu-22.04-cuda-11.7`.
- 2026-07-26: Queried simulator image manifests before pulling. The current GPGPU-Sim CI image is `linux/amd64` with about 5.5 GB of compressed layers; the maintained Accel-Sim CUDA 12.8 image is also `linux/amd64` and about 12.3 GB compressed. Tried the smaller official legacy `aamodt/gpgpu-sim_regress:latest` image (also `linux/amd64`, about 1.3 GB compressed) under Docker amd64 emulation. Two pulls reproducibly stalled without an error or completed image on its 1.218 GB toolchain layer and were terminated with exit 130 after bounded waits. No simulator workload or Math2 CUDA kernel executed, so this attempt is environment/tooling evidence only and is not recorded as CUDA simulation or hardware verification.
- 2026-07-26: Added `install-cuda-simulator.sh`, `cuda-simulator/Dockerfile`, and a post-build smoke script. Defaults pin the upstream CUDA 11.7 CI image by digest, GPGPU-Sim source commit `a4ce3feac901c97a4b4601f679e43cf3589c79de`, and `linux/amd64`; overrides propagate to both build and smoke validation. `bash -n install-cuda-simulator.sh cuda-simulator/simulator-smoke.sh` passes, `./install-cuda-simulator.sh --help` passes from the repository and `/private/tmp`, and an invalid option exits 2. `docker build --check --platform linux/amd64 ...` resolves the pinned manifest and completes with no warnings without pulling its multi-gigabyte layers. The dedicated `documentation/cuda-simulation.html` page and deployment/GPU/README links document installation, resource cost, ARM emulation, overrides, cleanup, and simulation-versus-hardware evidence. The complete image build/smoke and a Math2 kernel simulation remain unexecuted and are not claimed as passing.
- 2026-07-26: Final installer/documentation verification passes. `mvn verify -Ddebug=false` reports `BUILD SUCCESS` for the four-module `com.nologo` reactor with 136 Java tests (106 API, 10 optional CUDA foundation, 20 REST), 0 failures, 0 errors, and 0 skips. The local-link audit resolves all 287 relative links across 22 documentation HTML files and the fence audit finds 0 unbalanced fences across `GPU.md`, `CONTINUE.md`, and 49 requirement files. The identity audit used here was case-sensitive and incomplete; it is superseded by the later case-insensitive audit.
- 2026-07-26: Added the planned macOS Metal alternative to this tracker. Docker/CUDA simulation remains an optional development lane rather than a runtime dependency. No Metal requirement, artifact, native bridge, kernel, or hardware test exists yet, so this entry records planned work only and makes no implementation or verification claim.
- 2026-07-26: Corrected the overlooked digit-suffixed .NET namespace to `Nologo.Math2` in all four client production sources, the test import, README example, and .NET documentation page. A clean rebuild with Homebrew .NET 8.0.129 and `dotnet test dotnet-api/Math2.Tests/Math2.Tests.csproj --no-restore` passes 3/3 tests with 0 failures and 0 skips. The first sandboxed test attempt was blocked by its local control socket; a rerun against the system launcher found no .NET 8 runtime, so neither is counted as a test pass. The final case-insensitive content and filename audit includes generated outputs and finds no old digit-suffixed identity.
- 2026-07-26: Added the normative vendor-neutral CUDA/Metal policy, five Metal class specifications, the independently optional `math2-java-metal-backend` module, guarded provider/runtime/device lifecycle, empty production capabilities, and deterministic no-hardware fixtures. The focused command `mvn -pl java-metal-backend -am test -Ddebug=false` passes 118 tests (106 API and 12 Metal), 0 failures/errors/skips. A subsequent clean five-module `mvn clean verify -Ddebug=false` passes 149 tests (107 API, 10 CUDA, 12 Metal, 20 REST), 0 failures, 0 errors, and 0 skips.
- 2026-07-26: Added strict `build-metal-native.sh` and the Objective-C `math2_metal` JNI discovery bridge. The script passes `bash -n`, runs successfully from `/private/tmp`, and builds an arm64 Mach-O dynamic library with Java 21.0.9 headers, Xcode 16.1, and the macOS 15.1 SDK. The first sandboxed hardware run compiled but returned `Metal bridge returned no device`; the identical approved run outside the restricted sandbox passed `MetalHardwareIT` 1/1 with 0 failures/errors/skips on macOS 26.5.2 and the built-in Apple M3 Pro 14-core GPU reported as Metal-supported. This is device discovery/lifecycle evidence only: no buffer, shader, Math2 operation, performance, or CPU/Metal calculation conformance was tested.
- 2026-07-26: Final post-bridge verification rebuilt from clean state. `mvn clean verify -Ddebug=false` passes the five-module reactor with 149 tests (107 API, 10 CUDA, 12 Metal, 20 REST), 0 failures, 0 errors, and 0 skips; the explicitly named `MetalHardwareIT` remains outside normal Surefire patterns rather than becoming a hidden skip. After rebuilding `libmath2_metal.dylib`, the approved explicit hardware command again passes 1/1. Static audits resolve 306 relative links across 23 documentation HTML files, find 0 unbalanced fences across 56 tracked Markdown files, match all 5 Metal production classes to class specifications, confirm the service-provider registration in the Metal JAR, and find 0 obsolete digit-suffixed identity matches case-insensitively, including generated output.
- 2026-07-26: A final `./test-all.sh` live cross-language rerun was requested but did not start: the approval system rejected process creation because the workspace was out of approval credits. This is an environment/authorization blocker, not a test result. The most recent live HTTP/client evidence remains the earlier passing 136-test CPU/CUDA-foundation run; the new 149-test clean reactor and explicit Metal device test are current, but there is no fresh post-Metal live transport run yet.
- 2026-09-15: Added the zero-runtime-dependency Python 3.10+ REST client as distribution `nologo-math2` and import package `nologo_math2`, with immutable canonical `Number`, composable `Equation`, synchronous single/batch transport, four distinct failure categories, eight production-class specifications, three runnable examples, package/site documentation, wheel/build/clean wiring, and a live integration-test leg. The required precondition `mvn verify -q` completed with exit code 0 before implementation. `py -3 -m unittest discover -s python-api\tests -v` passes 13 local tests with 1 expected live-service skip; after timeout coverage was added, the final run passes 13 local tests with 1 expected live-service skip out of 14. With the packaged service healthy on `127.0.0.1:18080`, the same command with `MATH2_TEST_BASE_URL` passes all 14 tests with 0 failures/errors/skips. `py -3 -m pip wheel --no-deps --wheel-dir python-api\dist .\python-api` builds `nologo_math2-0.1.0-py3-none-any.whl`, and installation into a fresh virtual environment followed by a public-API import/canonicalization/composition smoke check passes. `bash -n build-all.sh clean-all.sh test-client-integration.sh` passes. The WSL-backed `bash ./build-all.sh python-api` path reports a deliberate skip because that separate `/usr/bin/python3` lacks pip; native Windows Python 3.14.7 performed the successful package build and tests.
- 2026-09-15: Corrected the documentation landing-page Chapter 05 card to list Python among the client libraries. The rendered `Math2 — decimal mathematics without the ceiling` page exposes “Java, JavaScript, TypeScript, Python, .NET, C++, and Rust”; focused editor diagnostics and `git diff --check -- documentation\index.html` pass.

## Blockers

CUDA hardware conformance is not yet runnable: the repository has no
`math2_cuda` native bridge or exact CUDA kernels, and this run produced no
declared CUDA device/driver/runtime evidence. This is intentionally not counted
as CUDA verification; production CUDA operation capabilities remain empty.
Functional simulation is now an explicit intermediate task, not a substitute
for this hardware gate. Its reproducible installer is ready, but no compiled
Math2 kernel probe exists yet.
The local simulator attempt is additionally blocked by the ARM Docker host and
large amd64-only upstream images: the smallest official image did not complete
pulling in two bounded attempts. A practical next attempt should use an amd64
runner with the current upstream CUDA 11.7 CI image pre-cached, then compile a
minimal exact-limb Math2 kernel probe before integrating the JNI bridge.
The macOS host can now build the discovery bridge and verify device lifecycle,
but usable Metal calculation remains blocked by the absence of bounded device
buffers, exact integer-limb shaders, result validation, and unchanged
CPU/Metal hardware conformance. Production Metal operation capabilities remain
empty.
The final post-Metal live cross-language transport rerun is also pending because
the approval system rejected `./test-all.sh` before process creation when the
workspace ran out of approval credits.
