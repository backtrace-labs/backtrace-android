package backtraceio.library.models.json;

import backtraceio.library.common.serializers.SerializedName;
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

    @SuppressWarnings({"UnusedDeclaration"})
    private SourceCode() { }

    public SourceCode(BacktraceStackFrame stackFrame) {
        this(stackFrame.line, stackFrame.sourceCodeFileName);
    }

    public SourceCode(Integer line, String sourceCodeFileName) {
        this.startLine = line;
        this.sourceCodeFileName = sourceCodeFileName;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public String getSourceCodeFileName() {
        return sourceCodeFileName;
    }
}
