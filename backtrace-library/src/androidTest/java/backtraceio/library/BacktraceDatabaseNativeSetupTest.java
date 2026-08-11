package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.interfaces.NativeCommunication;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.models.nativeHandler.CrashHandlerConfigurationTestFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
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

    private File databaseDirectory;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        this.databaseDirectory = new File(this.context.getFilesDir(), "native-setup-test-" + System.nanoTime());
        assertTrue(this.databaseDirectory.mkdirs());
        this.database = new BacktraceDatabase(this.context, this.databaseDirectory.getAbsolutePath());
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
        // Managed persistence stays operational and native integration stays disabled.
        assertFalse(this.database.addNativeAttribute("key", "value"));
        BacktraceDatabaseRecord managedRecord =
                this.database.add(new BacktraceReport("managed-after-native-setup-failure"), Collections.emptyMap());
        assertNotNull(managedRecord);
        assertEquals(1, this.database.count());
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

    /**
     * A platform API reference that does not resolve on the running OS surfaces as a LinkageError,
     * not a RuntimeException; it must be contained the same way.
     */
    @Test
    public void setupNativeIntegrationReturnsFalseWhenAbiProviderThrowsLinkageError() {
        this.database.useCrashHandlerConfiguration(
                CrashHandlerConfigurationTestFactory.withLinkageErrorAbiProvider(null));

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
        assertFalse(this.database.addNativeAttribute("key", "value"));
    }

    @Test
    public void setupNativeIntegrationUsesValidLinkerPathWhenAbiProviderThrowsLinkageError() throws Exception {
        File loadedLibrary = new File(this.databaseDirectory, "libbacktrace-native.so");
        try (FileOutputStream output = new FileOutputStream(loadedLibrary)) {
            output.write(new byte[] {1, 2, 3, 4});
        }
        this.database.useCrashHandlerConfiguration(
                CrashHandlerConfigurationTestFactory.withLinkageErrorAbiProvider(loadedLibrary.getAbsolutePath()));

        assertEquals(true, this.database.setupNativeIntegration(this.client, this.credentials));
        assertEquals(1, this.nativeCommunication.javaCrashHandlerCalls);
        assertTrue(hasEnvironmentValue(
                this.nativeCommunication.lastEnvironmentVariables,
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER,
                loadedLibrary.getAbsolutePath()));
    }

    @Test
    public void setupNativeIntegrationReturnsFalseForNullClient() {
        assertEquals(false, this.database.setupNativeIntegration(null, this.credentials));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
    }

    @Test
    public void setupNativeIntegrationReturnsFalseForNullCredentials() {
        assertEquals(false, this.database.setupNativeIntegration(this.client, null));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
    }

    /** A submission URL without a JSON marker yields a null minidump URL. */
    @Test
    public void setupNativeIntegrationReturnsFalseForMissingSubmissionUrl() {
        BacktraceCredentials credentialsWithoutMinidumpUrl =
                new BacktraceCredentials("https://submit.backtrace.io/universe/token/dump");

        assertEquals(false, this.database.setupNativeIntegration(this.client, credentialsWithoutMinidumpUrl));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
    }

    @Test
    public void setupNativeIntegrationReturnsFalseWhenCrashpadPathCannotBeCreated() throws Exception {
        // A regular file where the crashpad directory belongs makes directory creation impossible.
        File crashpadObstruction = new File(this.databaseDirectory, "crashpad");
        try (FileOutputStream output = new FileOutputStream(crashpadObstruction)) {
            output.write(new byte[] {1});
        }

        assertEquals(false, this.database.setupNativeIntegration(this.client, this.credentials));
        assertEquals(0, this.nativeCommunication.javaCrashHandlerCalls);
        assertFalse(this.database.addNativeAttribute("key", "value"));
    }

    @Test
    public void nativeCommunicationSeamRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> this.database.useNativeCommunication(null));
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
