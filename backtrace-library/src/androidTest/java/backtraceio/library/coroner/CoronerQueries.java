package backtraceio.library.coroner;

public class CoronerQueries {
    public static String filterByRxId(String rxId) {
        return "{" +
                "   \"group\":[" +
                "      \"fingerprint\"" +
                "   ]," +
                "   \"offset\":0," +
                "   \"limit\":20," +
                "   \"filter\":[" +
                "      {" +
                "         \"_rxid\":[" +
                "            [" +
                "               \"equal\"," +
                "               \"" + rxId + "\"" +
                "            ]" +
                "         ]" +
                "      }" +
                "   ]" +
                "}";
    }
}
