namespace Nologo2.Math2;

public sealed class Equation
{
    private readonly string expression;
    private Equation(string expression) => this.expression = string.IsNullOrWhiteSpace(expression)
        ? throw new Math2ValidationException("Expression must not be blank")
        : expression.Trim();

    public static Equation Parse(string expression) => new(expression);
    public static Equation Of(Number number) => new(number?.ToString() ?? throw new ArgumentNullException(nameof(number)));
    public Equation Add(Equation other) => Binary("+", other);
    public Equation Subtract(Equation other) => Binary("-", other);
    public Equation Multiply(Equation other) => Binary("*", other);
    public Equation Divide(Equation other) => Binary("/", other);
    public Equation Power(Equation other) => Binary("^", other);
    public Equation Min(Equation other) => Function("min", other);
    public Equation Max(Equation other) => Function("max", other);
    public Equation Sqrt() => Unary("sqrt");
    public Equation Abs() => Unary("abs");
    public Equation Log() => Unary("log");
    public Equation Sin() => Unary("sin");
    public Equation Cos() => Unary("cos");
    public Equation Tan() => Unary("tan");
    public override string ToString() => expression;

    private Equation Binary(string operation, Equation other) => new($"({this}{operation}{Require(other)})");
    private Equation Function(string name, Equation other) => new($"{name}({this},{Require(other)})");
    private Equation Unary(string name) => new($"{name}({this})");
    private static Equation Require(Equation? value) => value ?? throw new ArgumentNullException(nameof(value));
}
