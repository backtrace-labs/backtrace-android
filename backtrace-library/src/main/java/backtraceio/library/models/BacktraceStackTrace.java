package backtraceio.library.models;

import java.util.ArrayList;
import java.util.List;

public class BacktraceStackTrace {

    private Exception exception;
    private ArrayList<BacktraceStackFrame> stackFrames = new ArrayList<>();

    public BacktraceStackTrace(Exception exception)
    {
        this.exception = exception;
        Initialize();
    }

    public ArrayList<BacktraceStackFrame> getStackFrames() {
        return stackFrames;
    }

    private void Initialize()
    {
        SetStacktraceInformation(this.exception.getStackTrace());
    }

    private void SetStacktraceInformation(StackTraceElement[] frames) {
        if (frames == null || frames.length == 0)
        {
            return;
        }

        for (StackTraceElement frame : frames) {
            BacktraceStackFrame backtraceStackFrame = new BacktraceStackFrame(frame);
            this.stackFrames.add(backtraceStackFrame);
        }
    }
}
