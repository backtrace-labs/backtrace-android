package backtraceio.coroner.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

    public JsonArray getJson() {
        final JsonArray result = new JsonArray();
        final JsonObject filtersJson = new JsonObject();

        for (CoronerFieldFilter filter : filters) {
            filtersJson.add(filter.name, filter.getFilterValues());
        }

        result.add(filtersJson);

        return result;
    }
}
