# REQ-009 — Multi-Architecture Container Images

## Status
Active

## Category
Container / Build

## Priority
High

## Statement
All Docker container images produced by the Clinical Cache platform **MUST** be
built and published for both `linux/amd64` and `linux/arm64` architectures.

## Rationale
- Production AKS nodes may be provisioned on ARM-based hardware (e.g. Azure Cobalt,
  Ampere Altra) as well as x86-64 nodes.
- Local development on Apple Silicon (M-series) Macs requires arm64 images for
  efficient performance (no QEMU emulation overhead).
- Providing multi-arch manifests ensures a single image reference works on any
  supported platform without per-platform tags.

## Scope
All service images:
- `cache-backend`
- `vna-backend`
- `fhir-backend`
- `registry-backend`
- `hl7-receiver-backend`
- `atna-fhir-backend`
- `atna-fhir-frontend` (nginx-based)

## Implementation

### Dockerfile pattern
Each Dockerfile **MUST** declare `BUILDPLATFORM` and `TARGETPLATFORM` ARGs and
pin the build stage to `$BUILDPLATFORM` to enable native cross-compilation:

```dockerfile
ARG BUILDPLATFORM=linux/amd64
ARG TARGETPLATFORM

# Build stage runs on the host platform (faster cross-compilation)
FROM --platform=${BUILDPLATFORM} eclipse-temurin:17-jdk-alpine AS build
...

# Runtime stage resolves to the target platform
FROM eclipse-temurin:17-jre-alpine
...
```

### CI/CD (GitHub Actions)
The workflow `.github/workflows/docker-multiarch.yml` **MUST**:
1. Use `docker/setup-qemu-action` to enable cross-architecture emulation.
2. Use `docker/setup-buildx-action` to enable BuildKit multi-platform builds.
3. Pass `platforms: linux/amd64,linux/arm64` to `docker/build-push-action`.
4. Push multi-arch manifest lists to the container registry.

## Verification
- `docker buildx imagetools inspect <image>:<tag>` must show manifests for both
  `linux/amd64` and `linux/arm64`.
- CI pipeline must pass for both platforms on every merge to `main`.

## Related Requirements
- REQ-001 — No critical CVEs (applies to both architectures)
- REQ-003 — Horizontal/vertical scalability (ARM nodes extend scaling options)
