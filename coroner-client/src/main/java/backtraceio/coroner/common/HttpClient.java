package backtraceio.coroner.common;

import java.io.IOException;

import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;

public interface HttpClient {
    CoronerApiResponse get(final String requestJson) throws CoronerHttpException, IOException;
}
