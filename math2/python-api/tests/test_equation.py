import unittest

from nologo_math2 import Equation, Number, ValidationError


class EquationTest(unittest.TestCase):
    def test_composes_without_mutating_the_original(self) -> None:
        two = Equation.of(Number.from_integer(2))
        expression = two.add(Equation.of(Number.from_integer(3))).sqrt()
        self.assertEqual(str(expression), "sqrt((2+3))")
        self.assertEqual(str(two), "2")

    def test_supports_all_client_operations(self) -> None:
        left = Equation.parse("2")
        right = Equation.parse("3")
        self.assertEqual(str(left.subtract(right)), "(2-3)")
        self.assertEqual(str(left.multiply(right)), "(2*3)")
        self.assertEqual(str(left.divide(right)), "(2/3)")
        self.assertEqual(str(left.power(right)), "(2^3)")
        self.assertEqual(str(left.min(right)), "min(2,3)")
        self.assertEqual(str(left.max(right)), "max(2,3)")
        self.assertEqual(str(left.abs().log().sin().cos().tan()), "tan(cos(sin(log(abs(2)))))")

    def test_rejects_invalid_expressions_and_operands(self) -> None:
        with self.assertRaises(ValidationError):
            Equation.parse(" ")
        with self.assertRaises(ValidationError):
            Equation.of("2")  # type: ignore[arg-type]
        with self.assertRaises(ValidationError):
            Equation.parse("2").add("3")  # type: ignore[arg-type]


if __name__ == "__main__":
    unittest.main()
