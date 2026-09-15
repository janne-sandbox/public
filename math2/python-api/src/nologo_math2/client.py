"""Synchronous standard-library transport for the Math2 REST API."""

from __future__ import annotations

import json
import math
import socket
from typing import Any, Sequence
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen

from .equation import Equation
from .errors import ProtocolError, ServiceError, TransportError, ValidationError
from .number import Number

_DEFAULT_BASE_URL = "http://127.0.0.1:8080"
_DEFAULT_TIMEOUT_SECONDS = 10.0


class Math2Client:
    """Blocking client for single and ordered-batch Math2 calculations."""

    def __init__(
        self,
        base_url: str = _DEFAULT_BASE_URL,
        timeout_seconds: float = _DEFAULT_TIMEOUT_SECONDS,
    ) -> None:
        if not isinstance(base_url, str) or not base_url.strip():
            raise ValidationError("base_url must not be blank")
        parsed_url = urlsplit(base_url)
        if parsed_url.scheme not in ("http", "https") or not parsed_url.netloc:
            raise ValidationError("base_url must be an absolute HTTP or HTTPS URL")
        if isinstance(timeout_seconds, bool) or not isinstance(timeout_seconds, (int, float)):
            raise ValidationError("timeout_seconds must be positive")
        if not math.isfinite(timeout_seconds) or timeout_seconds <= 0:
            raise ValidationError("timeout_seconds must be positive")

        self._base_url = base_url.rstrip("/")
        self._timeout_seconds = float(timeout_seconds)

    @property
    def base_url(self) -> str:
        return self._base_url

    @property
    def timeout_seconds(self) -> float:
        return self._timeout_seconds

    def calculate(self, equation: Equation, precision: int | None = None) -> Number:
        _require_equation(equation)
        _validate_precision(precision)
        data = self._get(
            "/api/v1/calculate",
            {"equation": str(equation), "precision": precision},
        )
        result = data.get("result") if isinstance(data, dict) else None
        if not isinstance(result, str):
            raise ProtocolError("Response does not contain a string result")
        return Number.from_string(result)

    def calculate_batch(
        self,
        equations: Sequence[Equation],
        precision: int | None = None,
    ) -> tuple[Number, ...]:
        if isinstance(equations, (str, bytes)) or not isinstance(equations, Sequence):
            raise ValidationError("Equations must be a sequence")
        if not equations:
            raise ValidationError("Equations must not be empty")
        for equation in equations:
            _require_equation(equation)
        _validate_precision(precision)

        data = self._get(
            "/api/v1/calculate/batch",
            {
                "equations": ",".join(str(equation) for equation in equations),
                "precision": precision,
            },
        )
        results = data.get("results") if isinstance(data, dict) else None
        if (
            not isinstance(results, list)
            or len(results) != len(equations)
            or any(not isinstance(result, str) for result in results)
        ):
            raise ProtocolError("Response result count does not match request")
        return tuple(Number.from_string(result) for result in results)

    def _get(self, path: str, parameters: dict[str, object | None]) -> Any:
        query = urlencode(
            {key: value for key, value in parameters.items() if value is not None}
        )
        request = Request(
            f"{self._base_url}{path}?{query}",
            headers={"Accept": "application/json"},
            method="GET",
        )
        try:
            with urlopen(request, timeout=self._timeout_seconds) as response:
                return _decode_json(response.read())
        except HTTPError as error:
            data = _decode_json(error.read())
            body = data if isinstance(data, dict) else {}
            position = body.get("position")
            raise ServiceError(
                error.code,
                body.get("code") if isinstance(body.get("code"), str) else "UNKNOWN",
                body.get("message")
                if isinstance(body.get("message"), str)
                else "Service error",
                position if isinstance(position, int) and not isinstance(position, bool) else None,
            ) from error
        except (URLError, TimeoutError, socket.timeout, OSError) as error:
            reason = getattr(error, "reason", error)
            if isinstance(reason, (TimeoutError, socket.timeout)):
                message = "Request timed out"
            else:
                message = f"Request failed: {reason}"
            raise TransportError(message) from error


def _decode_json(payload: bytes) -> Any:
    try:
        return json.loads(payload.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ProtocolError("Response is not valid JSON") from error


def _require_equation(value: object) -> None:
    if not isinstance(value, Equation):
        raise ValidationError("Expected an Equation")


def _validate_precision(value: int | None) -> None:
    if value is not None and (
        isinstance(value, bool) or not isinstance(value, int) or value < 1
    ):
        raise ValidationError("precision must be a positive integer")
