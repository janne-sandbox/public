from nologo_math2 import (
    Equation,
    Math2Client,
    ProtocolError,
    ServiceError,
    TransportError,
    ValidationError,
)

client = Math2Client()

try:
    print(client.calculate(Equation.parse("sqrt(-1)")))
except ValidationError as error:
    print(f"Invalid local input: {error}")
except ServiceError as error:
    print(
        f"Math2 rejected the request: HTTP {error.status}, "
        f"code={error.code}, position={error.position}, message={error}"
    )
except TransportError as error:
    print(f"Could not reach Math2: {error}")
except ProtocolError as error:
    print(f"Math2 returned an invalid response: {error}")
