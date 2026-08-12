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
 * annotation on the handler argument vector, and exception messages commonly embed rejected
 * library paths; runner logs end up in Logcat and CI artifacts. These tests throw failures whose
 * <em>messages contain every sensitive sentinel</em> and prove that no logged message leaks them:
 * the logger contract carries no {@link Throwable}, and messages hold only a stable stage code and
 * the failure class name.
 */
@RunWith(MockitoJUnitRunner.class)
public class BacktraceCrashHandlerRunnerLoggingTest {

    private static final String URL_SENTINEL = "https://example.invalid/minidump?token=SECRET_URL_TOKEN_SENTINEL";
    private static final String ANNOTATION_SENTINEL = "--annotation=private.customer.value=PRIVATE_CUSTOMER_SENTINEL";
    private static final String DATABASE_SENTINEL = "/data/data/customer/files/crashpad";
    private static final String LIBRARY_SENTINEL =
            "/data/app/~~opaque/split_config.arm64_v8a.apk!/lib/arm64-v8a/libbacktrace-native.so";
    private static final String ATTACHMENT_SENTINEL = "/customer/attachment/medical-record.txt";
    private static final String ALL_SENTINELS = URL_SENTINEL + " " + ANNOTATION_SENTINEL + " " + DATABASE_SENTINEL + " "
            + LIBRARY_SENTINEL + " " + ATTACHMENT_SENTINEL;
    private static final String[] SENSITIVE_ARGS =
            new String[] {"--url=" + URL_SENTINEL, ANNOTATION_SENTINEL, "--database=" + DATABASE_SENTINEL};

    private static final class RecordingLogger implements BacktraceCrashHandlerRunner.RunnerLogger {
        final List<String> messages = new ArrayList<>();

        @Override
        public void error(String message) {
            messages.add(message);
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

        void assertNoSentinelLeaked() {
            for (String sentinel : new String[] {
                "SECRET_URL_TOKEN_SENTINEL",
                "PRIVATE_CUSTOMER_SENTINEL",
                "private.customer.value",
                DATABASE_SENTINEL,
                LIBRARY_SENTINEL,
                ATTACHMENT_SENTINEL,
                "token="
            }) {
                assertFalse("Sensitive value leaked into runner log: " + sentinel, anyMessageContains(sentinel));
            }
        }
    }

    private static HashMap<String, String> environmentWithHandlerPath() {
        HashMap<String, String> environment = new HashMap<>();
        environment.put(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, LIBRARY_SENTINEL);
        return environment;
    }

    @Test
    public void loadFailureWithSentinelMessageLeaksNothing() {
        SystemLoader loader = mock(SystemLoader.class);
        doThrow(new UnsatisfiedLinkError("dlopen failed: " + ALL_SENTINELS))
                .when(loader)
                .loadLibrary(any());
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(mock(BacktraceCrashHandlerWrapper.class), loader, logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        logger.assertNoSentinelLeaked();
        assertTrue(logger.anyMessageContains("BT_HANDLER_LOAD_FAILURE"));
        assertTrue(logger.anyMessageContains("java.lang.UnsatisfiedLinkError"));
    }

    @Test
    public void dispatchFailureWithSentinelMessageLeaksNothing() {
        BacktraceCrashHandlerWrapper crashHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(crashHandler.handleCrash(any(String[].class)))
                .thenThrow(new IllegalStateException("dispatch failed: " + ALL_SENTINELS));
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(crashHandler, mock(SystemLoader.class), logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        logger.assertNoSentinelLeaked();
        assertTrue(logger.anyMessageContains("BT_HANDLER_DISPATCH_FAILURE"));
        assertTrue(logger.anyMessageContains("java.lang.IllegalStateException"));
    }

    @Test
    public void handlerReturnedFailureLeaksNothing() {
        BacktraceCrashHandlerWrapper crashHandler = mock(BacktraceCrashHandlerWrapper.class);
        when(crashHandler.handleCrash(any(String[].class))).thenReturn(false);
        RecordingLogger logger = new RecordingLogger();

        BacktraceCrashHandlerRunner runner =
                new BacktraceCrashHandlerRunner(crashHandler, mock(SystemLoader.class), logger);

        assertFalse(runner.run(SENSITIVE_ARGS, environmentWithHandlerPath()));
        logger.assertNoSentinelLeaked();
        assertTrue(logger.anyMessageContains("BT_HANDLER_RETURNED_FAILURE"));
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

        assertTrue(loadLogger.anyMessageContains("BT_HANDLER_LOAD_FAILURE"));
        assertFalse(loadLogger.anyMessageContains("BT_HANDLER_DISPATCH_FAILURE"));
        assertTrue(dispatchLogger.anyMessageContains("BT_HANDLER_DISPATCH_FAILURE"));
        assertFalse(dispatchLogger.anyMessageContains("BT_HANDLER_LOAD_FAILURE"));
    }

    @Test
    public void missingEnvironmentAndBlankPathUseStageCodes() {
        RecordingLogger logger = new RecordingLogger();
        BacktraceCrashHandlerRunner runner = new BacktraceCrashHandlerRunner(
                mock(BacktraceCrashHandlerWrapper.class), mock(SystemLoader.class), logger);

        assertFalse(runner.run(new String[] {}, null));
        assertTrue(logger.anyMessageContains("BT_HANDLER_ENV_UNAVAILABLE"));

        HashMap<String, String> blankPath = new HashMap<>();
        blankPath.put(CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER, "   ");
        assertFalse(runner.run(new String[] {}, blankPath));
        assertTrue(logger.anyMessageContains("BT_HANDLER_PATH_UNAVAILABLE"));
        logger.assertNoSentinelLeaked();
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
