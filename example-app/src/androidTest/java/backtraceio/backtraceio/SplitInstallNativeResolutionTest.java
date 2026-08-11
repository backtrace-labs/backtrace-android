package backtraceio.backtraceio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.base.NativeLibraryLoader;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Play-style split-install qualification: when this application is installed from a device-specific
 * APK set (bundletool {@code install-apks}) with unextracted native libraries, the production
 * resolver must return the installed ABI configuration split, not a guessed path.
 *
 * <p>The test skips itself on universal-APK or extracted-library installs, so it is safe in every
 * lane and meaningful only in the dedicated AAB split-install job.
 */
@RunWith(AndroidJUnit4.class)
public class SplitInstallNativeResolutionTest {

    @Test
    public void loadedPathUsesInstalledAbiSplit() {
        NativeLibraryLoader.load();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();

        assumeTrue(
                "Not a split install; this test only qualifies bundletool-installed APK sets",
                applicationInfo.splitSourceDirs != null && applicationInfo.splitSourceDirs.length > 0);

        String resolved = getEnvironmentValue(
                new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertNotNull(resolved);

        int separator = resolved.indexOf("!/");
        assumeTrue(
                "Native libraries are extracted on this install; APK-backed resolution not exercised: " + resolved,
                separator > 0);

        String container = resolved.substring(0, separator);
        assertTrue(
                "Resolved container is not an installed split: " + resolved,
                Arrays.asList(applicationInfo.splitSourceDirs).contains(container));
        assertTrue(
                "Resolved entry does not match the process ABI: " + resolved,
                resolved.endsWith("!/lib/" + AbiHelper.getCurrentAbi() + "/libbacktrace-native.so"));
        assertTrue("Resolved container does not exist: " + resolved, new File(container).isFile());
    }

    private static String getEnvironmentValue(List<String> environment, String key) {
        String prefix = key + "=";
        for (String value : environment) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return null;
    }
}
