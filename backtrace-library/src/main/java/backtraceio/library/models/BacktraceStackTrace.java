package backtraceio.library.models;

import java.util.ArrayList;

/**
 * Backtrace stack trace
 */
public class BacktraceStackTrace {

    /**
     * Current exception
     */
    private Exception exception;

    /**
     * Collection of stacktrace elements
     */
    private ArrayList<BacktraceStackFrame> stackFrames = new ArrayList<>();

    /**
     * Create new instance of BacktraceStackTrace object
     *
     * @param exception current exception
     */
    public BacktraceStackTrace(Exception exception) {
        this.exception = exception;
        Initialize();
    }

    public ArrayList<BacktraceStackFrame> getStackFrames() {
        return stackFrames;
    }

    private void Initialize() {
        if(this.exception == null) {
            return;
        }
        SetStacktraceInformation(this.exception.getStackTrace());
    }

    private void SetStacktraceInformation(StackTraceElement[] frames) {
        if (frames == null || frames.length == 0) {
            return;
        }

        for (StackTraceElement frame : frames) {
            BacktraceStackFrame backtraceStackFrame = new BacktraceStackFrame(frame);
            this.stackFrames.add(backtraceStackFrame);
        }
    }
}
