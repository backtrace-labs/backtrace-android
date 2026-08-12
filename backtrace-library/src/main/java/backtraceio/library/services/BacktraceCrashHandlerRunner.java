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

    /** Logging seam so tests can prove no message carries handler arguments or credentials. */
    interface RunnerLogger {
        void error(String message, Throwable throwable);

        void info(String message);
    }

    private static final class LogcatRunnerLogger implements RunnerLogger {
        @Override
        public void error(String message, Throwable throwable) {
            if (throwable == null) {
                Log.e(LOG_TAG, message);
            } else {
                Log.e(LOG_TAG, message, throwable);
            }
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
     * Runs the crash handler. Diagnostics never include the handler arguments, the environment, or
     * the resolved library path: Crashpad passes the submission URL (which can carry the minidump
     * token) and every customer annotation on the argument vector, and this log ends up in Logcat
     * and CI artifacts.
     */
    public boolean run(String[] args, Map<String, String> environmentVariables) {
        if (environmentVariables == null) {
            logger.error("Cannot capture crash dump. Environment is unavailable.", null);
            return false;
        }

        String crashHandlerLibrary = environmentVariables.get(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        if (crashHandlerLibrary == null || crashHandlerLibrary.trim().isEmpty()) {
            logger.error("Cannot capture crash dump. Crash-handler path is unavailable.", null);
            return false;
        }

        // The library path was resolved in the application process; loading it here can still fail
        // (for example an APK-backed path the linker of this process cannot use). Contain both the
        // load and the dispatch separately so diagnostics distinguish the failure stages.
        try {
            loader.loadLibrary(crashHandlerLibrary);
        } catch (RuntimeException | LinkageError failure) {
            logger.error("Cannot load the native crash-handler library.", failure);
            return false;
        }

        final boolean result;
        try {
            result = crashHandler.handleCrash(args == null ? new String[0] : args);
        } catch (RuntimeException | LinkageError failure) {
            logger.error("Cannot execute the native crash handler.", failure);
            return false;
        }

        if (!result) {
            logger.error("Native crash-handler invocation returned failure.", null);
            return false;
        }

        logger.info("Successfully ran crash handler code.");
        return true;
    }
}
