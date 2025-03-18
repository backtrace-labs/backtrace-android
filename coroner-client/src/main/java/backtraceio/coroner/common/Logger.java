package backtraceio.coroner.common;

public final class Logger {
    private static AndroidLogDelegate delegate;

    private Logger() {
        // no instances
    }

    /**
     * Called from the Android test code to register a delegate that can log to Logcat.
     */
    public static synchronized void setDelegate(AndroidLogDelegate logDelegate) {
        delegate = logDelegate;
    }

    /**
     * Pure Java code can call this method instead of System.out.println().
     */
    public static synchronized void d(String tag, String message) {
        if (delegate != null) {
            delegate.d(tag, message);
        }
    }
}
