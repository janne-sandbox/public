from nologo_math2 import Equation, Math2Client

client = Math2Client(timeout_seconds=30)
equations = (
    Equation.parse("1/7"),
    Equation.parse("pi"),
    Equation.parse("sqrt(2)"),
)

for equation, result in zip(
    equations,
    client.calculate_batch(equations, precision=1000),
    strict=True,
):
    print(f"{equation} = {result}")
