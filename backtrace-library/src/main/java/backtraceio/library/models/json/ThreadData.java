package backtraceio.library.models.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.models.BacktraceStackFrame;

public class ThreadData {


    /// <summary>
    /// All collected application threads information
    /// </summary>
    public HashMap<String, ThreadInformation> threadInformations = new HashMap<String,
            ThreadInformation>();

    public String getMainThread() {
        return mainThread;
    }

    /// <summary>
    /// Application Id for current thread. This value is used in mainThreadSection in output JSON
    // file
    /// </summary>
    private String mainThread = "";

    public ThreadData(ArrayList<BacktraceStackFrame> exceptionStack) {
        generateCurrentThreadInformation(exceptionStack);
        processThreads();
    }

    private void generateCurrentThreadInformation(ArrayList<BacktraceStackFrame> exceptionStack) {
        Thread currThread = Thread.currentThread();
        mainThread = currThread.getName().toLowerCase();
        this.threadInformations.put(mainThread, new ThreadInformation(currThread, exceptionStack,
                true));
    }

    public void processThreads() {
        Map<Thread, StackTraceElement[]> myMap = Thread.getAllStackTraces();

        for (Map.Entry<Thread, StackTraceElement[]> entry : myMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Thread thread = entry.getKey();
            StackTraceElement[] stack = entry.getValue();
            String threadName = thread.getName().toLowerCase();
            ArrayList<BacktraceStackFrame> stackFrame = new ArrayList<>();
            if (this.getMainThread().equals(threadName)) {
                continue;
            }
            if (stack != null && stack.length != 0) {
                for (StackTraceElement stackTraceElement : stack) {
                    stackFrame.add(new BacktraceStackFrame(stackTraceElement));
                }
            }
            this.threadInformations.put(threadName, new ThreadInformation(thread, stackFrame,
                    false));
        }
    }
}
