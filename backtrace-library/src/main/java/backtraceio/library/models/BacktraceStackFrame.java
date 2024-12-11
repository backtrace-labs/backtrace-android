package backtraceio.library.models;


import java.util.UUID;

import backtraceio.library.common.json.serialization.SerializedName;
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
    public transient String sourceCodeFileName;

    /**
     * Create new instance of BacktraceStackFrame
     *
     * @deprecated
     * Use {@link #fromStackTraceElement(StackTraceElement frame)} instead.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public BacktraceStackFrame() {}

    /**
     * Create new instance of BacktraceStackFrame
     *
     * @deprecated
     * Use {@link #fromStackTraceElement(StackTraceElement frame)} instead.
     */
    @Deprecated
    public BacktraceStackFrame(StackTraceElement frame) {
        BacktraceStackFrame obj = BacktraceStackFrame.fromStackTraceElement(frame);
        if (obj == null) {
            throw new IllegalArgumentException("Wrong stacktrace element frame - can`t be null");
        }
        this.functionName = obj.functionName;
        this.sourceCodeFileName = obj.sourceCodeFileName;
        this.sourceCode = obj.sourceCode;
        this.line = obj.line;
    }

    /**
     * Create new instance of BacktraceStackFrame
     *
     * @param frame single stacktrace element
     */
    public static BacktraceStackFrame fromStackTraceElement(StackTraceElement frame) {
        if (frame == null || frame.getMethodName() == null) {
            BacktraceLogger.e(LOG_TAG, "Frame or method name is null");
            throw new IllegalArgumentException("Frame or method name is null");
        }
        final String functionName = frame.getClassName() + "." + frame.getMethodName();
        final String fileName = frame.getFileName();
        final Integer line = frame.getLineNumber() > 0 ? frame.getLineNumber() : null;
        return new BacktraceStackFrame(functionName, fileName, line);
    }

    public BacktraceStackFrame(String functionName, String sourceCodeFileName, Integer line) {
        this(functionName, sourceCodeFileName, line, UUID.randomUUID().toString());
    }

    public BacktraceStackFrame(String functionName, String sourceCodeFileName, Integer line, String sourceCodeUuid) {
        this.functionName = functionName;
        this.sourceCodeFileName = sourceCodeFileName;
        this.sourceCode = sourceCodeUuid;
        this.line = line;
    }
}
