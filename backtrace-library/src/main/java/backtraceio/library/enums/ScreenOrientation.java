package backtraceio.library.enums;

public enum ScreenOrientation {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape"),
    UNDEFINED("undefined");

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
