package backtraceio.library.models;

import java.util.ArrayList;

import backtraceio.library.logger.BacktraceLogger;

/**
 * Backtrace stack trace
 */
public class BacktraceStackTrace {

    private static final transient String LOG_TAG = BacktraceStackTrace.class.getSimpleName();

    /**
     * Current exception
     */
    private final Exception exception;

    /**
     * Collection of stacktrace elements
     */
    private final ArrayList<BacktraceStackFrame> stackFrames = new ArrayList<>();

    /**
     * Create new instance of BacktraceStackTrace object
     *
     * @param exception current exception
     */
    public BacktraceStackTrace(Exception exception) {
        this.exception = exception;
        initialize();
    }

    public ArrayList<BacktraceStackFrame> getStackFrames() {
        return stackFrames;
    }

    public Exception getException() {
        return exception;
    }

    private void initialize() {
        StackTraceElement[] stackTraceElements = this.exception != null ?
                this.exception.getStackTrace() : Thread.currentThread().getStackTrace();
        if (stackTraceElements == null || stackTraceElements.length == 0) {
            BacktraceLogger.w(LOG_TAG, "StackTraceElements are null or empty");
            return;
        }
        setStacktraceInformation(stackTraceElements);
    }

    private void setStacktraceInformation(StackTraceElement[] frames) {
        if (frames == null || frames.length == 0) {
            BacktraceLogger.w(LOG_TAG, "StackTraceFrames are null or empty");
            return;
        }

        for (StackTraceElement frame : frames) {
            if (frame != null && frame.getFileName() != null &&
                    frame.getFileName().startsWith("Backtrace")) {
                BacktraceLogger.d(LOG_TAG, "Skipping frame because it comes from inside the Backtrace library");
                continue;
            }
            BacktraceStackFrame backtraceStackFrame = BacktraceStackFrame.fromStackTraceElement(frame);
            this.stackFrames.add(backtraceStackFrame);
        }
    }
}
