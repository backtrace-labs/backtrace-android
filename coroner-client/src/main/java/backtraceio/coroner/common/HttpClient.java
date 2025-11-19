package backtraceio.coroner.common;

import backtraceio.coroner.response.CoronerApiResponse;
import backtraceio.coroner.response.CoronerHttpException;
import java.io.IOException;

public interface HttpClient {
    CoronerApiResponse get(final String requestJson) throws CoronerHttpException, IOException;
}
