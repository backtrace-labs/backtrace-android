package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.interfaces.NativeCommunication;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.models.nativeHandler.CrashHandlerConfigurationTestFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Failure containment for {@link BacktraceDatabase#setupNativeIntegration}: enabling the optional
 * native integration must never throw out of the SDK, and a resolution or bridge failure must
 * disable native integration only.
 */
@RunWith(AndroidJUnit4.class)
public class BacktraceDatabaseNativeSetupTest {
    private Context context;
    private BacktraceDatabase database;
    private BacktraceClient client;
    private BacktraceCredentials credentials;
    private RecordingNativeCommunication nativeCommunication;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getContext();
        this.database =
                new BacktraceDatabase(this.context, this.context.getFilesDir().getAbsolutePath());
        this.credentials = new BacktraceCredentials("https://test.sp.backtrace.io", "1231231231231");
        this.client = new BacktraceClient(this.context, this.credentials);
        this.nativeCommunication = new RecordingNativeCommunication();
        this.database.useNativeCommunication(this.nativeCommunication);
        this.database.start();
        this.database.clear();
    }

    @After
    public void after() {
        this.database.clear();
        this.database.disableNativeIntegration();
    }

    @Test
    public void setupNativeIntegrationReturnsFalseWhenEnvironmentResolutionThrows() {
        this.database.useCrashHandlerConfiguration(new CrashHandlerConfiguration() {
            @Override
            public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
                throw new IllegalStateException("Unable to determine the current process ABI");
            }
        });

        Boolean enabled = this.database.setupNativeIntegration(this.client, this.credentials);

        assertEquals(false, enabled);
        // Managed database behavior stays available and native integration stays disabled.
        assertFalse(this.database.addNativeAttribute("key", "value"));
        assertEquals(0, this.database.count());
    }

    @Test
    public void setupNativeIntegrationDoesNotCallNativeBridgeWhenResolutionThrows() {
        this.database.useCrashHandlerConfiguration(new CrashHandlerConfiguration() {
            @Override
            public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
                throw new IllegalArgumentException("ABI cannot be null or empty");
            }
        });

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
    }

    @Test
    public void setupNativeIntegrationReturnsFalseOnNativeBridgeLinkageError() {
        this.nativeCommunication.linkageErrorToThrow = new UnsatisfiedLinkError("no initializeJavaCrashHandler");

        Boolean enabled = this.database.setupNativeIntegration(this.client, this.credentials);

        assertEquals(false, enabled);
        assertEquals(1, this.nativeCommunication.javaCrashHandlerCalls);
        assertFalse(this.database.addNativeAttribute("key", "value"));
    }

    @Test
    public void setupNativeIntegrationUsesValidLinkerPathWhenAbiProviderThrows() throws Exception {
        File loadedLibrary = new File(this.context.getCacheDir(), "libbacktrace-native.so");
        try (FileOutputStream output = new FileOutputStream(loadedLibrary)) {
            output.write(new byte[] {1, 2, 3, 4});
        }
        this.database.useCrashHandlerConfiguration(
                CrashHandlerConfigurationTestFactory.withThrowingAbiProvider(loadedLibrary.getAbsolutePath()));

        Boolean enabled = this.database.setupNativeIntegration(this.client, this.credentials);

        assertEquals(true, enabled);
        assertEquals(1, this.nativeCommunication.javaCrashHandlerCalls);
        assertNotNull(this.nativeCommunication.lastEnvironmentVariables);
        assertTrue(hasEnvironmentValue(
                this.nativeCommunication.lastEnvironmentVariables,
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER,
                loadedLibrary.getAbsolutePath()));
    }

    @Test
    public void knownX86StillSkipsNativeInitialization() {
        this.database.useCrashHandlerConfiguration(CrashHandlerConfigurationTestFactory.withFixedAbi("x86"));

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
    }

    private static boolean hasEnvironmentValue(String[] environmentVariables, String key, String value) {
        String expected = key + "=" + value;
        for (String variable : environmentVariables) {
            if (expected.equals(variable)) {
                return true;
            }
        }
        return false;
    }

    private static final class RecordingNativeCommunication implements NativeCommunication {
        int javaCrashHandlerCalls;
        String[] lastEnvironmentVariables;
        LinkageError linkageErrorToThrow;

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
            javaCrashHandlerCalls++;
            lastEnvironmentVariables = environmentVariables;
            if (linkageErrorToThrow != null) {
                throw linkageErrorToThrow;
            }
            return true;
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
                UnwindingMode unwindingMode) {
            return false;
        }
    }
}
