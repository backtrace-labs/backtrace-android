package backtraceio.library.models;


import androidx.annotation.NonNull;

public class UnhandledThrowableWrapper extends Exception {

    private final transient Throwable instance;

    public UnhandledThrowableWrapper(Throwable throwable) {
        this.instance = throwable;
    }

    @Override
    public String getMessage() {
        return this.instance.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return this.instance.getLocalizedMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return this.instance.getCause();
    }

    @NonNull
    @Override
    public StackTraceElement[] getStackTrace() {
        return this.instance.getStackTrace();
    }

    public String getClassifier() {
        return this.instance.getClass().getCanonicalName();
    }
}
