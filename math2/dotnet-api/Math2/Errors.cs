namespace Nologo2.Math2;

public class Math2Exception : Exception
{
    public Math2Exception(string message, Exception? inner = null) : base(message, inner) { }
}
public sealed class Math2ValidationException : Math2Exception { public Math2ValidationException(string message) : base(message) { } }
public sealed class Math2TransportException : Math2Exception { public Math2TransportException(string message, Exception inner) : base(message, inner) { } }
public sealed class Math2ProtocolException : Math2Exception { public Math2ProtocolException(string message, Exception? inner = null) : base(message, inner) { } }
public sealed class Math2ServiceException : Math2Exception
{
    public int Status { get; }
    public string Code { get; }
    public int? Position { get; }
    public Math2ServiceException(int status, string code, string message, int? position) : base(message)
    { Status = status; Code = code; Position = position; }
}
