package backtraceio.library.coroner;

import java.util.ArrayList;
import java.util.List;

class CoronerQueries {
    private final CoronerQueryBuilder builder;

    public CoronerQueries() {
        builder = new CoronerQueryBuilder();
    }

    public String filterByRxId(String rxId) {
        return this.filterByRxId(rxId, new ArrayList<>());
    }

    public String filterByRxId(String rxId, List<String> attributes) {
        return this.builder.build(rxId, attributes);
    }
}
