package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public String buildRxIdGroup(final String filters, final List<String> headFolds) {
        return this.build(CoronerQueryFields.RXID, filters, headFolds);
    }

    private String build(final String groupName, final String filters, final List<String> headFolds) {
        final String folds = joinHeadFolds(headFolds);

        return "{" +
                "\"group\":[" +
                "  [\"" + groupName + "\"]" +
                "]," +
                "\"fold\": {" + folds + "}," +
                "   \"offset\":" + OFFSET + "," +
                "   \"limit\":" + LIMIT + "," +
                "   \"filter\":[" + filters + "]" +
                "}";
    }
    private String joinHeadFolds(final List<String> folds) {
        final List<String> result = new ArrayList<>();

        for (String fold : folds) {
            result.add(foldHead(fold));
        }

        return String.join(",", result);
    }
    private String foldHead(final String name) {
        return this.fold(name, FOLD_HEAD);
    }
    private String fold(final String name, final String val) {
        return "\"" + name + "\": " +
                "[" + "[\"" + val + "\"" + "]" + "]";
    }
}
