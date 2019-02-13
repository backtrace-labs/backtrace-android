package backtraceio.library.enums;

/**
 * Bluetooth statuses
 */
public enum BluetoothStatus {
    NOT_PERMITTED("not_permitted"),
    DISABLED("disabled"),
    ENABLED("enabled");

    private final String text;

    /**
     * @param text text related to enum
     */
    BluetoothStatus(final String text) {
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
