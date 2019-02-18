package backtraceio.library.enums;

public enum BatteryState {
    CHARGING("Charging"),
    UNKNOWN("Unknown"),
    FULL("Full"),
    UNPLAGGED("Unplagged");

    private final String text;

    /**
     * @param text text related to enum
     */
    BatteryState(final String text) {
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