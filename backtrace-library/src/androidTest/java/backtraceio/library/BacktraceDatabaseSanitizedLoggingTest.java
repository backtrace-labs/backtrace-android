package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.logger.Logger;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Native-integration failure diagnostics must never carry credentials or paths: a custom
 * credential provider or an {@link UnsatisfiedLinkError} can embed a submission URL, token, or
 * filesystem path in its exception message. These tests throw failures whose messages contain
 * every sensitive sentinel and prove the sanitized logging contract: no sentinel in any message,
 * no {@code Throwable} passed to the logger, and a stable stage code plus exception class present.
 */
@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseSanitizedLoggingTest {

    private static final String ALL_SENTINELS = "https://example.invalid/minidump?token=SECRET_URL_TOKEN_SENTINEL"
            + " --annotation=private.application.value=SENSITIVE_ATTRIBUTE_SENTINEL"
            + " /data/user/0/example.application/files/crashpad"
            + " /data/app/~~opaque/split_config.arm64_v8a.apk!/lib/arm64-v8a/libbacktrace-native.so"
            + " /data/user/0/example.application/files/attachment.txt";

    private static final class RecordingGlobalLogger implements Logger {
        final List<String> messages = new ArrayList<>();
        int throwableOverloadCalls;

        @Override
        public int d(String tag, String message) {
            messages.add(message);
            return 0;
        }

        @Override
        public int w(String tag, String message) {
            messages.add(message);
            return 0;
        }

        @Override
        public int e(String tag, String message) {
            messages.add(message);
            return 0;
        }

        @Override
        public int e(String tag, String message, Throwable tr) {
            throwableOverloadCalls++;
            messages.add(message);
            if (tr != null && tr.getMessage() != null) {
                messages.add(tr.getMessage());
            }
            return 0;
        }

        boolean anyMessageContains(String needle) {
            for (String message : messages) {
                if (message != null && message.contains(needle)) {
                    return true;
                }
            }
            return false;
        }

        void assertSanitized(String expectedCode, String expectedFailureClass) {
            assertFalse("SENTINEL leaked into diagnostics", anyMessageContains("SENTINEL"));
            assertFalse("token= leaked into diagnostics", anyMessageContains("token="));
            assertFalse("crashpad path leaked into diagnostics", anyMessageContains("/files/crashpad"));
            assertFalse("library path leaked into diagnostics", anyMessageContains("libbacktrace-native.so"));
            assertEquals("Throwable must never reach the logger on native paths", 0, throwableOverloadCalls);
            assertTrue("missing stable code " + expectedCode, anyMessageContains(expectedCode));
            if (expectedFailureClass != null) {
                assertTrue("missing failure class " + expectedFailureClass, anyMessageContains(expectedFailureClass));
            }
        }
    }

    private Context context;
    private Logger previousLogger;
    private RecordingGlobalLogger recordingLogger;
    private BacktraceDatabase database;
    private BacktraceClient client;
    private BacktraceCredentials credentials;

    @Before
    public void setUp() {
        this.previousLogger = BacktraceLogger.getLogger();
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File databaseDirectory = new File(this.context.getFilesDir(), "sanitized-logging-test-" + System.nanoTime());
        assertTrue(databaseDirectory.mkdirs());
        this.database = new BacktraceDatabase(this.context, databaseDirectory.getAbsolutePath());
        this.credentials = new BacktraceCredentials("https://test.sp.backtrace.io", "1231231231231");
        this.client = new BacktraceClient(this.context, this.credentials);
        this.database.start();

        this.recordingLogger = new RecordingGlobalLogger();
        BacktraceLogger.setLogger(this.recordingLogger);
    }

    @After
    public void tearDown() {
        // Restore the global logger even when an assertion failed mid-test.
        BacktraceLogger.setLogger(this.previousLogger);
    }

    @Test
    public void preparationFailureWithSentinelMessageIsSanitized() {
        this.database.useCrashHandlerConfiguration(new CrashHandlerConfiguration() {
            @Override
            public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
                throw new IllegalStateException("preparation failed: " + ALL_SENTINELS);
            }
        });

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        this.recordingLogger.assertSanitized("BT_NATIVE_PREPARE_FAILURE", "java.lang.IllegalStateException");
    }

    @Test
    public void credentialFailureWithSentinelMessageIsSanitized() {
        BacktraceCredentials throwingCredentials = new BacktraceCredentials("https://test.sp.backtrace.io", "token") {
            @Override
            public android.net.Uri getMinidumpSubmissionUrl() {
                throw new IllegalStateException("credential resolution failed: " + ALL_SENTINELS);
            }
        };

        assertEquals(false, this.database.setupNativeIntegration(this.client, throwingCredentials));
        this.recordingLogger.assertSanitized("BT_NATIVE_PREPARE_FAILURE", "java.lang.IllegalStateException");
    }

    @Test
    public void bridgeFailureWithSentinelMessageIsSanitized() {
        this.database.useNativeCommunication(new backtraceio.library.interfaces.NativeCommunication() {
            @Override
            public boolean handleCrash(String[] args) {
                return false;
            }

            @Override
            public boolean initializeJavaCrashHandler(
                    String url,
                    String databasePath,
                    String classPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    String[] environmentVariables) {
                throw new UnsatisfiedLinkError("bridge failed: " + ALL_SENTINELS);
            }

            @Override
            public boolean initializeCrashHandler(
                    String url,
                    String databasePath,
                    String handlerPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    boolean enableClientSideUnwinding,
                    backtraceio.library.enums.UnwindingMode unwindingMode) {
                return false;
            }
        });

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        this.recordingLogger.assertSanitized("BT_NATIVE_BRIDGE_FAILURE", "java.lang.UnsatisfiedLinkError");
    }

    @Test
    public void bridgeReturnedFalseUsesStableDiagnosticCode() {
        this.database.useNativeCommunication(new backtraceio.library.interfaces.NativeCommunication() {
            @Override
            public boolean handleCrash(String[] args) {
                return false;
            }

            @Override
            public boolean initializeJavaCrashHandler(
                    String url,
                    String databasePath,
                    String classPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    String[] environmentVariables) {
                return false;
            }

            @Override
            public boolean initializeCrashHandler(
                    String url,
                    String databasePath,
                    String handlerPath,
                    String[] attributeKeys,
                    String[] attributeValues,
                    String[] attachmentPaths,
                    boolean enableClientSideUnwinding,
                    backtraceio.library.enums.UnwindingMode unwindingMode) {
                return false;
            }
        });

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        this.recordingLogger.assertSanitized("BT_NATIVE_BRIDGE_FAILURE", null);
    }

    @Test
    public void disableFailureWithSentinelMessageIsSanitized() {
        this.database.useNativeDisableAction(() -> {
            throw new UnsatisfiedLinkError("disable failed: " + ALL_SENTINELS);
        });

        this.database.disableNativeIntegration();
        this.recordingLogger.assertSanitized("BT_NATIVE_DISABLE_FAILURE", "java.lang.UnsatisfiedLinkError");
        assertFalse(this.database.addNativeAttribute("key", "value"));
    }
}
