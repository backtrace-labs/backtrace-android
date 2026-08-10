package backtraceio.library.models.nativeHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.pm.ApplicationInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.common.AbiHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CrashHandlerConfigurationTest {
    private static final String LIBRARY_NAME = "libbacktrace-native.so";

    @Test
    public void usesExactApkBackedPathReportedByLinker() throws Exception {
        String abi = AbiHelper.getCurrentAbi();
        File root = createTemporaryDirectory("loaded-apk-path");
        File baseApk = createPlainFile(root, "base.apk");
        File splitApk = createPlainFile(root, "split_config." + normalizeAbi(abi) + ".apk");
        String loadedPath = splitApk.getAbsolutePath() + "!/lib/" + abi + "/" + LIBRARY_NAME;

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {splitApk.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> loadedPath);
        String resolvedPath = getEnvironmentValue(
                configuration.getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);

        assertEquals(loadedPath, resolvedPath);
    }

    @Test
    public void usesExactExtractedPathReportedByLinker() throws Exception {
        String abi = AbiHelper.getCurrentAbi();
        File root = createTemporaryDirectory("loaded-file-path");
        File baseApk = createPlainFile(root, "base.apk");
        File extractedLibrary = createPlainFile(root, LIBRARY_NAME);

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(extractedLibrary::getAbsolutePath);

        String resolvedPath = configuration.resolveBacktraceNativeLibraryPath(
                applicationInfo, abi, extractedLibrary.getAbsolutePath());

        assertEquals(extractedLibrary.getAbsolutePath(), resolvedPath);
    }

    @Test
    public void usesExtractedNativeLibraryWhenLinkerMetadataIsUnavailable() throws Exception {
        File root = createTemporaryDirectory("extracted-fallback");
        File baseApk = createPlainFile(root, "base.apk");
        File nativeLibraryDirectory = new File(root, "lib");
        assertTrue(nativeLibraryDirectory.mkdirs());
        File extractedLibrary = createPlainFile(nativeLibraryDirectory, LIBRARY_NAME);

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, nativeLibraryDirectory);
        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);

        assertEquals(
                extractedLibrary.getAbsolutePath(),
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    @Test
    public void selectsAbiSplitByFilenameWithoutOpeningTheApk() throws Exception {
        File root = createTemporaryDirectory("filename-fallback");
        File baseApk = createPlainFile(root, "base.apk");
        File languageSplit = createPlainFile(root, "split_config.en.apk");
        File abiSplit = createPlainFile(root, "split_config.arm64_v8a.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {languageSplit.getAbsolutePath(), abiSplit.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                abiSplit.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    @Test
    public void selectsAbiSplitBySplitNameWhenFilenameIsGeneric() throws Exception {
        File root = createTemporaryDirectory("split-name-fallback");
        File baseApk = createPlainFile(root, "base.apk");
        File genericSplit = createPlainFile(root, "split_7.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {genericSplit.getAbsolutePath()};
        applicationInfo.splitNames = new String[] {"config.arm64_v8a"};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                genericSplit.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    @Test
    public void supportsPublicSplitMetadata() throws Exception {
        File root = createTemporaryDirectory("public-split-fallback");
        File baseApk = createPlainFile(root, "base.apk");
        File abiSplit = createPlainFile(root, "split_config.x86_64.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitPublicSourceDirs = new String[] {abiSplit.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                abiSplit.getAbsolutePath() + "!/lib/x86_64/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "x86_64", null));
    }

    @Test
    public void doesNotTreatX86SplitAsX8664Split() throws Exception {
        File root = createTemporaryDirectory("abi-token-boundary");
        File baseApk = createPlainFile(root, "base.apk");
        File x86Split = createPlainFile(root, "split_config.x86.apk");
        File x8664Split = createPlainFile(root, "split_config.x86_64.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {x86Split.getAbsolutePath(), x8664Split.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                x8664Split.getAbsolutePath() + "!/lib/x86_64/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "x86_64", null));
    }

    @Test
    public void rejectsLoadedApkPathForDifferentAbi() throws Exception {
        File root = createTemporaryDirectory("wrong-loaded-abi");
        File baseApk = createPlainFile(root, "base.apk");
        File armSplit = createPlainFile(root, "split_config.arm64_v8a.apk");
        File x86Split = createPlainFile(root, "split_config.x86_64.apk");
        String wrongLoadedPath = x86Split.getAbsolutePath() + "!/lib/x86_64/" + LIBRARY_NAME;

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {armSplit.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> wrongLoadedPath);
        assertEquals(
                armSplit.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", wrongLoadedPath));
    }

    @Test
    public void ignoresLanguageAndDensitySplitsAndFallsBackToBase() throws Exception {
        File root = createTemporaryDirectory("base-fallback");
        File baseApk = createPlainFile(root, "base.apk");
        File languageSplit = createPlainFile(root, "split_config.fr.apk");
        File densitySplit = createPlainFile(root, "split_config.xxhdpi.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs =
                new String[] {languageSplit.getAbsolutePath(), densitySplit.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                baseApk.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    @Test
    public void handlesNullNativeLibraryDirectory() throws Exception {
        File root = createTemporaryDirectory("null-native-dir");
        File baseApk = createPlainFile(root, "base.apk");
        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        List<String> environment = configuration.getCrashHandlerEnvironmentVariables(applicationInfo);

        String librarySearchPath = getEnvironmentValue(environment, "LD_LIBRARY_PATH");
        assertNotNull(librarySearchPath);
        assertFalse(librarySearchPath.contains("null"));
    }

    private static ApplicationInfo createApplicationInfo(File baseApk, File nativeLibraryDirectory) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.sourceDir = baseApk.getAbsolutePath();
        applicationInfo.publicSourceDir = baseApk.getAbsolutePath();
        applicationInfo.nativeLibraryDir =
                nativeLibraryDirectory == null ? null : nativeLibraryDirectory.getAbsolutePath();
        return applicationInfo;
    }

    private static File createTemporaryDirectory(String name) {
        File cache =
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        File directory = new File(cache, "bt-" + name + "-" + System.nanoTime());
        assertTrue(directory.mkdirs());
        return directory;
    }

    private static File createPlainFile(File directory, String name) throws Exception {
        File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {1, 2, 3, 4});
        }
        assertTrue(file.isFile());
        return file;
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

    private static String normalizeAbi(String abi) {
        return abi.replace('-', '_');
    }
}
