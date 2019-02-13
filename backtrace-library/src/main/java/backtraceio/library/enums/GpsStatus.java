package backtraceio.library.enums;

/**
 * GPS statuses
 */
public enum GpsStatus {
    DISABLED("disabled"),
    ENABLED("enabled");

    private final String text;

    /**
     * @param text text related to enum
     */
    GpsStatus(final String text) {
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
