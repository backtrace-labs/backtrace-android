package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

public class CoronerFiltersBuilder {
    private final List<CoronerFieldFilter> filters;

    public CoronerFiltersBuilder() {
        this.filters = new ArrayList<>();
    }

    public CoronerFiltersBuilder addFilter(final String name, final FilterOperator operator, final Object value) {
        CoronerFieldFilter element = null;

        for (CoronerFieldFilter filter : filters) {
            if (filter.getName().equals(name)) {
                element = filter;
            }
        }

        if (element != null) {
            element.addValueFilter(operator, value);
        } else {
            this.filters.add(new CoronerFieldFilter(name, operator, value));
        }

        return this;
    }

    public String get() {
        final List<String> result = new ArrayList<>();

        for (CoronerFieldFilter filter : filters) {
            result.add(filter.toString());
        }

        return "{ " + String.join(",", result) + " }";
    }

}
