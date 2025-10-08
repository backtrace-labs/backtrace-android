package backtraceio.coroner.response;

import backtraceio.coroner.CoronerError;

public class CoronerApiResponse {
    public CoronerResponse response;
    public CoronerError error;

    @SuppressWarnings("unused")
    public CoronerApiResponse() {}

    @SuppressWarnings("unused")
    public CoronerApiResponse(final CoronerResponse response, final CoronerError error) {
        this.response = response;
        this.error = error;
    }

    public CoronerResponse getResponse() {
        return this.response;
    }

    @SuppressWarnings("unused")
    public void setResponse(CoronerResponse response) {
        this.response = response;
    }

    public CoronerError getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public void setError(CoronerError error) {
        this.error = error;
    }
}
