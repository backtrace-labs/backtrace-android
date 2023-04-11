package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

public class CoronerFieldFilter {
    public final String name;
    public final List<CoronerValueFilter> values = new ArrayList<>();

    public CoronerFieldFilter(final String name, final FilterOperator operator, final Object value) {
        this.name = name;
        this.addValueFilter(operator, value);
    }

    public String getName() {
        return this.name;
    }

    public void addValueFilter(final FilterOperator operator, final Object value) {
        values.add(new CoronerValueFilter(operator, value));
    }

    @Override
    public String toString() {
        final List<String> result = new ArrayList<>();

        for (CoronerValueFilter value : values) {
            result.add(value.toString());
        }
        return "\"" + name + "\": [" + String.join(",", result) + "]";
    }
}
