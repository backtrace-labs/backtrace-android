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
    private final RunnerLogger logger;

    /**
     * Logging seam without a {@link Throwable} overload: exception messages commonly carry the
     * rejected library path ({@code UnsatisfiedLinkError}) or credential-bearing URLs, and this
     * process's Logcat is collected into CI artifacts. Tests use it to prove no message leaks.
     */
    interface RunnerLogger {
        void error(String message);

        void info(String message);
    }

    private static final class LogcatRunnerLogger implements RunnerLogger {
        @Override
        public void error(String message) {
            Log.e(LOG_TAG, message);
        }

        @Override
        public void info(String message) {
            Log.i(LOG_TAG, message);
        }
    }

    public static void main(String[] args) {
        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner();
        // A handler process that could not capture the dump must exit nonzero so the failure is
        // visible in process diagnostics instead of looking like a success.
        if (!runner.run(args, System.getenv())) {
            System.exit(1);
        }
    }

    public BacktraceCrashHandlerRunner() {
        this(new BacktraceCrashHandlerWrapper(), new SystemLoader());
    }

    public BacktraceCrashHandlerRunner(BacktraceCrashHandlerWrapper crashHandler, SystemLoader loader) {
        this(crashHandler, loader, new LogcatRunnerLogger());
    }

    BacktraceCrashHandlerRunner(BacktraceCrashHandlerWrapper crashHandler, SystemLoader loader, RunnerLogger logger) {
        this.crashHandler = crashHandler;
        this.loader = loader;
        this.logger = logger;
    }

    /**
     * Runs the crash handler. Diagnostics carry only a stable stage code and the failure class
     * name — never the handler arguments, environment, resolved library path, exception message,
     * or stack trace: Crashpad passes the submission URL (which can carry the minidump token) and
     * every application-provided annotation on the argument vector, and exception messages embed
     * paths.
     */
    public boolean run(String[] args, Map<String, String> environmentVariables) {
        if (environmentVariables == null) {
            logger.error("BT_HANDLER_ENV_UNAVAILABLE: Cannot capture crash dump. Environment is unavailable.");
            return false;
        }

        String crashHandlerLibrary = environmentVariables.get(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        if (crashHandlerLibrary == null || crashHandlerLibrary.trim().isEmpty()) {
            logger.error("BT_HANDLER_PATH_UNAVAILABLE: Cannot capture crash dump. Crash-handler path is unavailable.");
            return false;
        }

        try {
            loader.loadLibrary(crashHandlerLibrary);
        } catch (RuntimeException | LinkageError failure) {
            logger.error("BT_HANDLER_LOAD_FAILURE: Cannot load the native crash-handler library. Failure type: "
                    + failureType(failure));
            return false;
        }

        final boolean result;
        try {
            result = crashHandler.handleCrash(args == null ? new String[0] : args);
        } catch (RuntimeException | LinkageError failure) {
            logger.error("BT_HANDLER_DISPATCH_FAILURE: Cannot execute the native crash handler. Failure type: "
                    + failureType(failure));
            return false;
        }

        if (!result) {
            logger.error("BT_HANDLER_RETURNED_FAILURE: Native crash-handler invocation returned failure.");
            return false;
        }

        logger.info("Successfully ran crash handler code.");
        return true;
    }

    private static String failureType(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        String type = failure.getClass().getName();
        return type == null || type.trim().isEmpty() ? "unknown" : type;
    }
}
