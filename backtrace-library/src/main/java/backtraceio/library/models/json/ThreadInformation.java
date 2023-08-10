package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import backtraceio.library.models.BacktraceStackFrame;

/**
 * Get an information about single thread passed in constructor
 */
public class ThreadInformation {
    /**
     * Thread name
     */
    @SerializedName("name")
    public String name;

    /**
     * Denotes whether a thread is a faulting thread
     */
    @SerializedName("fault")
    @SuppressWarnings({"UnusedDeclaration"})
    private final Boolean fault;

    /**
     * Current thread stacktrace
     */
    @SerializedName("stack")
    @SuppressWarnings({"UnusedDeclaration"})
    private final ArrayList<BacktraceStackFrame> stack;

    /**
     * Create new instance of ThreadInformation
     *
     * @param threadName thread name
     * @param fault      denotes whether a thread is a faulting thread - in most cases main thread
     * @param stack      exception stack information
     */
    private ThreadInformation(String threadName, Boolean fault, ArrayList<BacktraceStackFrame>
            stack) {
        this.stack = stack == null ? new ArrayList<BacktraceStackFrame>() : stack;
        this.name = threadName;
        this.fault = fault;
    }

    /**
     * Create new instance of ThreadInformation
     *
     * @param thread        thread to analyse
     * @param stack         exception stack information
     * @param currentThread is current thread flag
     */
    ThreadInformation(Thread thread, ArrayList<BacktraceStackFrame> stack, Boolean currentThread) {
        this(thread.getName().toLowerCase(), currentThread, stack);
    }

    public String getName() {
        return name;
    }

    public Boolean getFault() {
        return fault;
    }

    public ArrayList<BacktraceStackFrame> getStack() {
        return stack;
    }
}
