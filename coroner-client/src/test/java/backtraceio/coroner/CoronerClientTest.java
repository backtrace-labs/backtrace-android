package backtraceio.coroner;

import static backtraceio.coroner.utils.ResourceUtils.QUERY_CORONER_RXID_123;
import static backtraceio.coroner.utils.ResourceUtils.QUERY_CORONER_RXID_123_ATTR_ERR_MSG;
import static backtraceio.coroner.utils.ResourceUtils.QUERY_CORONER_TIMESTAMP_ERR_TYPE;
import static backtraceio.coroner.utils.ResourceUtils.RESPONSE_OPERATION_ERROR_JSON;
import static backtraceio.coroner.utils.ResourceUtils.RESPONSE_RX_FILTER_CORONER_JSON;
import static backtraceio.coroner.utils.ResourceUtils.RESPONSE_TIMESTAMP_ERR_TYPE_CORONER_JSON;
import static backtraceio.coroner.utils.ResourceUtils.readResourceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import backtraceio.coroner.response.CoronerResponse;
import backtraceio.coroner.response.CoronerResponseException;
import backtraceio.coroner.response.CoronerResponseProcessingException;
import backtraceio.coroner.serialization.GsonWrapper;
import backtraceio.coroner.utils.MockHttpClient;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CoronerClientTest {
    private static final String rxId = "12345";
    private MockHttpClient mockHttpClient;
    private CoronerClient client;

    @Before
    public void init() {
        this.mockHttpClient = mock(MockHttpClient.class);
        this.client = new CoronerClient(mockHttpClient);
    }

    @Test
    public void rxIdFilter()
            throws CoronerResponseException, IOException, CoronerHttpException, CoronerResponseProcessingException {
        // GIVEN
        final String expectedJsonQuery = readResourceFile(QUERY_CORONER_RXID_123);
        final String jsonResponse = readResourceFile(RESPONSE_RX_FILTER_CORONER_JSON);

        final CoronerApiResponse expectedResponse = GsonWrapper.fromJson(jsonResponse, CoronerApiResponse.class);

        // MOCK
        when(mockHttpClient.get(Mockito.contains(expectedJsonQuery))).thenReturn(expectedResponse);

        // WHEN
        final CoronerResponse result = client.rxIdFilter(rxId);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getResultsNumber());
        assertEquals("Invalid index of selected element!", result.getAttribute(0, "error.message", String.class));
    }

    @Test
    public void rxIdFilterAttributes()
            throws CoronerResponseException, IOException, CoronerHttpException, CoronerResponseProcessingException {
        // GIVEN
        final String expectedJsonQuery = readResourceFile(QUERY_CORONER_RXID_123_ATTR_ERR_MSG);
        final String jsonResponse = readResourceFile(RESPONSE_RX_FILTER_CORONER_JSON);
        final List<String> customAttributes = Arrays.asList("error.message");
        final CoronerApiResponse expectedResponse = GsonWrapper.fromJson(jsonResponse, CoronerApiResponse.class);

        // MOCK
        when(mockHttpClient.get(Mockito.contains(expectedJsonQuery))).thenReturn(expectedResponse);

        // WHEN
        final CoronerResponse result = client.rxIdFilter(rxId, customAttributes);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getResultsNumber());
        assertEquals("Invalid index of selected element!", result.getAttribute(0, "error.message", String.class));
    }

    @Test
    public void errorTypeTimestampFilter()
            throws CoronerResponseException, IOException, CoronerHttpException, CoronerResponseProcessingException {
        // GIVEN
        final String expectedJsonQuery = readResourceFile(QUERY_CORONER_TIMESTAMP_ERR_TYPE);
        final String jsonResponse = readResourceFile(RESPONSE_TIMESTAMP_ERR_TYPE_CORONER_JSON);
        final CoronerApiResponse expectedResponse = GsonWrapper.fromJson(jsonResponse, CoronerApiResponse.class);

        final String errorType = "Crash";
        final long timestampStart = 0L;
        final long timestampEnd = 10000000000L;
        final List<String> customAttributes = Arrays.asList("error.message");

        // MOCK
        when(mockHttpClient.get(Mockito.contains(expectedJsonQuery))).thenReturn(expectedResponse);

        // WHEN
        final CoronerResponse result = client.errorTypeTimestampFilter(
                errorType, Long.toString(timestampStart), Long.toString(timestampEnd), customAttributes);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getResultsNumber());
        assertEquals("exception", result.getAttribute(0, "classifiers", String.class));
        assertEquals("DumpWithoutCrash", result.getAttribute(0, "error.message", String.class));
    }

    @Test
    public void errorResponseTest() throws IOException, CoronerHttpException, CoronerResponseException {
        // GIVEN
        final String jsonResponse = readResourceFile(RESPONSE_OPERATION_ERROR_JSON);
        final CoronerApiResponse expectedResponse = GsonWrapper.fromJson(jsonResponse, CoronerApiResponse.class);
        final String errorMessage = "empty body";

        // MOCK
        when(mockHttpClient.get(Mockito.anyString())).thenReturn(expectedResponse);

        // WHEN
        try {
            client.rxIdFilter(rxId);
        } catch (CoronerResponseException exception) {
            // THEN
            assertEquals(errorMessage, exception.getMessage());
            assertNotNull(exception.getCoronerError());
            assertEquals(errorMessage, exception.getCoronerError().getMessage());
            assertEquals(32769, exception.getCoronerError().getCode());
        }
    }
}
