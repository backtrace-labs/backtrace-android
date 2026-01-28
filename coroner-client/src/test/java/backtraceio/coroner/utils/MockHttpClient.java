package backtraceio.coroner.utils;

import backtraceio.coroner.common.HttpClient;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import java.io.IOException;

public class MockHttpClient implements HttpClient {

    @Override
    public CoronerApiResponse get(String requestJson) throws CoronerHttpException, IOException {
        return null;
    }
}
