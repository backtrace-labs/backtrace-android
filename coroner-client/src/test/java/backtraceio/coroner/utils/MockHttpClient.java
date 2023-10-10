package backtraceio.coroner.utils;

import java.io.IOException;

import backtraceio.coroner.common.HttpClient;
import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;

public class MockHttpClient implements HttpClient {

    @Override
    public CoronerApiResponse get(String requestJson) throws CoronerHttpException, IOException {
        return null;
    }
}
