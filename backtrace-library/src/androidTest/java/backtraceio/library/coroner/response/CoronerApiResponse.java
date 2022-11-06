package backtraceio.library.coroner.response;

import backtraceio.library.coroner.CoronerError;


public class CoronerApiResponse {
    public CoronerError error;
    public CoronerResponse response;

    @SuppressWarnings("unused")
    public CoronerApiResponse() {

    }

    @SuppressWarnings("unused")
    public CoronerApiResponse(CoronerError error, CoronerResponse response) {
        this.error = error;
        this.response = response;
    }

    public CoronerError getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public void setError(CoronerError error) {
        this.error = error;
    }

    public CoronerResponse getResponse() {
        return this.response;
    }

    @SuppressWarnings("unused")
    public void setResponse(CoronerResponse response) {
        this.response = response;
    }
}

