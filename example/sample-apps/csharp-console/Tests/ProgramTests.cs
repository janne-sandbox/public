internal static class ProgramTests
{
    internal static void NameIsReturned()
    {
        if (Program.LoadNameAsync().GetAwaiter().GetResult() != "review sample")
        {
            throw new InvalidOperationException("Unexpected sample name");
        }
    }
}
