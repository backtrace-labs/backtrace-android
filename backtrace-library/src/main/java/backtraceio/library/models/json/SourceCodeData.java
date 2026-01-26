package backtraceio.library.models.json;

import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceStackFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collect all source data information about current program
 */
public class SourceCodeData {
    private static final transient String LOG_TAG = SourceCodeData.class.getSimpleName();

    /**
     * Source code information about current executed program
     */
    public final Map<String, SourceCode> data = new HashMap<>();

    public SourceCodeData(List<BacktraceStackFrame> exceptionStack) {
        BacktraceLogger.d(LOG_TAG, "Initialization source code data");
        if (exceptionStack == null || exceptionStack.size() == 0) {
            BacktraceLogger.w(LOG_TAG, "Exception stack is null or empty");
            return;
        }

        for (BacktraceStackFrame backtraceStackFrame : exceptionStack) {
            if (backtraceStackFrame == null || backtraceStackFrame.sourceCode.equals("")) {
                BacktraceLogger.w(LOG_TAG, "Stack frame is null or sourceCode is empty");
                continue;
            }
            String id = backtraceStackFrame.sourceCode;
            SourceCode value = new SourceCode(backtraceStackFrame);
            data.put(id, value);
        }
    }
}
