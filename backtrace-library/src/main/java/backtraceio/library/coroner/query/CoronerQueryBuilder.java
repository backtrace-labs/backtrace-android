package backtraceio.library.coroner.query;

import java.util.List;
import java.util.stream.Collectors;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public String buildRxIdGroup(String rxId, List<String> headFolds) {
        return this.build(CoronerQueryFields.RXID, CoronerQueryFields.RXID, rxId, headFolds);
    }

    public String build(String groupName, String filterName, String filterValue, List<String> headFolds) {
        String rxFilter = filterEq(filterName, filterValue);
        String folds = headFolds.stream().map(this::foldHead).collect(Collectors.joining(","));

        return "{" +
                "   \"group\":[" +
                "      \"" + groupName + "\"" +
                "   ]," +
                "\"fold\": {" +
                folds +
                "}," +
                "   \"offset\":" + OFFSET + "," +
                "   \"limit\":" + LIMIT + "," +
                "   \"filter\":[" +
                rxFilter +
                "   ]" +
                "}";
    }

    private String filterEq(String name, String val) {
        return "{" +
                "  \"" + name + "\": [" +
                "    [" +
                "      \"" + FilterOperator.EQUAL + "\"," +
                "      \"" + val + "\"" +
                "    ]" +
                "  ]" +
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
