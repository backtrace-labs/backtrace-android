package backtraceio.coroner.query;

public enum FilterOperator {
    EQUAL("equal"),
    AT_MOST("at-most"),
    AT_LEAST("at-least");

    private final String text;

    /**
     * @param text
     */
    FilterOperator(final String text) {
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
