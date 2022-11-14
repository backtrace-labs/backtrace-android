package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

public class CoronerQueries {
    private final CoronerQueryBuilder builder;

    public CoronerQueries() {
        builder = new CoronerQueryBuilder();
    }

    public String filterByRxId(String rxId) {
        return this.filterByRxId(rxId, new ArrayList<>());
    }

    public String filterByRxId(String rxId, List<String> attributes) {
        return this.builder.buildRxIdGroup(rxId, attributes);
    }
}
