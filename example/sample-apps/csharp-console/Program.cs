internal sealed class FakeCommand
{
    public string CommandText { get; set; } = string.Empty;
}

internal static class Program
{
    internal static async Task<string> LoadNameAsync()
    {
        await Task.Yield();
        return "review sample";
    }

    internal static async void AuditInBackground()
    {
        await Task.Yield();
        Console.WriteLine("Audit complete");
    }

    private static void Main()
    {
        var name = LoadNameAsync().Result;
        var command = new FakeCommand();
        command.CommandText = $"SELECT * FROM users WHERE name = '{name}'";
        AuditInBackground();
        Console.WriteLine(command.CommandText);
    }
}
