using System.Globalization;
using System.Text.RegularExpressions;

namespace Nologo2.Math2;

public sealed partial class Number : IEquatable<Number>, IComparable<Number>
{
    private readonly string value;
    private Number(string value) => this.value = Canonicalize(value);

    public static Number FromString(string value) => new(value);
    public static Number FromInteger(int value) => new(value.ToString(CultureInfo.InvariantCulture));
    public static Number FromLong(long value) => new(value.ToString(CultureInfo.InvariantCulture));
    public static Number FromFloat(float value) => float.IsFinite(value)
        ? new(value.ToString("R", CultureInfo.InvariantCulture))
        : throw new Math2ValidationException("Number must be finite");
    public static Number FromDouble(double value) => double.IsFinite(value)
        ? new(value.ToString("R", CultureInfo.InvariantCulture))
        : throw new Math2ValidationException("Number must be finite");
    public static Number Zero() => new("0");

    public int CompareTo(Number? other) => other is null ? 1 : CompareDecimal(value, other.value);
    public bool Equals(Number? other) => other is not null && value == other.value;
    public override bool Equals(object? obj) => Equals(obj as Number);
    public override int GetHashCode() => value.GetHashCode(StringComparison.Ordinal);
    public override string ToString() => value;

    private static string Canonicalize(string input)
    {
        if (input is null) throw new ArgumentNullException(nameof(input));
        var match = DecimalPattern().Match(input.Trim());
        if (!match.Success || (match.Groups[2].Value.Length == 0 && match.Groups[3].Value.Length == 0))
            throw new Math2ValidationException($"Invalid decimal number: {input}");
        var negative = match.Groups[1].Value == "-";
        var integer = match.Groups[2].Value;
        var fraction = match.Groups[3].Value;
        var exponent = match.Groups[4].Success ? int.Parse(match.Groups[4].Value, CultureInfo.InvariantCulture) : 0;
        var combined = (integer + fraction).TrimStart('0');
        if (combined.Length == 0) return "0";
        var decimalIndex = integer.Length + exponent - (integer.Length + fraction.Length) + combined.Length;
        string result = decimalIndex <= 0
            ? "0." + new string('0', -decimalIndex) + combined
            : decimalIndex >= combined.Length
                ? combined + new string('0', decimalIndex - combined.Length)
                : combined[..decimalIndex] + "." + combined[decimalIndex..];
        result = result.Contains('.') ? result.TrimEnd('0').TrimEnd('.') : result;
        return negative ? "-" + result : result;
    }

    private static int CompareDecimal(string left, string right)
    {
        var leftNegative = left.StartsWith('-');
        var rightNegative = right.StartsWith('-');
        if (leftNegative != rightNegative) return leftNegative ? -1 : 1;
        var magnitude = CompareMagnitude(left.TrimStart('-'), right.TrimStart('-'));
        return leftNegative ? -magnitude : magnitude;
    }

    private static int CompareMagnitude(string left, string right)
    {
        var leftParts = left.Split('.', 2);
        var rightParts = right.Split('.', 2);
        if (leftParts[0].Length != rightParts[0].Length)
            return leftParts[0].Length.CompareTo(rightParts[0].Length);
        var integerComparison = string.CompareOrdinal(leftParts[0], rightParts[0]);
        if (integerComparison != 0) return integerComparison;
        var leftFraction = leftParts.Length == 2 ? leftParts[1] : "";
        var rightFraction = rightParts.Length == 2 ? rightParts[1] : "";
        var length = Math.Max(leftFraction.Length, rightFraction.Length);
        return string.CompareOrdinal(leftFraction.PadRight(length, '0'), rightFraction.PadRight(length, '0'));
    }

    [GeneratedRegex(@"^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$")]
    private static partial Regex DecimalPattern();
}
