package backtraceio.coroner.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public JsonObject buildRxIdGroup(final JsonArray filters, final List<String> headFolds) {
        return this.build(CoronerQueryFields.RXID, filters, headFolds);
    }

    private JsonObject build(final String groupName, final JsonArray filters, final List<String> headFolds) {
        final JsonObject folds = joinHeadFolds(headFolds);

        final JsonObject result = new JsonObject();

        final JsonArray group = new JsonArray();
        final JsonArray subGroup = new JsonArray();

        subGroup.add(groupName);
        group.add(subGroup);

        result.add(Constants.FOLD, folds);
        result.add(Constants.GROUP, group);
        result.add(Constants.OFFSET, new JsonPrimitive(OFFSET));
        result.add(Constants.LIMIT, new JsonPrimitive(LIMIT));
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
