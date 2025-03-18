package backtraceio.coroner.common;

// Interface that the instrumentation test can implement to call android.util.Log
public interface AndroidLogDelegate {
    void d(String tag, String message);
}
