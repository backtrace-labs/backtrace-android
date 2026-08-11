package backtraceio.library.services;

import android.util.Log;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.nativeCalls.BacktraceCrashHandlerWrapper;
import backtraceio.library.nativeCalls.SystemLoader;
import java.util.Map;

public class BacktraceCrashHandlerRunner {
    private static final String LOG_TAG = BacktraceCrashHandlerRunner.class.getSimpleName();
    private final BacktraceCrashHandlerWrapper crashHandler;
    private final SystemLoader loader;

    public static void main(String[] args) {
        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner();
        // A handler process that could not capture the dump must exit nonzero so the failure is
        // visible to Crashpad and in process diagnostics instead of looking like a success.
        if (!runner.run(args, System.getenv())) {
            System.exit(1);
        }
    }

    public BacktraceCrashHandlerRunner() {
        this(new BacktraceCrashHandlerWrapper(), new SystemLoader());
    }

    public BacktraceCrashHandlerRunner(BacktraceCrashHandlerWrapper crashHandler, SystemLoader loader) {
        this.crashHandler = crashHandler;
        this.loader = loader;
    }

    public boolean run(String[] args, Map<String, String> environmentVariables) {
        if (environmentVariables == null) {
            Log.e(LOG_TAG, "Cannot capture crash dump. Environment variables are undefined");
            return false;
        }

        String crashHandlerLibrary = environmentVariables.get(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        if (crashHandlerLibrary == null || crashHandlerLibrary.trim().isEmpty()) {
            Log.e(
                    LOG_TAG,
                    String.format(
                            "Cannot capture crash dump. Cannot find %s environment variable",
                            CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER));
            return false;
        }

        // The library path was resolved in the application process; loading it here can still fail
        // (for example an APK-backed path the linker of this process cannot use). Contain the
        // failure so the handler exits with a diagnosable error instead of an uncaught throw.
        try {
            loader.loadLibrary(crashHandlerLibrary);

            boolean result = crashHandler.handleCrash(args);
            if (!result) {
                Log.e(
                        LOG_TAG,
                        String.format("Cannot capture crash dump. Invocation parameters: %s", String.join(" ", args)));
                return false;
            }
        } catch (LinkageError | SecurityException failure) {
            Log.e(LOG_TAG, "Cannot load the native crash-handler library: " + crashHandlerLibrary, failure);
            return false;
        }

        Log.i(LOG_TAG, "Successfully ran crash handler code.");
        return true;
    }
}
