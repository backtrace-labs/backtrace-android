package backtraceio.library.enums;

/**
 * Location statuses
 */
public enum LocationStatus {
    DISABLED("disabled"),
    ENABLED("enabled");

    private final String text;

    /**
     * @param text text related to enum
     */
    LocationStatus(final String text) {
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
