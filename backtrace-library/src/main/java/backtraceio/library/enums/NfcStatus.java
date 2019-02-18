package backtraceio.library.enums;

/**
 * NFC statuses
 */
public enum NfcStatus {
    NOT_AVAILABLE("NotAvailable"),
    DISABLED("Disabled"),
    ENABLED("Enabled");

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
