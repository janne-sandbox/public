"""Exception hierarchy for the Math2 Python client."""


class Math2Error(Exception):
    """Base exception for all Math2 client failures."""


class ValidationError(Math2Error):
    """Raised when a local argument is invalid."""


class TransportError(Math2Error):
    """Raised when the service cannot be reached or a request times out."""


class ProtocolError(Math2Error):
    """Raised when a service response does not follow the Math2 protocol."""


class ServiceError(Math2Error):
    """A structured error response returned by the Math2 service."""

    def __init__(
        self,
        status: int,
        code: str,
        message: str,
        position: int | None = None,
    ) -> None:
        super().__init__(message)
        self.status = status
        self.code = code
        self.position = position
