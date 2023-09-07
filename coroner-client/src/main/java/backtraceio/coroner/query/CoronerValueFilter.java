package backtraceio.coroner.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class CoronerValueFilter {
    public final FilterOperator operator;
    public final Object value;

    public CoronerValueFilter(final FilterOperator operator, final Object value) {
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toString() {
        return this.get().toString();
    }

    public JsonElement get() {
        JsonArray result = new JsonArray();

        result.add(operator.toString());
        result.add(value.toString());

        return result;
    }
}
