package backtraceio.library.models.nativeHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import backtraceio.library.base.NativeLibraryLoader;
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
        setSplitNames(applicationInfo, new String[] {"config.arm64_v8a"});

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

    /**
     * Android has already resolved which module this process loaded, so the linker path wins even
     * when the device-preferred ABI disagrees — the 32-bit-process-on-64-bit-device case.
     */
    @Test
    public void loadedLinkerPathIsAuthoritativeWhenDevicePreferredAbiDiffers() throws Exception {
        File root = createTemporaryDirectory("process-abi-mismatch");
        File baseApk = createPlainFile(root, "base.apk");
        File arm32Split = createPlainFile(root, "split_config.armeabi_v7a.apk");
        String loadedPath = arm32Split.getAbsolutePath() + "!/lib/armeabi-v7a/" + LIBRARY_NAME;

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> loadedPath);
        assertEquals(
                loadedPath,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo,
                        "arm64-v8a", // Simulates the device-preferred ABI disagreeing.
                        loadedPath));
    }

    @Test
    public void rejectsRelativeOrWrongLibraryLoadedPath() throws Exception {
        File root = createTemporaryDirectory("invalid-loaded-path");
        File baseApk = createPlainFile(root, "base.apk");
        File otherLibrary = createPlainFile(root, "libsomething-else.so");
        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        String baseFallback = baseApk.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME;

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);

        // Relative path.
        assertEquals(
                baseFallback,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo, "arm64-v8a", "lib/arm64-v8a/" + LIBRARY_NAME));
        // Absolute path to a different library.
        assertEquals(
                baseFallback,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo, "arm64-v8a", otherLibrary.getAbsolutePath()));
        // Absolute path that does not exist.
        assertEquals(
                baseFallback,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo, "arm64-v8a", new File(root, LIBRARY_NAME).getAbsolutePath()));
        // APK-backed path whose entry is not the Backtrace library.
        assertEquals(
                baseFallback,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo, "arm64-v8a", baseApk.getAbsolutePath() + "!/lib/arm64-v8a/libother.so"));
        // APK-backed path whose container does not exist.
        assertEquals(
                baseFallback,
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo,
                        "arm64-v8a",
                        new File(root, "absent.apk").getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME));
    }

    @Test
    public void linkerPathProviderFailureUsesMetadataFallback() throws Exception {
        File root = createTemporaryDirectory("provider-failure");
        File baseApk = createPlainFile(root, "base.apk");
        File nativeLibraryDirectory = new File(root, "lib");
        assertTrue(nativeLibraryDirectory.mkdirs());
        File extractedLibrary = createPlainFile(nativeLibraryDirectory, LIBRARY_NAME);

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, nativeLibraryDirectory);

        // Mirrors an UnsatisfiedLinkError from the JNI helper.
        CrashHandlerConfiguration linkageErrorConfiguration = new CrashHandlerConfiguration(() -> {
            throw new UnsatisfiedLinkError("no resolveLoadedLibraryPath");
        });
        String resolvedPath = getEnvironmentValue(
                linkageErrorConfiguration.getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertEquals(extractedLibrary.getAbsolutePath(), resolvedPath);

        // Mirrors an unexpected runtime failure inside the provider.
        CrashHandlerConfiguration runtimeErrorConfiguration = new CrashHandlerConfiguration(() -> {
            throw new IllegalStateException("provider exploded");
        });
        String runtimeResolvedPath = getEnvironmentValue(
                runtimeErrorConfiguration.getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertEquals(extractedLibrary.getAbsolutePath(), runtimeResolvedPath);
    }

    @Test
    public void ignoresNullSplitPathEvenWhenSplitNameMatchesAbi() throws Exception {
        File root = createTemporaryDirectory("null-split-path");
        File baseApk = createPlainFile(root, "base.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {null};
        setSplitNames(applicationInfo, new String[] {"config.arm64_v8a"});

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                baseApk.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    /** The base {@code config.<abi>} split must outrank a dynamic-feature ABI split. */
    @Test
    public void baseConfigAbiSplitIsPreferred() throws Exception {
        File root = createTemporaryDirectory("base-config-preferred");
        File baseApk = createPlainFile(root, "base.apk");
        File featureSplit = createPlainFile(root, "split_feature_video.config.arm64_v8a.apk");
        File baseConfigSplit = createPlainFile(root, "split_config.arm64_v8a.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        // Feature split deliberately listed first so ordering alone would pick the wrong one.
        applicationInfo.splitSourceDirs =
                new String[] {featureSplit.getAbsolutePath(), baseConfigSplit.getAbsolutePath()};

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                baseConfigSplit.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    /**
     * Reproduces the pre-API-26 code path, where {@code splitNames} is unavailable and matching must
     * fall back to the ABI token carried by the split filename.
     */
    @Test
    public void resolvesSplitByFilenameWhenSplitNamesAreUnavailable() throws Exception {
        File root = createTemporaryDirectory("no-split-names");
        File baseApk = createPlainFile(root, "base.apk");
        File abiSplit = createPlainFile(root, "split_config.arm64_v8a.apk");

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);
        applicationInfo.splitSourceDirs = new String[] {abiSplit.getAbsolutePath()};
        // splitNames intentionally left unset, as it is on API 21-25.

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertEquals(
                abiSplit.getAbsolutePath() + "!/lib/arm64-v8a/" + LIBRARY_NAME,
                configuration.resolveBacktraceNativeLibraryPath(applicationInfo, "arm64-v8a", null));
    }

    /**
     * Exercises the real default wiring end to end: the production constructor, the live JNI
     * lookup, and this process's own {@link ApplicationInfo}. The resolved handler path must name a
     * container that actually exists on disk.
     */
    @Test
    public void defaultWiringResolvesRealLoadedLibraryPath() {
        NativeLibraryLoader.load();

        ApplicationInfo applicationInfo =
                InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationInfo();

        String resolvedPath = getEnvironmentValue(
                new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);

        assertNotNull(resolvedPath);
        assertTrue(resolvedPath, resolvedPath.endsWith(LIBRARY_NAME));

        int separatorIndex = resolvedPath.indexOf("!/");
        File container = new File(separatorIndex >= 0 ? resolvedPath.substring(0, separatorIndex) : resolvedPath);
        assertTrue("resolved container does not exist: " + resolvedPath, container.isFile());
    }

    /**
     * Proves the production provider obtained the path from {@code dladdr()} rather than a metadata fallback.
     * Every piece of application metadata is deliberately fake, so the only way the resolver can return an existing container is through the live JNI lookup.
     * A missing JNI symbol or a null {@code dli_fname} would fall through to the fabricated base-APK path and fail the inequality assertion.
     */
    @Test
    public void productionProviderUsesTheLoadedModulePath() throws Exception {
        NativeLibraryLoader.load();

        File root = createTemporaryDirectory("jni-path-proof");
        File fakeBaseApk = createPlainFile(root, "fake-base.apk");

        ApplicationInfo fakeApplicationInfo = new ApplicationInfo();
        fakeApplicationInfo.sourceDir = fakeBaseApk.getAbsolutePath();
        fakeApplicationInfo.publicSourceDir = fakeBaseApk.getAbsolutePath();
        fakeApplicationInfo.nativeLibraryDir = null;
        fakeApplicationInfo.splitSourceDirs = null;
        fakeApplicationInfo.splitPublicSourceDirs = null;

        String resolvedPath = getEnvironmentValue(
                new CrashHandlerConfiguration().getCrashHandlerEnvironmentVariables(fakeApplicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);

        String fallbackPath = fakeBaseApk.getAbsolutePath() + "!/lib/" + AbiHelper.getCurrentAbi() + "/" + LIBRARY_NAME;
        assertNotEquals("Resolver used metadata fallback instead of dladdr", fallbackPath, resolvedPath);

        assertNotNull(resolvedPath);
        assertTrue(resolvedPath, resolvedPath.endsWith(LIBRARY_NAME));

        int separatorIndex = resolvedPath.indexOf("!/");
        File container = new File(separatorIndex >= 0 ? resolvedPath.substring(0, separatorIndex) : resolvedPath);
        assertTrue("Loaded module container does not exist: " + resolvedPath, container.isFile());
    }

    /**
     * The linker path is validated before any process-ABI requirement, so a valid loaded module
     * resolves even when no ABI is available at all.
     */
    @Test
    public void loadedLinkerPathSucceedsWithoutProcessAbi() throws Exception {
        File root = createTemporaryDirectory("no-abi-linker-path");
        File baseApk = createPlainFile(root, "base.apk");
        File loadedLibrary = createPlainFile(root, LIBRARY_NAME);

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(loadedLibrary::getAbsolutePath);
        assertEquals(
                loadedLibrary.getAbsolutePath(),
                configuration.resolveBacktraceNativeLibraryPath(
                        applicationInfo, null, loadedLibrary.getAbsolutePath()));
    }

    /**
     * End-to-end variant: environment construction must succeed on a valid linker path even when
     * the process-ABI provider throws, as it can on malformed vendor builds.
     */
    @Test
    public void loadedLinkerPathSucceedsWhenAbiProviderFails() throws Exception {
        File root = createTemporaryDirectory("failing-abi-provider");
        File baseApk = createPlainFile(root, "base.apk");
        File loadedLibrary = createPlainFile(root, LIBRARY_NAME);

        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(loadedLibrary::getAbsolutePath, () -> {
            throw new IllegalStateException("Unable to determine the current process ABI");
        });

        String resolvedPath = getEnvironmentValue(
                configuration.getCrashHandlerEnvironmentVariables(applicationInfo),
                CrashHandlerConfiguration.BACKTRACE_CRASH_HANDLER);
        assertEquals(loadedLibrary.getAbsolutePath(), resolvedPath);
    }

    /** Metadata fallbacks are the only place a process ABI is required. */
    @Test
    public void metadataFallbackStillRequiresProcessAbi() throws Exception {
        File root = createTemporaryDirectory("metadata-requires-abi");
        File baseApk = createPlainFile(root, "base.apk");
        ApplicationInfo applicationInfo = createApplicationInfo(baseApk, null);

        CrashHandlerConfiguration configuration = new CrashHandlerConfiguration(() -> null);
        assertThrows(
                IllegalArgumentException.class,
                () -> configuration.resolveBacktraceNativeLibraryPath(applicationInfo, null, null));
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

    /**
     * {@link ApplicationInfo#splitNames} exists only from API 26, so tests that need it are skipped below that level rather than failing on a field that is genuinely absent.
     * The field write itself lives in {@link Api26TestImpl}, mirroring the production isolation, so this test class carries no direct {@code splitNames} reference in its bytecode and can load on API 21-25.
     */
    private static void setSplitNames(ApplicationInfo applicationInfo, String[] splitNames) {
        assumeTrue("ApplicationInfo.splitNames requires API 26", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        Api26TestImpl.setSplitNames(applicationInfo, splitNames);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static final class Api26TestImpl {
        private Api26TestImpl() {}

        static void setSplitNames(ApplicationInfo applicationInfo, String[] splitNames) {
            applicationInfo.splitNames = splitNames;
        }
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
