package backtraceio.library.coroner;

import java.util.Arrays;
import java.util.List;

public class CoronerClient {
    private final List<String> DEFAULT_ATTRIBUTES = Arrays.asList(
            CoronerQueryFields.FOLD_CALLSTACK,
            CoronerQueryFields.FOLD_GUID,
            CoronerQueryFields.FOLD_CLASSIFIERS
    );

    public final CoronerHttpClient coronerHttpClient;
    public final CoronerQueries coronerQueries;

    public CoronerClient(String apiUrl, String coronerToken) {
        this.coronerHttpClient = new CoronerHttpClient(apiUrl, coronerToken);
        this.coronerQueries = new CoronerQueries();
    }

    public void rxIdFilter(String rxId) throws Exception {
        String coronerQuery = this.coronerQueries.filterByRxId(rxId, DEFAULT_ATTRIBUTES);
        CoronerApiResponse response = this.coronerHttpClient.get(coronerQuery);

        if (response.error != null) {
            throw new Exception(response.getError().getMessage()); // todo: another exception type
        }

    }
}
