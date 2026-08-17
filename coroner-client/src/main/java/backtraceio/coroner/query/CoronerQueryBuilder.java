package backtraceio.coroner.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

class CoronerQueryBuilder {
    private static final String FOLD_HEAD = "head";
    private static final int OFFSET = 0;
    private static final int DEFAULT_LIMIT = 1;
    static final int MAX_LIMIT = 100;

    public JsonObject buildRxIdGroup(final JsonArray filters, final List<String> headFolds) {
        return this.buildRxIdGroup(filters, headFolds, DEFAULT_LIMIT);
    }

    /**
     * Builds an {@code _rxid}-grouped query returning up to {@code limit} result groups. Duplicate
     * detection requires a limit larger than the expected report count: with the historical
     * hard-coded limit of one, a query could never return more than one report, so an
     * "exactly one" assertion could not fail on duplicates.
     */
    public JsonObject buildRxIdGroup(final JsonArray filters, final List<String> headFolds, final int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return this.build(CoronerQueryFields.RXID, filters, headFolds, limit);
    }

    private JsonObject build(
            final String groupName, final JsonArray filters, final List<String> headFolds, final int limit) {
        final JsonObject folds = joinHeadFolds(headFolds);

        final JsonObject result = new JsonObject();

        final JsonArray group = new JsonArray();
        final JsonArray subGroup = new JsonArray();

        subGroup.add(groupName);
        group.add(subGroup);

        result.add(Constants.FOLD, folds);
        result.add(Constants.GROUP, group);
        result.add(Constants.OFFSET, new JsonPrimitive(OFFSET));
        result.add(Constants.LIMIT, new JsonPrimitive(limit));
        result.add(Constants.FILTER, filters);

        return result;
    }

    private JsonObject joinHeadFolds(final List<String> folds) {
        final JsonObject result = new JsonObject();

        for (String fold : folds) {
            result.add(fold, foldHead());
        }

        return result;
    }

    private JsonArray foldHead() {
        final JsonArray foldValue = new JsonArray();
        final JsonArray foldInnerValue = new JsonArray();

        foldInnerValue.add(FOLD_HEAD);
        foldValue.add(foldInnerValue);

        return foldValue;
    }
}
