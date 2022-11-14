package backtraceio.coroner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import backtraceio.coroner.query.CoronerQueries;
import backtraceio.coroner.query.CoronerQueryFields;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseException;

public class CoronerClient {
    public final CoronerHttpClient coronerHttpClient;
    public final CoronerQueries coronerQueries;
    private final List<String> DEFAULT_ATTRIBUTES = Arrays.asList(
            CoronerQueryFields.FOLD_CALLSTACK,
            CoronerQueryFields.FOLD_GUID,
            CoronerQueryFields.FOLD_CLASSIFIERS
    );

    public CoronerClient(String apiUrl, String coronerToken) {
        this.coronerHttpClient = new CoronerHttpClient(apiUrl, coronerToken);
        this.coronerQueries = new CoronerQueries();
    }

    public CoronerResponse rxIdFilter(String rxId) throws Exception {
        return this.rxIdFilter(rxId, new ArrayList<>());
    }

    public CoronerResponse rxIdFilter(String rxId, List<String> customAttributes) throws CoronerResponseException, CoronerHttpException, IOException {
        List<String> attributes = Stream.concat(
                        DEFAULT_ATTRIBUTES.stream(),
                        customAttributes.stream())
                .collect(Collectors.toList());
        String coronerQuery = this.coronerQueries.filterByRxId(rxId, attributes);

        CoronerApiResponse response = this.coronerHttpClient.get(coronerQuery);

        if (response.error != null) {
            throw new CoronerResponseException(response.getError());
        }

        return response.getResponse();
    }
}
