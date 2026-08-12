package backtraceio.coroner.query;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class CoronerQueries {
    private final CoronerQueryBuilder builder;

    public CoronerQueries() {
        builder = new CoronerQueryBuilder();
    }

    public JsonObject filterByRxId(final String rxId) {
        return this.filterByRxId(rxId, new ArrayList<>());
    }

    public JsonObject filterByRxId(final String rxId, final List<String> attributes) {
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();
        filtersBuilder.addFilter(CoronerQueryFields.RXID, FilterOperator.EQUAL, rxId);

        return this.builder.buildRxIdGroup(filtersBuilder.getJson(), attributes);
    }

    public JsonObject filterByErrorTypeAndTimestamp(
            final String errorType,
            final String timestampLeast,
            final String timestampMost,
            final List<String> attributes) {
        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();
        filtersBuilder.addFilter(CoronerQueryFields.ERROR_TYPE, FilterOperator.EQUAL, errorType);
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_LEAST, timestampLeast + ".");
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_MOST, timestampMost + ".");

        return this.builder.buildRxIdGroup(filtersBuilder.getJson(), attributes);
    }

    /**
     * GUID-correlated query with a caller-chosen result limit. GUID is the primary correlation key
     * for qualification reports (fatal reports carry no controllable {@code error.message}), and
     * the limit must exceed the expected count for duplicate detection to be possible.
     */
    public JsonObject filterByGuidAndTimestamp(
            final String guid,
            final String timestampLeast,
            final String timestampMost,
            final List<String> attributes,
            final int limit) {
        if (guid == null || guid.trim().isEmpty()) {
            throw new IllegalArgumentException("guid cannot be null or blank");
        }

        final CoronerFiltersBuilder filtersBuilder = new CoronerFiltersBuilder();
        filtersBuilder.addFilter(CoronerQueryFields.GUID, FilterOperator.EQUAL, guid);
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_LEAST, timestampLeast + ".");
        filtersBuilder.addFilter(CoronerQueryFields.TIMESTAMP, FilterOperator.AT_MOST, timestampMost + ".");

        return this.builder.buildRxIdGroup(filtersBuilder.getJson(), attributes, limit);
    }
}
