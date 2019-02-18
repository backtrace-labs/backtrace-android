package backtraceio.library.enums;

/**
 * Screen orientation statuses
 */
public enum ScreenOrientation {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    UNDEFINED("Unknown");

    private final String text;

    /**
     * @param text text related to enum
     */
    ScreenOrientation(final String text) {
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
