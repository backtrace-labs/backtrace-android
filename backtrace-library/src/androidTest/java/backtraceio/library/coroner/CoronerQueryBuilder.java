package backtraceio.library.coroner;

import java.util.List;
import java.util.stream.Collectors;

class CoronerQueryBuilder {
    private final String FOLD_HEAD = "head";
    private final int OFFSET = 0;
    private final int LIMIT = 1;

    public String build(String rxId, List<String> headFolds) {
        String rxFilter = filterEq(CoronerQueryFields.RXID, rxId);
        String folds = headFolds.stream().map(this::foldHead).collect(Collectors.joining(","));

        return "{" +
                "   \"group\":[" +
                "      \"" + CoronerQueryFields.RXID + "\"" +
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
                "      \"equal\"," +
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
