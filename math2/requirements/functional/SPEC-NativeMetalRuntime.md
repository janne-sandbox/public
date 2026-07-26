# Class specification — NativeMetalRuntime

`NativeMetalRuntime` is the guarded JNI lifecycle adapter. It rejects
non-macOS hosts, loads the `math2_metal` bridge, and opens the default Metal
device only when `MetalBackendProvider.create()` is called. Provider
construction and metadata access must not load a library or probe hardware.
Failed acquisition releases any opened handle, and close is idempotent.
The native implementation uses the system default Metal device, retains it for
the Java runtime lifetime, exposes stable registry ID/name/recommended working
set metadata, and releases the retained device exactly once.
