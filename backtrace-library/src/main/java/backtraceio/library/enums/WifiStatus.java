package backtraceio.library.enums;

/**
 * WiFi statuses
 */
public enum WifiStatus {
    NOT_PERMITTED("not permitted"),
    DISABLED("disabled"),
    ENABLED("enabled");

    private final String text;

    /**
     * @param text text related to enum
     */
    WifiStatus(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
