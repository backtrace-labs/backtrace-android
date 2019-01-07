package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import backtraceio.library.models.BacktraceStackFrame;

/**
 * Single instance of source data frame
 */
public class SourceCode{
    /**
     * Line number in source code where exception occurs
     */
    @SerializedName("startLine")
    public int startLine;

    /**
     * Filename to source file where exception occurs
     */
    @SerializedName("path")
    public String sourceCodeFileName;

    public SourceCode()
    {

    }

    /**
     * Get a SourceData instance from Exception stack
     * @param stackFrame exception stack
     * @return new instance of SourceCode
     */
    public static SourceCode FromExceptionStack(final BacktraceStackFrame stackFrame)
    {
        return new SourceCode()
        {{
            startLine = stackFrame.line;
            sourceCodeFileName = stackFrame.sourceCodeFileName;
        }};
    }

}
