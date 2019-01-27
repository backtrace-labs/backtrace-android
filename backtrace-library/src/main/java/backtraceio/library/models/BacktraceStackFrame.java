package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class BacktraceStackFrame {

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

    /// <summary>
    /// Source code file name where exception occurs
    /// </summary>
    /**
     *
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
    public BacktraceStackFrame() {}

    /**
     * Create new instance of BacktraceStackFrame
     * @param frame single stacktrace element
     */
    public BacktraceStackFrame(StackTraceElement frame) {
        if (frame == null || frame.getMethodName() == null) {
            return;
        }
        this.functionName = frame.getMethodName();
        this.sourceCodeFileName = frame.getFileName();
        this.sourceCode = UUID.randomUUID().toString();
        this.line = frame.getLineNumber() > 0 ? frame.getLineNumber() : null;
    }
}