package backtraceio.library.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.nativeCalls.BacktraceCrashHandlerWrapper;
import backtraceio.library.nativeCalls.SystemLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Crashpad passes the submission URL (which can carry the minidump token) and every customer
 * annotation on the handler argument vector, and runner logs end up in Logcat and CI artifacts.
 * These tests prove no diagnostic message leaks that data on any failure stage.
 */
@RunWith(MockitoJUnitRunner.class)
public class BacktraceCrashHandlerRunnerLoggingTest {

    private static final String URL_SENTINEL = "SECRET_URL_TOKEN_SENTINEL";
    private static final String ANNOTATION_SENTINEL = "PRIVATE_CUSTOMER_VALUE_SENTINEL";
    private static final String[] SENSITIVE_ARGS = new String[] {
        "--url=https://example.invalid/minidump?token=" + URL_SENTINEL,
        "--annotation=private.customer.value=" + ANNOTATION_SENTINEL,
        "--database=/data/data/example/files/crashpad"
    };

    private static final class RecordingLogger implements BacktraceCrashHandlerRunner.RunnerLogger {
        final List<String> messages = new ArrayList<>();

        @Override
        public void error(String message, Throwable throwable) {
            messages.add(message);
            if (throwable != null) {
                messages.add(String.valueOf(throwable.getMessage()));
            }
        }

        @Override
        public void info(String message) {
            messages.add(message);
        }

        boolean anyMessageContains(String needle) {
            for (String message : messages) {
                if (message != null && message.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static HashMap<String, String> environmentWithHandlerPath() {
        HashMap<String, String> environment = new HashMap<>();
        environment.put(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, "path/to/lib");
        return environment;
    }

    @Test
    public void handlerFailureDoesNotLogArguments() {
        BacktraceCrashHandlerWrapper crashHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(crashHandler.handleCrash(any(String[].class))).thenReturn(false);
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(crashHandler, mock(SystemLoader.class), logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        assertFalse(logger.anyMessageContains(URL_SENTINEL));
        assertFalse(logger.anyMessageContains(ANNOTATION_SENTINEL));
        assertFalse(logger.anyMessageContains("private.customer.value"));
    }

    @Test
    public void handlerDispatchExceptionDoesNotLogArguments() {
        BacktraceCrashHandlerWrapper crashHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(crashHandler.handleCrash(any(String[].class))).thenThrow(new IllegalStateException("dispatch failed"));
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(crashHandler, mock(SystemLoader.class), logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        assertFalse(logger.anyMessageContains(URL_SENTINEL));
        assertFalse(logger.anyMessageContains(ANNOTATION_SENTINEL));
        assertTrue(logger.anyMessageContains("Cannot execute the native crash handler."));
    }

    @Test
    public void loadFailureDoesNotLogLibraryPathOrArguments() {
        SystemLoader loader = mock(SystemLoader.class);
        doThrow(new UnsatisfiedLinkError("dlopen failed")).when(loader).loadLibrary(any());
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(mock(BacktraceCrashHandlerWrapper.class), loader, logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        assertFalse(logger.anyMessageContains(URL_SENTINEL));
        assertFalse(logger.anyMessageContains("path/to/lib"));
        assertTrue(logger.anyMessageContains("Cannot load the native crash-handler library."));
    }

    @Test
    public void loadAndDispatchFailuresAreDistinguishable() {
        SystemLoader failingLoader = mock(SystemLoader.class);
        doThrow(new UnsatisfiedLinkError("dlopen failed")).when(failingLoader).loadLibrary(any());
        RecordingLogger loadLogger = new RecordingLogger();
        new BacktraceCrashHandlerRunner(mock(BacktraceCrashHandlerWrapper.class), failingLoader, loadLogger)
                .run(new String[] {}, environmentWithHandlerPath());

        BacktraceCrashHandlerWrapper failingHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(failingHandler.handleCrash(any(String[].class))).thenThrow(new UnsatisfiedLinkError("no HandleCrash"));
        RecordingLogger dispatchLogger = new RecordingLogger();
        new BacktraceCrashHandlerRunner(failingHandler, mock(SystemLoader.class), dispatchLogger)
                .run(new String[] {}, environmentWithHandlerPath());

        assertTrue(loadLogger.anyMessageContains("Cannot load the native crash-handler library."));
        assertFalse(loadLogger.anyMessageContains("Cannot execute the native crash handler."));
        assertTrue(dispatchLogger.anyMessageContains("Cannot execute the native crash handler."));
        assertFalse(dispatchLogger.anyMessageContains("Cannot load the native crash-handler library."));
    }

    @Test
    public void nullArgumentsAreToleratedByDispatch() {
        BacktraceCrashHandlerWrapper crashHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(crashHandler.handleCrash(any(String[].class))).thenReturn(true);
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(crashHandler, mock(SystemLoader.class), logger);

        assertTrue(runner.run(null, environmentWithHandlerPath()));
    }
}
