package backtraceio.library.models.types;

/**
 * Existing send method result statuses
 */
public enum BacktraceResultStatus {
    /**
     * Set when error occurs while sending diagnostic data
     */
    ServerError,

    /**
     * Set when data were send to API
     */
    Ok;

    public static BacktraceResultStatus enumOf(String val) {
        switch (val.toLowerCase()) {
            case "ok":
                return BacktraceResultStatus.Ok;

            case "servererror":
                return BacktraceResultStatus.ServerError;

            default:
                throw new IllegalArgumentException("Invalid BacktraceResultStatus enum value");
        }
    }
}
