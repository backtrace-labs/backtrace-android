package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.models.BacktraceStackFrame;

/**
 * Collect all source data information about current program
 */
public class SourceCodeData {
    /**
     * Source code information about current executed program
     */
    public Map<String, SourceCode> data = new HashMap<>();

    public SourceCodeData(ArrayList<BacktraceStackFrame> exceptionStack)
    {
        if (exceptionStack == null || exceptionStack.size() == 0)
        {
            return;
        }

        for(BacktraceStackFrame backtraceStackFrame : exceptionStack)
        {
            if (backtraceStackFrame == null || backtraceStackFrame.sourceCode.equals(""))
            {
                continue;
            }
            String id = backtraceStackFrame.sourceCode;
            SourceCode value = new SourceCode(backtraceStackFrame);
            data.put(id, value);
        }
    }
}