package backtraceio.coroner.response;

import backtraceio.coroner.CoronerError;

public class CoronerResponseException extends Exception {
    private final CoronerError coronerError;

    public CoronerResponseException(final CoronerError coronerError) {
        super(coronerError.getMessage());
        this.coronerError = coronerError;
    }

    @SuppressWarnings("unused")
    public CoronerError getCoronerError() {
        return coronerError;
    }
}
