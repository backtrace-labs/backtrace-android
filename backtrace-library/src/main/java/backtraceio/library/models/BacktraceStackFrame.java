package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

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
    public int line;

    /**
     * Create new instance of BacktraceStackFrame
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public BacktraceStackFrame() {

    }

    /**
     * Create new instance of BacktraceStackFrame
     * @param frame single stacktrace element
     */
    public BacktraceStackFrame(StackTraceElement frame) {
        if (frame == null || frame.getMethodName() == null) {
            return;
        }
        this.functionName = frame.getMethodName();
        this.line = frame.getLineNumber();
    }
}