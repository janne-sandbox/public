from nologo_math2 import Equation, Math2Client, Number

client = Math2Client()
two = Equation.of(Number.from_integer(2))
three = Equation.of(Number.from_integer(3))
equation = three.sqrt().add(two.power(three))

result = client.calculate(equation, precision=100)
print(f"{equation} = {result}")
