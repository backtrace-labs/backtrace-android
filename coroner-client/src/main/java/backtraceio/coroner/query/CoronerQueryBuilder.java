package backtraceio.coroner.query;

import java.util.List;
import java.util.stream.Collectors;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public String buildRxIdGroup(String filters, List<String> headFolds) {
        return this.build(CoronerQueryFields.RXID, filters, headFolds);
    }

    private String build(String groupName, String filters, List<String> headFolds) {
        String folds = headFolds.stream().map(this::foldHead).collect(Collectors.joining(","));

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

    private String foldHead(String name) {
        return this.fold(name, FOLD_HEAD);
    }

    private String fold(String name, String val) {
        return "\"" + name + "\": " +
                "[" + "[\"" + val + "\"" + "]" + "]";
    }
}
