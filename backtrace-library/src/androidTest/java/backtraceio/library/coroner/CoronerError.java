package backtraceio.library.coroner;

class CoronerError {
    public String message;
    public int code;

    public CoronerError() {

    }

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

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
