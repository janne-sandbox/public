# NFR-002 — Quality and packaging

1. The Java modules build as a Maven reactor and the calculation library is usable as a standalone JAR.
2. Production Java code targets Java 21 and follows package root `com.nologo.math2`.
   Maven project coordinates use group ID `com.nologo`. Repository package,
   namespace, crate, resource registration, and documentation identities use
   `nologo` consistently.
3. Unit tests cover every public class, operator, error condition, conversion boundary, and parser precedence rule.
4. REST integration tests use the actual H2/Liquibase configuration.
5. The service is distributable as an executable Spring Boot JAR and Docker image.
6. Shell scripts use strict error handling and work when invoked outside the repository directory.
7. Runtime database, PID, log, build-output, IDE, and operating-system files are ignored by version control.
8. Public APIs and configuration properties are documented.
9. The optional CUDA simulator installer is a strict root-level shell script
   that can be invoked outside the repository directory. It builds from a
   digest-pinned upstream CUDA toolchain image and a commit-pinned GPGPU-Sim
   source revision, selects `linux/amd64` explicitly for reproducibility,
   performs a post-build smoke check, and documents download/storage cost,
   ARM-host emulation, overrides, cleanup, and the distinction between
   simulation and physical-hardware verification.
10. CUDA and Metal backend JARs are independently optional. Installing either
    JAR must not load a native library during service-provider discovery, and
    ordinary CPU builds and runtime operation require neither artifact,
    Docker, nor a vendor SDK.
11. The macOS Metal bridge build is an explicit strict root-level script that
    works outside the repository directory, uses the selected Java 21 headers
    and active macOS SDK, writes only under the Metal module build directory,
    and does not run as part of ordinary platform-neutral Maven verification.
