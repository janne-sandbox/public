"""Immutable Math2 equation composition."""

from __future__ import annotations

from dataclasses import dataclass

from .errors import ValidationError
from .number import Number


@dataclass(frozen=True, slots=True, init=False)
class Equation:
    """An immutable expression accepted by the Math2 REST API."""

    _expression: str

    def __init__(self, expression: str) -> None:
        if not isinstance(expression, str) or not expression.strip():
            raise ValidationError("Expression must not be blank")
        object.__setattr__(self, "_expression", expression.strip())

    @classmethod
    def parse(cls, expression: str) -> Equation:
        return cls(expression)

    @classmethod
    def of(cls, number: Number) -> Equation:
        if not isinstance(number, Number):
            raise ValidationError("Expected a Math2 Number")
        return cls(str(number))

    def add(self, other: Equation) -> Equation:
        return self._binary("+", other)

    def subtract(self, other: Equation) -> Equation:
        return self._binary("-", other)

    def multiply(self, other: Equation) -> Equation:
        return self._binary("*", other)

    def divide(self, other: Equation) -> Equation:
        return self._binary("/", other)

    def power(self, other: Equation) -> Equation:
        return self._binary("^", other)

    def min(self, other: Equation) -> Equation:
        return self._function("min", other)

    def max(self, other: Equation) -> Equation:
        return self._function("max", other)

    def sqrt(self) -> Equation:
        return self._unary("sqrt")

    def abs(self) -> Equation:
        return self._unary("abs")

    def log(self) -> Equation:
        return self._unary("log")

    def sin(self) -> Equation:
        return self._unary("sin")

    def cos(self) -> Equation:
        return self._unary("cos")

    def tan(self) -> Equation:
        return self._unary("tan")

    def __str__(self) -> str:
        return self._expression

    def _binary(self, operator: str, other: Equation) -> Equation:
        _require_equation(other)
        return Equation(f"({self}{operator}{other})")

    def _function(self, name: str, other: Equation) -> Equation:
        _require_equation(other)
        return Equation(f"{name}({self},{other})")

    def _unary(self, name: str) -> Equation:
        return Equation(f"{name}({self})")


def _require_equation(value: object) -> None:
    if not isinstance(value, Equation):
        raise ValidationError("Expected an Equation")
