# Math2 .NET client

.NET 8 client with immutable `Number` and `Equation` classes and async single/batch REST methods.

```csharp
using Nologo2.Math2;

using var client = new Math2Client();
var equation = Equation.Of(Number.FromInteger(9)).Sqrt();
var result = await client.CalculateAsync(equation);
```

Build with `dotnet build Math2/Math2.csproj`. Transport, protocol, validation, and service errors use distinct exception types.
