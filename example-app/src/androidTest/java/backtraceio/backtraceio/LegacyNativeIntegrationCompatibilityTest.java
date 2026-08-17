package backtraceio.backtraceio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.BacktraceClient;
import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * API 21/22 verifier and resolver smoke: proves the API-isolated references
 * ({@code ApplicationInfo.splitNames} behind an API 26 nested class, {@code Process.is64Bit()}
 * behind an API 23 nested class) let every native-integration class load and run on the oldest
 * supported runtimes without {@code VerifyError}, {@code NoSuchMethodError},
 * {@code NoSuchFieldError}, or {@code UnsatisfiedLinkError}. Requires no backend credentials, so
 * it runs in secretless lanes. The assertions are meaningful on every API level and the CI matrix
 * pins this class to API 21 and 22 emulators.
 */
@RunWith(AndroidJUnit4.class)
public class LegacyNativeIntegrationCompatibilityTest {

    @Test
    public void nativeIntegrationClassesLoadAndVerify() throws Exception {
        // Explicit class initialization surfaces verifier and linkage errors eagerly.
        for (String className : new String[] {
            "backtraceio.library.common.AbiHelper",
            "backtraceio.library.models.nativeHandler.CrashHandlerConfiguration",
            "backtraceio.library.base.BacktraceBase",
            "backtraceio.library.BacktraceClient",
            "backtraceio.library.BacktraceDatabase",
            "backtraceio.library.services.BacktraceCrashHandlerRunner"
        }) {
            assertNotNull(Class.forName(className, true, getClass().getClassLoader()));
        }
    }

    @Test
    public void abiHelperReportsAnAbiOnEveryApiLevel() {
        String abi = AbiHelper.getCurrentAbi();
        assertNotNull(abi);
        assertFalse(abi.trim().isEmpty());
    }

    @Test
    public void environmentConstructionWorksForTheInstalledApp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();

        List<String> environment = new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(applicationInfo);

        String handlerPath = null;
        String prefix = CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER + "=";
        for (String variable : environment) {
            if (variable.startsWith(prefix)) {
                handlerPath = variable.substring(prefix.length());
            }
        }
        assertNotNull("Resolved handler path must exist for a real installed app", handlerPath);
        assertFalse(handlerPath.trim().isEmpty());
    }

    @Test
    public void knownX86RemainsUnsupported() {
        assertFalse(new CrashHandlerConfiguration().isSupportedAbi("x86"));
    }

    @Test
    public void failedSetupThenBothDumpOverloadsSurvive() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File databaseRoot = new File(context.getFilesDir(), "legacy-safety-" + System.nanoTime());
        assertTrue(databaseRoot.mkdirs());

        // Malformed credentials: a null endpoint NPEs inside the URL getter and must be contained
        // by setupNativeIntegration. The client itself is constructed with valid credentials,
        // since the client constructor eagerly builds the submission URL.
        BacktraceCredentials validCredentials = new BacktraceCredentials("https://test.sp.backtrace.io", "token");
        BacktraceCredentials malformedCredentials = new BacktraceCredentials((String) null, "token");
        BacktraceDatabase database = new BacktraceDatabase(context, databaseRoot.getAbsolutePath());
        BacktraceClient client = new BacktraceClient(context, validCredentials, database);
        database.start();

        assertFalse(Boolean.TRUE.equals(database.setupNativeIntegration(client, malformedCredentials)));

        client.dumpWithoutCrash("legacy-dump-after-failure");
        client.dumpWithoutCrash("legacy-dump-after-failure", true);
    }
}
