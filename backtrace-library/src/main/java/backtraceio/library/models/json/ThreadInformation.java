package backtraceio.library.models.json;

import java.util.ArrayList;

import backtraceio.library.models.BacktraceStackFrame;

/// <summary>
/// Get an information about single thread passed in constructor
/// </summary>
public class ThreadInformation
{
    /// <summary>
    /// Thread Name
    /// </summary>
    public String Name;

    /// <summary>
    /// Denotes whether a thread is a faulting thread 
    /// </summary>
    public Boolean Fault;


    ArrayList<BacktraceStackFrame> Stack;

    /// <summary>
    /// Create new instance of ThreadInformation
    /// </summary>
    /// <param name="threadName">Thread name</param>
    /// <param name="fault">Denotes whether a thread is a faulting thread - in most cases main thread</param>
    /// <param name="stack">Exception stack information</param>
    public ThreadInformation(String threadName, Boolean fault, ArrayList<BacktraceStackFrame> stack)
    {
        Stack = stack == null ? new ArrayList<BacktraceStackFrame>(): stack;
        Name = threadName;
        Fault = fault;
    }

//    /// <summary>
//    /// Create new instance of ThreadInformation
//    /// </summary>
//    /// <param name="thread">Thread to analyse</param>
//    /// <param name="stack">Exception stack information</param>
//    /// <param name="currentThread">Is current thread flag</param>
    public ThreadInformation(Thread thread, ArrayList<BacktraceStackFrame> stack, Boolean currentThread)
    {
        this(thread.getName().toLowerCase(), currentThread, stack);
    }
}
