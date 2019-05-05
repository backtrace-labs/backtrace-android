package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import backtraceio.library.logger.BacktraceLogger;

/**
 * Backtrace stack frame
 */
public class BacktraceStackFrame {

    private static transient String LOG_TAG = BacktraceStackFrame.class.getSimpleName();

    /**
     * Function where exception occurs
     */
    @SerializedName("funcName")
    public String functionName;
    /**
     * Line number in source code where exception occurs
     */
    @SerializedName("line")
    public Integer line = null;

    /**
     * Source code file name where exception occurs
     */
    @SerializedName("sourceCode")
    public String sourceCode;

    /**
     * Source code file name where exception occurs
     */
    public transient String sourceCodeFileName;

    /**
     * Create new instance of BacktraceStackFrame
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public BacktraceStackFrame() {
    }

    /**
     * Create new instance of BacktraceStackFrame
     *
     * @param frame single stacktrace element
     */
    public BacktraceStackFrame(StackTraceElement frame) {
        if (frame == null || frame.getMethodName() == null) {
            BacktraceLogger.w(LOG_TAG, "Frame or method name is null");
            return;
        }
        this.functionName = frame.getClassName() + "." + frame.getMethodName();
        this.sourceCodeFileName = frame.getFileName();
        this.sourceCode = UUID.randomUUID().toString();
        this.line = frame.getLineNumber() > 0 ? frame.getLineNumber() : null;
    }
}