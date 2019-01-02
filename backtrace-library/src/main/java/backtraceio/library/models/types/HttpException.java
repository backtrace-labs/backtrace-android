package backtraceio.library.models.types;

public class HttpException extends Exception {
    private Integer httpStatus;

    public HttpException(String message) {
        this(null, message);
    }

    public HttpException(Integer httpStatus) {
        this(httpStatus, null);
    }

    public HttpException(Integer httpStatus, String message) {
        super(message);
        setHttpStatus(httpStatus);
    }

    private void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
