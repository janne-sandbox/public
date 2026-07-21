using System.Net.Http.Json;
using System.Text.Json;

namespace Nologo2.Math2;

public sealed class Math2Client : IDisposable
{
    private readonly HttpClient http;
    private readonly bool ownsClient;

    public Math2Client(string baseUrl = "http://127.0.0.1:8080", TimeSpan? timeout = null, HttpClient? httpClient = null)
    {
        http = httpClient ?? new HttpClient();
        ownsClient = httpClient is null;
        http.BaseAddress ??= new Uri(baseUrl.TrimEnd('/') + "/");
        http.Timeout = timeout ?? TimeSpan.FromSeconds(10);
    }

    public async Task<Number> CalculateAsync(Equation equation, int? precision = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(equation);
        var query = "api/v1/calculate?equation=" + Uri.EscapeDataString(equation.ToString()) + Precision(precision);
        var response = await SendAsync(query, cancellationToken).ConfigureAwait(false);
        return Number.FromString(response.GetProperty("result").GetString()
            ?? throw new Math2ProtocolException("Result is null"));
    }

    public async Task<IReadOnlyList<Number>> CalculateBatchAsync(IEnumerable<Equation> equations, int? precision = null, CancellationToken cancellationToken = default)
    {
        var values = equations?.ToArray() ?? throw new ArgumentNullException(nameof(equations));
        if (values.Length == 0 || values.Any(value => value is null))
            throw new Math2ValidationException("Equations must not be empty or contain null");
        var query = "api/v1/calculate/batch?equations=" + Uri.EscapeDataString(string.Join(',', values.AsEnumerable())) + Precision(precision);
        var response = await SendAsync(query, cancellationToken).ConfigureAwait(false);
        var results = response.GetProperty("results").EnumerateArray().Select(value =>
            Number.FromString(value.GetString() ?? throw new Math2ProtocolException("Result is null"))).ToArray();
        if (results.Length != values.Length) throw new Math2ProtocolException("Result count does not match request");
        return results;
    }

    public void Dispose() { if (ownsClient) http.Dispose(); }

    private async Task<JsonElement> SendAsync(string query, CancellationToken cancellationToken)
    {
        HttpResponseMessage response;
        try { response = await http.GetAsync(query, cancellationToken).ConfigureAwait(false); }
        catch (Exception exception) when (exception is HttpRequestException or TaskCanceledException)
        { throw new Math2TransportException("Math2 request failed", exception); }
        using (response)
        {
            JsonDocument body;
            try { body = await response.Content.ReadFromJsonAsync<JsonDocument>(cancellationToken: cancellationToken).ConfigureAwait(false)
                    ?? throw new Math2ProtocolException("Response is empty"); }
            catch (JsonException exception) { throw new Math2ProtocolException("Response is not valid JSON", exception); }
            using (body)
            {
                if (!response.IsSuccessStatusCode)
                {
                    var root = body.RootElement;
                    throw new Math2ServiceException((int)response.StatusCode,
                        root.TryGetProperty("code", out var code) ? code.GetString() ?? "UNKNOWN" : "UNKNOWN",
                        root.TryGetProperty("message", out var message) ? message.GetString() ?? "Service error" : "Service error",
                        root.TryGetProperty("position", out var position) && position.ValueKind == JsonValueKind.Number ? position.GetInt32() : null);
                }
                return body.RootElement.Clone();
            }
        }
    }

    private static string Precision(int? value) => value.HasValue ? "&precision=" + value.Value : "";
}
