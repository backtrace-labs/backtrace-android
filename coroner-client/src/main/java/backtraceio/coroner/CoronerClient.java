package backtraceio.coroner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import backtraceio.coroner.common.HttpClient;
import backtraceio.coroner.query.CoronerQueries;
import backtraceio.coroner.query.CoronerQueryFields;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseException;

public class CoronerClient {
    private final HttpClient coronerHttpClient;
    private final CoronerQueries coronerQueries;
    private final List<String> DEFAULT_ATTRIBUTES = Arrays.asList(
            CoronerQueryFields.FOLD_CALLSTACK,
            CoronerQueryFields.FOLD_GUID,
            CoronerQueryFields.FOLD_CLASSIFIERS
    );

    public CoronerClient(final String apiUrl, final String coronerToken) {
        this(new CoronerHttpClient(apiUrl, coronerToken));
    }

    public CoronerClient(HttpClient httpClient) {
        this.coronerHttpClient = httpClient;
        this.coronerQueries = new CoronerQueries();
    }

    public CoronerResponse rxIdFilter(final String rxId) throws CoronerResponseException, CoronerHttpException, IOException  {
        return this.rxIdFilter(rxId, new ArrayList<>());
    }

    public CoronerResponse rxIdFilter(final String rxId, final List<String> customAttributes) throws CoronerResponseException, CoronerHttpException, IOException {
        final List<String> attributes = concatAttributes(customAttributes);

        final String coronerQuery = this.coronerQueries.filterByRxId(rxId, attributes);

        return makeRequest(coronerQuery);
    }

    public CoronerResponse errorTypeTimestampFilter(final String errorType, final String timestampLeast, final String timestampMost, final List<String> customAttributes) throws CoronerResponseException, IOException, CoronerHttpException {
        final List<String> attributes = concatAttributes(customAttributes);

        String coronerQuery = this.coronerQueries.filterByErrorTypeAndTimestamp(errorType, timestampLeast, timestampMost, attributes);

        return makeRequest(coronerQuery);
    }

    private List<String> concatAttributes(final List<String> customAttributes) {
        final List<String> result = new ArrayList<>(customAttributes);
        result.addAll(DEFAULT_ATTRIBUTES);
        return result;
    }

    private CoronerResponse makeRequest(final String coronerQuery) throws CoronerResponseException, IOException, CoronerHttpException {
        final CoronerApiResponse response = this.coronerHttpClient.get(coronerQuery);

        if (response.error != null) {
            throw new CoronerResponseException(response.getError());
        }

        return response.getResponse();
    }
}
