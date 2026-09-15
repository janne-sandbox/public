# Class specification — Python Math2Client

`nologo_math2.Math2Client` is the synchronous standard-library HTTP client for
REG-006. It defaults to loopback port 8080 and a ten-second timeout, returns
immutable `Number` values, preserves batch ordering and cardinality, and
distinguishes validation, transport, protocol, and structured service errors.
