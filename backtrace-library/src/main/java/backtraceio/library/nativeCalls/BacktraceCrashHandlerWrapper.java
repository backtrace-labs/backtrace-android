package backtraceio.library.nativeCalls;

public class BacktraceCrashHandlerWrapper {
    public boolean handleCrash(String[] args) {
        return BacktraceCrashHandler.handleCrash(args);
    }
}
