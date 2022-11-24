package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

public class CoronerFieldFilter {
    public String name;
    public List<CoronerValueFilter> values = new ArrayList<>();

    public CoronerFieldFilter(String name, FilterOperator operator, Object value) {
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
        List<String> result = new ArrayList<>();

        for (CoronerValueFilter value : values) {
            result.add(value.toString());
        }
        return "\"" + name + "\": [" + String.join(",", result) + "  ]";
    }
}
