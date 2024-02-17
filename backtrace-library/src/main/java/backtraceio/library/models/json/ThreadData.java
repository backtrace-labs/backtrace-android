package backtraceio.library.models.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.models.BacktraceStackFrame;

/**
 * Generate information about application threads
 */
public class ThreadData {

    /**
     * All collected application threads information
     */
    public Map<String, ThreadInformation> threadInformation = new HashMap<>();

    /**
     * Application Id for current thread.
     * This value is used in mainThreadSection in output JSON file
     */
    private String mainThread = "";

    /**
     * Create instance of ThreadData class to collect information about used threads
     *
     * @param exceptionStack current BacktraceReport exception stack
     */
    public ThreadData(List<BacktraceStackFrame> exceptionStack) {
        generateCurrentThreadInformation(exceptionStack);
        processThreads();
    }

    /**
     * Get main thread id
     *
     * @return main thread id
     */
    public String getMainThread() {
        return mainThread;
    }

    /**
     * Generate information for current thread
     *
     * @param exceptionStack current BacktraceReport exception stack
     */
    private void generateCurrentThreadInformation(List<BacktraceStackFrame> exceptionStack) {
        Thread currThread = Thread.currentThread();
        mainThread = currThread.getName().toLowerCase();
        this.threadInformation.put(mainThread,
                new ThreadInformation(currThread, exceptionStack, true)
        );
    }

    /**
     * Process all threads and save information about thread and stacktrace
     */
    private void processThreads() {
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
                    stackFrame.add(BacktraceStackFrame.fromStackTraceElement(stackTraceElement));
                }
            }
            this.threadInformation.put(threadName, new ThreadInformation(thread, stackFrame,
                    false));
        }
    }
}
