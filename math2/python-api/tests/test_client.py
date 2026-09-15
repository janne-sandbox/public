from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import threading
import time
import unittest
from urllib.parse import parse_qs, urlsplit

from nologo_math2 import (
    Equation,
    Math2Client,
    ProtocolError,
    ServiceError,
    TransportError,
    ValidationError,
)


class _Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        url = urlsplit(self.path)
        query = parse_qs(url.query)
        if query.get("equation") == ["bad"]:
            self._json(
                400,
                {"code": "INVALID_EXPRESSION", "message": "bad", "position": 0},
            )
        elif query.get("equation") == ["invalid-json"]:
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"not-json")
        elif query.get("equation") == ["slow"]:
            time.sleep(0.1)
            self._json(200, {"result": "1"})
        elif url.path.endswith("/batch"):
            self._json(200, {"results": ["2", "3"]})
        else:
            self._json(200, {"result": "14"})

    def log_message(self, format: str, *args: object) -> None:
        pass

    def _json(self, status: int, value: object) -> None:
        body = json.dumps(value).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionAbortedError, ConnectionResetError):
            pass


class Math2ClientTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), _Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        host, port = cls.server.server_address
        cls.base_url = f"http://{host}:{port}"

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def test_calculates_single_and_batch_responses(self) -> None:
        client = Math2Client(self.base_url)
        self.assertEqual(str(client.calculate(Equation.parse("2+3*4"))), "14")
        values = client.calculate_batch(
            (Equation.parse("1+1"), Equation.parse("max(2,3)"))
        )
        self.assertEqual(tuple(map(str, values)), ("2", "3"))

    def test_exposes_structured_service_errors(self) -> None:
        client = Math2Client(self.base_url)
        with self.assertRaises(ServiceError) as raised:
            client.calculate(Equation.parse("bad"))
        self.assertEqual(raised.exception.status, 400)
        self.assertEqual(raised.exception.code, "INVALID_EXPRESSION")
        self.assertEqual(raised.exception.position, 0)

    def test_rejects_malformed_responses(self) -> None:
        with self.assertRaises(ProtocolError):
            Math2Client(self.base_url).calculate(Equation.parse("invalid-json"))

    def test_rejects_invalid_local_arguments(self) -> None:
        with self.assertRaises(ValidationError):
            Math2Client("relative")
        with self.assertRaises(ValidationError):
            Math2Client(timeout_seconds=0)
        for timeout in (float("nan"), float("inf")):
            with self.subTest(timeout=timeout):
                with self.assertRaises(ValidationError):
                    Math2Client(timeout_seconds=timeout)
        with self.assertRaises(ValidationError):
            Math2Client(self.base_url).calculate(Equation.parse("1"), 0)
        with self.assertRaises(ValidationError):
            Math2Client(self.base_url).calculate_batch(())

    def test_wraps_connection_failures(self) -> None:
        unavailable_port = self.server.server_address[1] + 1
        client = Math2Client(f"http://127.0.0.1:{unavailable_port}", 0.1)
        with self.assertRaises(TransportError):
            client.calculate(Equation.parse("1+1"))

    def test_reports_timeouts(self) -> None:
        client = Math2Client(self.base_url, 0.01)
        with self.assertRaisesRegex(TransportError, "Request timed out"):
            client.calculate(Equation.parse("slow"))


@unittest.skipUnless(os.environ.get("MATH2_TEST_BASE_URL"), "live service not configured")
class LiveMath2ClientTest(unittest.TestCase):
    def test_calculates_against_live_service(self) -> None:
        client = Math2Client(os.environ["MATH2_TEST_BASE_URL"], 30)
        self.assertEqual(
            str(client.calculate(Equation.parse("sqrt(9)+2^3"), 1000)),
            "11",
        )
        values = client.calculate_batch(
            (
                Equation.parse("1+1"),
                Equation.parse("max(2,3)"),
                Equation.parse("sqrt(16)"),
            ),
            1000,
        )
        self.assertEqual(tuple(map(str, values)), ("2", "3", "4"))


if __name__ == "__main__":
    unittest.main()
