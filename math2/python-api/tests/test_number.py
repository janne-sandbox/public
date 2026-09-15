import unittest

from nologo_math2 import Number, ValidationError


class NumberTest(unittest.TestCase):
    def test_canonicalizes_decimal_and_scientific_notation(self) -> None:
        cases = {
            "001.2300e2": "123",
            "-0.0": "0",
            ".0012300": "0.00123",
            "1e3": "1000",
            "1e-3": "0.001",
            "-12.3400": "-12.34",
        }
        for source, expected in cases.items():
            with self.subTest(source=source):
                self.assertEqual(str(Number.from_string(source)), expected)

    def test_supports_arbitrarily_large_integers(self) -> None:
        value = 10**1000
        self.assertEqual(str(Number.from_integer(value)), "1" + ("0" * 1000))

    def test_is_immutable(self) -> None:
        number = Number.from_string("1")
        with self.assertRaises((AttributeError, TypeError)):
            number._value = "2"  # type: ignore[misc]

    def test_rejects_invalid_values(self) -> None:
        for value in ("", ".", "1.2.3", "nan", "infinity"):
            with self.subTest(value=value):
                with self.assertRaises(ValidationError):
                    Number.from_string(value)
        with self.assertRaises(ValidationError):
            Number.from_float(float("inf"))
        with self.assertRaises(ValidationError):
            Number.from_integer(True)
        with self.assertRaises(ValidationError):
            Number(1)  # type: ignore[arg-type]


if __name__ == "__main__":
    unittest.main()
