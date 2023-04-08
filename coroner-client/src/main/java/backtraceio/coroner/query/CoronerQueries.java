package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

public class CoronerQueries {
    private final CoronerQueryBuilder builder;

    public CoronerQueries() {
        builder = new CoronerQueryBuilder();
    }

    public String filterByRxId(final String rxId) {
        return this.filterByRxId(rxId, new ArrayList<>());
    }

    public String filterByErrorTypeAndTimestamp(final String errorType, final String timestampLeast, final String timestampMost, final List<String> attributes) {
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();
        filtersBuilder.addFilter(CoronerQueryFields.ERROR_TYPE, FilterOperator.EQUAL, errorType);
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_LEAST, timestampLeast + ".");
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_MOST, timestampMost + ".");

        return this.builder.buildRxIdGroup(filtersBuilder.get(), attributes);
    }

    public String filterByRxId(final String rxId, final List<String> attributes) {
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();
        filtersBuilder.addFilter(CoronerQueryFields.RXID, FilterOperator.EQUAL, rxId);

        return this.builder.buildRxIdGroup(filtersBuilder.get(), attributes);
    }
}
