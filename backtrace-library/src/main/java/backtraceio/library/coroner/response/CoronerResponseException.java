package backtraceio.library.coroner.response;

import backtraceio.library.coroner.CoronerError;

public class CoronerResponseException extends Exception {
    private final CoronerError coronerError;

    public CoronerResponseException(CoronerError coronerError) {
        super(coronerError.getMessage());
        this.coronerError = coronerError;
    }

    @SuppressWarnings("unused")
    public CoronerError getCoronerError() {
        return coronerError;
    }
}
