package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import backtraceio.library.models.BacktraceStackFrame;

/**
 * Single instance of source data frame
 */
public class SourceCode {
    /**
     * Line number in source code where exception occurs
     */
    @SerializedName("startLine")
    public Integer startLine;

    /**
     * Filename to source file where exception occurs
     */
    @SerializedName("path")
    public String sourceCodeFileName;


    public SourceCode(BacktraceStackFrame stackFrame) {
        this.sourceCodeFileName = stackFrame.sourceCodeFileName;
        this.startLine = stackFrame.line;
    }
}
