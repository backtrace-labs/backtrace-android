package backtraceio.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@code dumpWithoutCrash()} must be a safe no-op whenever native integration is not enabled: the
 * native backend guards on its initialized/disabled state and a null Crashpad client, and the Java
 * wrapper contains linkage failures. A contained setup failure followed by a normal dump call was
 * previously able to dereference a never-created Crashpad client and kill the host process.
 *
 * <p>These tests share the instrumentation process; the guards make the calls safe regardless of
 * whether another test initialized the process-global native state.
 */
@RunWith(AndroidJUnit4.class)
public class BacktraceDumpWithoutCrashSafetyTest {
    private Context context;
    private File databaseDirectory;
    private BacktraceCredentials credentials;

    @Before
    public void setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        this.databaseDirectory = new File(this.context.getFilesDir(), "dump-safety-test-" + System.nanoTime());
        this.databaseDirectory.mkdirs();
        this.credentials = new BacktraceCredentials("https://test.sp.backtrace.io", "1231231231231");
    }

    @Test
    public void dumpWithoutCrashDoesNotCrashWhenSetupResolutionFails() {
        BacktraceDatabase database = new BacktraceDatabase(this.context, this.databaseDirectory.getAbsolutePath());
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);
        database.useCrashHandlerConfiguration(new CrashHandlerConfiguration() {
            @Override
            public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
                throw new IllegalStateException("resolution failed");
            }
        });
        database.start();

        assertEquals(false, database.setupNativeIntegration(client, this.credentials));

        client.dumpWithoutCrash("dump-after-resolution-failure");
        client.dumpWithoutCrash("dump-after-resolution-failure", true);
    }

    @Test
    public void dumpWithoutCrashDoesNotCrashWhenBridgeReturnsFalse() {
        BacktraceDatabase database = new BacktraceDatabase(this.context, this.databaseDirectory.getAbsolutePath());
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);
        database.useNativeCommunication(new backtraceio.library.interfaces.NativeCommunication() {
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
        database.start();

        assertEquals(false, database.setupNativeIntegration(client, this.credentials));

        client.dumpWithoutCrash("dump-after-bridge-false");
    }

    @Test
    public void dumpWithoutCrashIsSafeAfterDisable() {
        BacktraceDatabase database = new BacktraceDatabase(this.context, this.databaseDirectory.getAbsolutePath());
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);
        database.start();

        client.disableNativeIntegration();

        client.dumpWithoutCrash("dump-after-disable");
    }

    @Test
    public void managedReportStillWorksAfterFailedNativeSetupAndDumpAttempt() {
        BacktraceDatabase database = new BacktraceDatabase(this.context, this.databaseDirectory.getAbsolutePath());
        BacktraceClient client = new BacktraceClient(this.context, this.credentials, database);
        database.useCrashHandlerConfiguration(new CrashHandlerConfiguration() {
            @Override
            public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
                throw new IllegalStateException("resolution failed");
            }
        });
        database.start();

        assertEquals(false, database.setupNativeIntegration(client, this.credentials));
        client.dumpWithoutCrash("dump-after-failure");

        BacktraceDatabaseRecord managedRecord =
                database.add(new BacktraceReport("managed-after-dump-attempt"), Collections.emptyMap());
        assertNotNull(managedRecord);
        assertEquals(1, database.count());
    }
}
