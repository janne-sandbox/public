using Nologo2.Math2;
using System;
using System.Linq;
using System.Threading.Tasks;
using Xunit;

public class ValueTests
{
    [Fact]
    public void NumberCanonicalizesWithoutPrimitivePrecision()
    {
        Assert.Equal("123", Number.FromString("001.2300e2").ToString());
        Assert.Equal(Number.Zero(), Number.FromString("-0.0"));
        Assert.True(Number.FromString("1000000000000000000000000000001")
            .CompareTo(Number.FromString("1000000000000000000000000000000")) > 0);
    }

    [Fact]
    public void EquationCompositionIsImmutable()
    {
        var two = Equation.Of(Number.FromInteger(2));
        Assert.Equal("sqrt((2+3))", two.Add(Equation.Of(Number.FromInteger(3))).Sqrt().ToString());
        Assert.Equal("2", two.ToString());
    }

    [Fact]
    public async Task ClientCalculatesAgainstLiveMath2Service()
    {
        var baseUrl = Environment.GetEnvironmentVariable("MATH2_TEST_BASE_URL");
        if (string.IsNullOrWhiteSpace(baseUrl)) return;

        using var client = new Math2Client(baseUrl, TimeSpan.FromSeconds(30));
        var single = await client.CalculateAsync(Equation.Parse("sqrt(9)+2^3"), 1000);
        Assert.Equal("11", single.ToString());

        var batch = await client.CalculateBatchAsync(new[] {
            Equation.Parse("1+1"), Equation.Parse("max(2,3)"), Equation.Parse("sqrt(16)")
        }, 1000);
        Assert.Equal(new[] { "2", "3", "4" }, batch.Select(value => value.ToString()));
    }
}
