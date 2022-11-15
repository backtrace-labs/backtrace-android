package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoronerFiltersBuilder {
    private final List<CoronerFieldFilter> filters;

    public CoronerFiltersBuilder() {
        this.filters = new ArrayList<>();
    }

    public CoronerFiltersBuilder addFilter(final String name, final FilterOperator operator, final Object value) {
        final Optional<CoronerFieldFilter> filter = this.filters.stream().filter(x->x.getName().equals(name)).findAny();

        if(filter.isPresent()) {
            filter.get().addValueFilter(operator, value);
        } else {
            this.filters.add(new CoronerFieldFilter(name, operator, value));
        }

        return this;
    }

    public String get() {
        String result = filters.stream().map(x->x.toString()).collect(Collectors.joining(","));

        return "{ " + result + " }";
    }

}
