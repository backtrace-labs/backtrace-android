package backtraceio.coroner;

import backtraceio.coroner.common.HttpClient;
import backtraceio.coroner.query.CoronerQueries;
import backtraceio.coroner.query.CoronerQueryFields;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseException;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoronerClient {
    private final HttpClient coronerHttpClient;
    private final CoronerQueries coronerQueries;
    private final List<String> DEFAULT_ATTRIBUTES = Arrays.asList(
            CoronerQueryFields.FOLD_CALLSTACK, CoronerQueryFields.FOLD_GUID, CoronerQueryFields.FOLD_CLASSIFIERS);

    public CoronerClient(final String apiUrl, final String coronerToken) {
        this(new CoronerHttpClient(apiUrl, coronerToken));
    }

    public CoronerClient(HttpClient httpClient) {
        this.coronerHttpClient = httpClient;
        this.coronerQueries = new CoronerQueries();
    }

    public CoronerResponse rxIdFilter(final String rxId)
            throws CoronerResponseException, CoronerHttpException, IOException {
        return this.rxIdFilter(rxId, new ArrayList<>());
    }

    public CoronerResponse rxIdFilter(final String rxId, final List<String> customAttributes)
            throws CoronerResponseException, CoronerHttpException, IOException {
        final List<String> attributes = concatAttributes(customAttributes);

        final JsonObject coronerQuery = this.coronerQueries.filterByRxId(rxId, attributes);

        return makeRequest(coronerQuery);
    }

    public CoronerResponse errorTypeTimestampFilter(
            final String errorType,
            final String timestampLeast,
            final String timestampMost,
            final List<String> customAttributes)
            throws CoronerResponseException, IOException, CoronerHttpException {
        final List<String> attributes = concatAttributes(customAttributes);

        final JsonObject coronerQuery =
                this.coronerQueries.filterByErrorTypeAndTimestamp(errorType, timestampLeast, timestampMost, attributes);

        return makeRequest(coronerQuery);
    }

    /**
     * GUID-correlated query returning up to {@code limit} reports. Use a limit larger than the
     * expected report count (qualification tests use 10); otherwise duplicate detection is
     * impossible.
     */
    public CoronerResponse guidTimestampFilter(
            final String guid,
            final String timestampLeast,
            final String timestampMost,
            final List<String> customAttributes,
            final int limit)
            throws CoronerResponseException, IOException, CoronerHttpException {
        final List<String> attributes = concatAttributes(customAttributes);

        final JsonObject coronerQuery =
                this.coronerQueries.filterByGuidAndTimestamp(guid, timestampLeast, timestampMost, attributes, limit);

        return makeRequest(coronerQuery);
    }

    /**
     * GUID plus exact {@code error.message} query returning up to {@code limit} reports. Use this
     * to prove a specific message was ingested exactly once even when the backend collapses a
     * GUID-wide grouped query into a single wildcard group.
     */
    public CoronerResponse guidMessageTimestampFilter(
            final String guid,
            final String errorMessage,
            final String timestampLeast,
            final String timestampMost,
            final List<String> customAttributes,
            final int limit)
            throws CoronerResponseException, IOException, CoronerHttpException {
        final List<String> attributes = concatAttributes(customAttributes);

        final JsonObject coronerQuery = this.coronerQueries.filterByGuidMessageAndTimestamp(
                guid, errorMessage, timestampLeast, timestampMost, attributes, limit);

        return makeRequest(coronerQuery);
    }

    private List<String> concatAttributes(final List<String> customAttributes) {
        final List<String> result = new ArrayList<>(customAttributes);
        result.addAll(DEFAULT_ATTRIBUTES);
        return result;
    }

    private CoronerResponse makeRequest(final JsonObject coronerQuery)
            throws CoronerResponseException, IOException, CoronerHttpException {
        final CoronerApiResponse response = this.coronerHttpClient.get(coronerQuery.toString());

        if (response.error != null) {
            throw new CoronerResponseException(response.getError());
        }

        return response.getResponse();
    }
}
