package backtraceio.coroner.query;

public class CoronerValueFilter {
    public final FilterOperator operator;
    public final Object value;

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
