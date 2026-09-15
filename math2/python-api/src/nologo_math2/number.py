"""Immutable canonical decimal values."""

from __future__ import annotations

from dataclasses import dataclass
import math
import re

from .errors import ValidationError

_DECIMAL_PATTERN = re.compile(
    r"^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$"
)


@dataclass(frozen=True, slots=True, init=False)
class Number:
    """An immutable decimal value stored without binary precision loss."""

    _value: str

    def __init__(self, value: str) -> None:
        if not isinstance(value, str):
            raise ValidationError("Decimal value must be a string")
        object.__setattr__(self, "_value", _canonical_decimal(value))

    @classmethod
    def from_string(cls, value: str) -> Number:
        if not isinstance(value, str):
            raise ValidationError("Decimal value must be a string")
        return cls(value)

    @classmethod
    def from_integer(cls, value: int) -> Number:
        if isinstance(value, bool) or not isinstance(value, int):
            raise ValidationError("Integer value must be an int")
        return cls(str(value))

    @classmethod
    def from_float(cls, value: float) -> Number:
        if isinstance(value, bool) or not isinstance(value, float) or not math.isfinite(value):
            raise ValidationError("Float value must be finite")
        return cls(str(value))

    @classmethod
    def zero(cls) -> Number:
        return cls("0")

    def __str__(self) -> str:
        return self._value


def _canonical_decimal(value: str) -> str:
    text = value.strip()
    match = _DECIMAL_PATTERN.fullmatch(text)
    if match is None or (not match.group(2) and not match.group(3)):
        raise ValidationError(f"Invalid decimal number: {value}")

    sign = "-" if match.group(1) == "-" else ""
    integer = match.group(2) or ""
    fraction = match.group(3) or ""
    exponent = int(match.group(4) or "0")
    combined = integer + fraction
    digits = combined.lstrip("0")
    if not digits:
        return "0"

    decimal_index = len(integer) + exponent - (len(combined) - len(digits))
    if decimal_index <= 0:
        result = "0." + ("0" * -decimal_index) + digits
    elif decimal_index >= len(digits):
        result = digits + ("0" * (decimal_index - len(digits)))
    else:
        result = digits[:decimal_index] + "." + digits[decimal_index:]

    if "." in result:
        result = result.rstrip("0").rstrip(".")
    return sign + result
