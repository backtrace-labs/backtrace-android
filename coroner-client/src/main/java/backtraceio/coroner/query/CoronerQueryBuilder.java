package backtraceio.coroner.query;

import java.util.ArrayList;
import java.util.List;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public String buildRxIdGroup(String filters, List<String> headFolds) {
        return this.build(CoronerQueryFields.RXID, filters, headFolds);
    }

    private String build(String groupName, String filters, List<String> headFolds) {
        String folds = joinHeadFolds(headFolds);

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
    private String joinHeadFolds(List<String> folds) {
        List<String> result = new ArrayList<>();

        for (String fold : folds) {
            result.add(foldHead(fold));
        }

        return String.join(",", result);
    }
    private String foldHead(String name) {
        return this.fold(name, FOLD_HEAD);
    }
    private String fold(String name, String val) {
        return "\"" + name + "\": " +
                "[" + "[\"" + val + "\"" + "]" + "]";
    }
}
