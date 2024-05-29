package backtraceio.library.nativeCalls;

public class BacktraceCrashHandler {
    public static native boolean handleCrash(String[] args);
}