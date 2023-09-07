package backtraceio.coroner.response;

/**
 * Exception which represents a HTTP fault
 */
public class CoronerHttpException extends Exception {

    /**
     * HTTP status code
     */
    private Integer httpStatus;

    /**
     * Create new instance with message
     *
     * @param message received exception message
     */
    @SuppressWarnings("unused")
    public CoronerHttpException(String message) {
        this(null, message);
    }

    /**
     * Create new instance with HTTP status
     *
     * @param httpStatus received HTTP status code
     */
    @SuppressWarnings("unused")
    public CoronerHttpException(final Integer httpStatus) {
        this(httpStatus, null);
    }

    /**
     * Create new instance with HTTP status and without message
     *
     * @param httpStatus received HTTP status code
     * @param message    received exception message
     */
    public CoronerHttpException(final Integer httpStatus, final String message) {
        super(message);
        setHttpStatus(httpStatus);
    }

    /**
     * Return HTTP status code
     *
     * @return HTTP status code
     */
    @SuppressWarnings("unused")
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Set HTTP status code
     *
     * @param httpStatus received HTTP status code
     */
    private void setHttpStatus(final int httpStatus) {
        this.httpStatus = httpStatus;
    }
}