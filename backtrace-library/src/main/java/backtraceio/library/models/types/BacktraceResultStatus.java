package backtraceio.library.models.types;

/// <summary>
/// Existing send method result statuses
/// </summary>
public enum BacktraceResultStatus
{
    /**
     * Set when error occurs while sending diagnostic data
     */
    ServerError,
    
    /**
     * Set when data were send to API
     */
    Ok,
}