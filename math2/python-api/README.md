# Math2 Python client

`nologo-math2` is the synchronous, zero-runtime-dependency Python client for
the Math2 arbitrary-precision REST API. It keeps every decimal as canonical
text, builds immutable equations, and never routes service results through a
binary `float`.

## Requirements

- Python 3.10 or newer with `pip` for installation
- A running Math2 REST service

Start the service from the repository root with `./run-dev-local.sh`.

## Install

Install the local project in a virtual environment:

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install ./python-api
```

On Windows PowerShell, activate with `.venv\Scripts\Activate.ps1`.

## Calculate

```python
from nologo_math2 import Equation, Math2Client, Number

client = Math2Client()
two = Equation.of(Number.from_integer(2))
three = Equation.of(Number.from_integer(3))
equation = three.sqrt().add(two.power(three))

result = client.calculate(equation, precision=100)
print(result)
```

`Number.from_string()` accepts ordinary or scientific decimal notation and
normalizes it:

```python
assert str(Number.from_string("001.2300e2")) == "123"
assert Number.from_string("-0.0") == Number.zero()
```

Use `Number.from_integer()` for arbitrary-size Python integers.
`Number.from_float()` is explicit because the source float may already have
lost decimal precision.

## Compose equations

`Equation` instances never change. Every operation returns a new expression:

```python
base = Equation.of(Number.from_integer(2))
equation = base.add(Equation.of(Number.from_integer(3))).sqrt()

assert str(base) == "2"
assert str(equation) == "sqrt((2+3))"
```

Supported methods are `add`, `subtract`, `multiply`, `divide`, `power`, `min`,
`max`, `sqrt`, `abs`, `log`, `sin`, `cos`, and `tan`. Use
`Equation.parse("...")` for complete Math2 expressions, including constants and
new server-side operations.

## Ordered batches

```python
equations = (
    Equation.parse("1+1"),
    Equation.parse("max(2,3)"),
    Equation.parse("sqrt(16)"),
)
results = client.calculate_batch(equations, precision=1000)

assert tuple(map(str, results)) == ("2", "3", "4")
```

The result is an immutable tuple in the same order and with the same
cardinality as the request.

## Configuration

The defaults are `http://127.0.0.1:8080` and ten seconds:

```python
client = Math2Client(
    base_url="https://math2.example.test",
    timeout_seconds=30,
)
```

`precision` is optional. When supplied, it must be a positive integer and is
sent unchanged to the service.

## Errors

- `ValidationError`: invalid local value, expression, operand, batch, precision,
  URL, or timeout.
- `TransportError`: DNS, connection, TLS, or timeout failure.
- `ProtocolError`: invalid JSON or a response with the wrong result shape.
- `ServiceError`: a non-success Math2 response. Its `status`, `code`, and
  optional `position` attributes retain the structured service error.
- `Math2Error`: common base class for all four categories.

```python
try:
    result = client.calculate(Equation.parse("sqrt(-1)"))
except ServiceError as error:
    print(error.status, error.code, error.position, str(error))
```

## Run examples and tests

With the repository package on `PYTHONPATH`:

```bash
PYTHONPATH=python-api/src python python-api/examples/single_calculation.py
PYTHONPATH=python-api/src python -m unittest discover -s python-api/tests -v
```

The single, batch, and error-handling programs in `examples/` are directly
runnable while the service is available. The repository's
`./test-client-integration.sh` also runs the Python tests against an isolated
packaged Math2 service.
