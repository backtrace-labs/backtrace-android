package backtraceio.library.coroner;

public class CoronerError {
    public String message;
    public int code;

    @SuppressWarnings("unused")
    public CoronerError() {

    }

    @SuppressWarnings("unused")
    public CoronerError(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @SuppressWarnings("unused")
    public int getCode() {
        return code;
    }

    @SuppressWarnings("unused")
    public void setCode(int code) {
        this.code = code;
    }
}
