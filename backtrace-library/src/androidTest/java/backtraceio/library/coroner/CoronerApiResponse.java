package backtraceio.library.coroner;

import java.util.List;

class CoronerApiResponse {
    public CoronerError error;
    public CoronerResponse response;

    public CoronerApiResponse() {

    }

    public CoronerApiResponse(CoronerError error, CoronerResponse response) {
        this.error = error;
        this.response = response;
    }

    public CoronerError getError() {
        return error;
    }

    public void setError(CoronerError error) {
        this.error = error;
    }

    public CoronerResponse getResponse() {
        return response;
    }

    public void setResponse(CoronerResponse response) {
        this.response = response;
    }
}

