package backtraceio.library.enums;

/**
 * NFC statuses
 */
public enum NfcStatus {
    NOT_AVAILABLE("not available"),
    DISABLED("disabled"),
    ENABLED("enabled");

    private final String text;

    /**
     * @param text text related to enum
     */
    NfcStatus(final String text) {
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
