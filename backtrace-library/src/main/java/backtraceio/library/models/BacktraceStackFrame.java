package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

public class BacktraceStackFrame {

    /// <summary>
    /// Function where exception occurs
    /// </summary>
    //  [JsonProperty(PropertyName = "funcName")]
    @SerializedName("funcName")
    public String functionName;
//
//    /// <summary>
//    /// Line number in source code where exception occurs
//    /// </summary>
//        [JsonProperty(PropertyName = "line")]
    @SerializedName("line")
    public int line;

    public BacktraceStackFrame(){

    }

    public BacktraceStackFrame(StackTraceElement frame)
    {
        if (frame == null || frame.getMethodName() == null)
        {
            return;
        }

        this.functionName = frame.getMethodName();
        this.line = frame.getLineNumber();
    }
}
