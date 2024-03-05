package backtraceio.library.models;


import java.util.UUID;

import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.logger.BacktraceLogger;

/**
 * Backtrace stack frame
 */
public class BacktraceStackFrame {

    private static final transient String LOG_TAG = BacktraceStackFrame.class.getSimpleName();

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
    public transient String sourceCodeFileName; // why transient

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
    public static BacktraceStackFrame fromStackTraceElement(StackTraceElement frame) {
        if (frame == null || frame.getMethodName() == null) {
            BacktraceLogger.w(LOG_TAG, "Frame or method name is null");
            return null;
        }
        final String functionName = frame.getClassName() + "." + frame.getMethodName();
        final String fileName = frame.getFileName();
        final Integer line = frame.getLineNumber() > 0 ? frame.getLineNumber() : null;
        return new BacktraceStackFrame(functionName, fileName, line);
    }

    public BacktraceStackFrame(String functionName, String sourceCodeFileName, Integer line) {
        this.functionName = functionName;
        this.sourceCodeFileName = sourceCodeFileName; // why
        this.sourceCode = UUID.randomUUID().toString();
        this.line = line;
    }
}