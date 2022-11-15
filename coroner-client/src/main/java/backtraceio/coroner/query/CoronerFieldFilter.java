package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        String result = values.stream().map(x->x.toString()).collect(Collectors.joining(","));;
        return "\"" + name + "\": [" + result + "  ]";
    }
}
