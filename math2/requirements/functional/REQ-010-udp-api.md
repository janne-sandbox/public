# REG-010 — Optional UDP calculation transport

## Status

Target requirement. This capability is not implemented in the current REST service.

## Transport and configuration

1. HTTP remains the required, default, documented API. UDP is an optional transport to the same validated calculation application service; it does not contain a separate evaluator.
2. UDP is disabled by default and is enabled with `math2.udp.enabled` / `MATH2_UDP_ENABLED`.
3. The UDP bind address and port are independently configurable and default to `127.0.0.1` and `8081`.
4. Enabling UDP on a non-loopback address requires an explicit insecure-transport acknowledgement or a separately specified authenticated secure envelope. Startup fails otherwise.
5. The absence of a transport handshake does not imply reliability, identity, confidentiality, integrity, ordering, congestion control, or delivery.

## Datagram protocol

6. Each datagram is one UTF-8 JSON object with protocol version, client-generated request ID, operation (`calculate` or `calculateBatch`), precision, and either one expression or an ordered expression array.
7. Request IDs are 128-bit random values encoded as 32 lowercase hexadecimal characters. Responses repeat the request ID.
8. Success and error values reuse the HTTP API's canonical decimal strings and stable error codes. Batch responses retain input order and identify an error by zero-based item index.
9. The protocol does not split one application message across IP fragments or multiple application datagrams. Oversized requests are rejected when a response can be sent safely; clients must use HTTP.
10. The configurable maximum datagram payload defaults to 1200 bytes and may not exceed 65,507 bytes. The service validates the received byte count before JSON parsing.
11. UDP requests use the same expression, batch, precision, timeout, and calculation limits as HTTP. Transport limits may be stricter.
12. Unknown versions, malformed UTF-8/JSON, duplicate fields, unknown fields, invalid request IDs, and invalid operations are rejected without calculation.

## Reliability and safety

13. Clients own timeout and bounded retry policy. The server does not promise delivery or ordering.
14. A bounded, expiring replay cache keyed by remote endpoint plus request ID returns the identical encoded response for a duplicate request and prevents duplicate calculation while an original request is in flight.
15. Reusing a request ID with different request bytes is rejected.
16. Responses never exceed the request byte count by more than a configurable amplification allowance before a client address has been validated by an authenticated envelope. If a full response would exceed the allowance, the server returns no datagram or a bounded `USE_HTTP` error.
17. Receive queue, concurrent calculations, per-source rate, replay entries, response size, and execution time are bounded. Saturation drops or rejects work before evaluation.
18. UDP calculation is read-only. Configuration changes, health administration, cache administration, and other mutating operations are forbidden.
19. Logs and metrics do not include equations or results by default. They include protocol version, outcome, sizes, timing, truncation/drop reason, and a one-way request-ID digest.
20. Shutdown stops intake, bounds drain time, cancels unfinished work, and closes the socket without leaving calculation threads running.

## Acceptance

21. Unit tests cover codec validation, byte limits, request correlation, replay behavior, request-ID conflicts, amplification limits, ordering, and HTTP-equivalent error mapping.
22. Loopback integration tests use real UDP datagrams for single and batch calculations, timeout/retry behavior, malformed input, duplicate input, oversize rejection, and disabled mode.
23. Tests prove that UDP and HTTP return identical result/error payload semantics for equivalent supported requests.
24. Documentation must state that plain UDP is intended only for trusted local networks, recommend HTTP behind authenticated TLS for remote use, and explain loss, duplication, reordering, MTU, and retry behavior.
