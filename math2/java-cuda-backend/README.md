# Math2 optional CUDA backend

This optional Java 21 artifact provides the guarded CUDA service-provider and
native-runtime lifecycle boundary. It depends on `math2-java-api`; the core API
and REST service do not depend on this module.

Current status: foundation only. Provider metadata is safe to inspect without
loading native code. Backend creation attempts to load the `math2_cuda` JNI
bridge, but the backend advertises no executable plans until exact
arbitrary-precision kernels pass CPU/CUDA conformance. `DISABLED` remains the
default and does not discover or initialize this provider.

Ordinary tests use deterministic runtime and accelerated-backend doubles. They
cover guarded discovery, absent-native fallback, required-GPU failure,
lifecycle cleanup, exact reference parity across operations/precision/rounding
contexts, failures, and ordered batches without requiring CUDA hardware.
Hardware conformance is still required before any operation capability can be
advertised or released.

```bash
mvn -pl java-cuda-backend -am test -Ddebug=false
```
