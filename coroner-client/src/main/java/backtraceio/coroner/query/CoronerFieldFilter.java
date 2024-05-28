package backtraceio.coroner.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

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

    public JsonArray getFilterValues() {
        final JsonArray result = new JsonArray();

        for (CoronerValueFilter value : values) {
            JsonArray filter = new JsonArray();

            filter.add(new JsonPrimitive(value.operator.toString()));
            filter.add(new JsonPrimitive(value.value.toString()));

            result.add(filter);
        }
        return result;
    }
}
