package backtraceio.coroner.query;

public class CoronerValueFilter {
    public FilterOperator operator;
    public Object value;

    public CoronerValueFilter(final FilterOperator operator, final Object value) {
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toString() {
        return "    [" +
                "      \"" + operator.toString() + "\"," +
                "      \"" + value.toString() + "\"" +
                "    ]";
    }
}
