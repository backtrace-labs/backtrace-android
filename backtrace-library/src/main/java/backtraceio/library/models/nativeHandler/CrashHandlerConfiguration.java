package backtraceio.library.models.nativeHandler;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.RequiresApi;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.services.BacktraceCrashHandlerRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CrashHandlerConfiguration {

    public static final String BACKTRACE_CRASH_HANDLER = "BACKTRACE_CRASH_HANDLER";
    public static final Set<String> UNSUPPORTED_ABIS = new HashSet<String>(Arrays.asList(new String[] {"x86"}));
    private static final String CRASHPAD_DIRECTORY_NAME = "crashpad";
    private static final String APK_LIBRARY_SEPARATOR = "!/";
    private static final String BACKTRACE_NATIVE_LIBRARY_NAME = "libbacktrace-native.so";

    interface NativeLibraryPathProvider {
        String getLoadedLibraryPath();
    }

    interface AbiProvider {
        String getCurrentAbi();
    }

    private final NativeLibraryPathProvider nativeLibraryPathProvider;
    private final AbiProvider abiProvider;

    public CrashHandlerConfiguration() {
        this(CrashHandlerConfiguration::safelyResolveLoadedLibraryPath, AbiHelper::getCurrentAbi);
    }

    CrashHandlerConfiguration(NativeLibraryPathProvider nativeLibraryPathProvider) {
        this(nativeLibraryPathProvider, AbiHelper::getCurrentAbi);
    }

    CrashHandlerConfiguration(NativeLibraryPathProvider nativeLibraryPathProvider, AbiProvider abiProvider) {
        this.nativeLibraryPathProvider = nativeLibraryPathProvider;
        this.abiProvider = abiProvider;
    }

    public Boolean isSupportedAbi() {
        final String abi;
        try {
            abi = abiProvider.getCurrentAbi();
        } catch (RuntimeException | LinkageError ignored) {
            // The unsupported-ABI policy is separate from path resolution: an undeterminable
            // process ABI must not block a module the linker has already loaded, so an ABI that
            // cannot be determined is treated as not known-unsupported. LinkageError covers
            // platform API references that do not resolve on old runtimes.
            return true;
        }
        return isSupportedAbi(abi);
    }

    public Boolean isSupportedAbi(String abi) {
        return !this.UNSUPPORTED_ABIS.contains(abi);
    }

    public String getClassPath() {
        return BacktraceCrashHandlerRunner.class.getCanonicalName();
    }

    public List<String> getCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            throw new IllegalArgumentException("ApplicationInfo cannot be null");
        }

        final String classPathApk = firstNonEmpty(applicationInfo.sourceDir, applicationInfo.publicSourceDir);
        if (classPathApk == null) {
            throw new IllegalArgumentException("ApplicationInfo does not define an application APK path");
        }

        final String loadedLibraryPath = getLoadedLibraryPath();
        final String backtraceNativeLibraryPath = resolveBacktraceNativeLibraryPath(applicationInfo, loadedLibraryPath);

        final List<String> environmentVariables = copySystemEnvironment();
        environmentVariables.add(String.format("CLASSPATH=%s", classPathApk));
        environmentVariables.add(String.format("%s=%s", BACKTRACE_CRASH_HANDLER, backtraceNativeLibraryPath));
        environmentVariables.add(
                String.format("LD_LIBRARY_PATH=%s", buildNativeLibrarySearchPath(applicationInfo.nativeLibraryDir)));
        environmentVariables.add("ANDROID_DATA=/data");

        return environmentVariables;
    }

    /**
     * @deprecated Prefer {@link #getCrashHandlerEnvironmentVariables(ApplicationInfo)}, which
     *     correctly resolves split APKs on Google Play/AAB installs without opening the APK.
     *     This method may be removed in a future release.
     */
    @Deprecated
    public List<String> getCrashHandlerEnvironmentVariables(String apkPath, String nativeLibraryDirPath, String arch) {
        final List<String> environmentVariables = copySystemEnvironment();
        final String backtraceNativeLibraryPath = getBacktraceNativeLibraryPath(nativeLibraryDirPath, apkPath, arch);

        if (!isNullOrEmpty(apkPath)) {
            environmentVariables.add(String.format("CLASSPATH=%s", apkPath));
        }
        if (!isNullOrEmpty(backtraceNativeLibraryPath)) {
            environmentVariables.add(String.format("%s=%s", BACKTRACE_CRASH_HANDLER, backtraceNativeLibraryPath));
        }
        environmentVariables.add(
                String.format("LD_LIBRARY_PATH=%s", buildNativeLibrarySearchPath(nativeLibraryDirPath)));
        environmentVariables.add("ANDROID_DATA=/data");

        return environmentVariables;
    }

    /**
     * Returns the Crashpad database directory under {@code databaseDirectory}, creating it when
     * necessary. Fails deterministically instead of returning a path that Crashpad cannot use:
     * callers contain the failure and disable native integration only.
     */
    public String useCrashpadDirectory(String databaseDirectory) {
        if (isNullOrEmpty(databaseDirectory)) {
            throw new IllegalArgumentException("Database directory cannot be null or empty");
        }

        File crashpadDirectory = new File(databaseDirectory, CRASHPAD_DIRECTORY_NAME);
        if (crashpadDirectory.exists()) {
            if (!crashpadDirectory.isDirectory()) {
                throw new IllegalStateException("Crashpad path is not a directory: " + crashpadDirectory);
            }
        } else if (!crashpadDirectory.mkdirs() && !crashpadDirectory.isDirectory()) {
            throw new IllegalStateException("Unable to create Crashpad directory: " + crashpadDirectory);
        }
        return crashpadDirectory.getAbsolutePath();
    }

    /**
     * Resolves the native library path without reading the base or split APK ZIP central directory.
     *
     * <p>The exact path reported by Android's native linker is authoritative and is validated
     * before any process-ABI requirement, so a valid loaded module initializes the handler even
     * when ABI metadata is malformed or unavailable. If linker metadata is unavailable, the
     * resolver uses extracted-library and ABI-split metadata before retaining the historical base
     * APK fallback; only those metadata fallbacks require a process ABI.
     */
    String resolveBacktraceNativeLibraryPath(ApplicationInfo appInfo, String loadedLibraryPath) {
        if (appInfo == null) {
            throw new IllegalArgumentException("ApplicationInfo cannot be null");
        }

        final String validatedLoadedPath = validateLoadedLibraryPath(loadedLibraryPath);
        if (validatedLoadedPath != null) {
            return validatedLoadedPath;
        }

        return resolveFromApplicationMetadata(appInfo, abiProvider.getCurrentAbi());
    }

    /**
     * Test seam kept for explicit-ABI scenarios, see:
     * {@link #resolveBacktraceNativeLibraryPath(ApplicationInfo, String)} for ordering semantics.
     */
    String resolveBacktraceNativeLibraryPath(ApplicationInfo appInfo, String arch, String loadedLibraryPath) {
        if (appInfo == null) {
            throw new IllegalArgumentException("ApplicationInfo cannot be null");
        }

        final String validatedLoadedPath = validateLoadedLibraryPath(loadedLibraryPath);
        if (validatedLoadedPath != null) {
            return validatedLoadedPath;
        }

        return resolveFromApplicationMetadata(appInfo, arch);
    }

    private static String resolveFromApplicationMetadata(ApplicationInfo appInfo, String arch) {
        if (isNullOrEmpty(arch)) {
            throw new IllegalArgumentException("ABI cannot be null or empty");
        }

        final String entry = getApkLibraryEntry(arch);
        final String extractedLibraryPath = getExtractedLibraryPath(appInfo.nativeLibraryDir);
        if (extractedLibraryPath != null) {
            return extractedLibraryPath;
        }

        final String splitApkPath = findAbiSplitPath(appInfo, arch);
        if (splitApkPath != null) {
            return splitApkPath + APK_LIBRARY_SEPARATOR + entry;
        }

        final String baseApkPath = firstNonEmpty(appInfo.sourceDir, appInfo.publicSourceDir);
        if (baseApkPath == null) {
            throw new IllegalArgumentException("ApplicationInfo does not define an application APK path");
        }
        return baseApkPath + APK_LIBRARY_SEPARATOR + entry;
    }

    private String getLoadedLibraryPath() {
        if (nativeLibraryPathProvider == null) {
            return null;
        }

        try {
            return nativeLibraryPathProvider.getLoadedLibraryPath();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Validates the linker-reported module path structurally.
     *
     * <p>The path is deliberately <em>not</em> compared against an ABI inferred in Java.
     * Android has already resolved which module this process loaded, so the linker's answer is authoritative;
     * comparing it against a guessed ABI would reject correct paths in 32-bit processes on 64-bit devices and under native-bridge translation.
     */
    private static String validateLoadedLibraryPath(String loadedLibraryPath) {
        if (isNullOrEmpty(loadedLibraryPath)) {
            return null;
        }

        final String path = loadedLibraryPath.trim();
        final int apkSeparatorIndex = path.indexOf(APK_LIBRARY_SEPARATOR);
        if (apkSeparatorIndex >= 0) {
            final String containerPath = path.substring(0, apkSeparatorIndex);
            final String entry = path.substring(apkSeparatorIndex + APK_LIBRARY_SEPARATOR.length());
            if (!isBacktraceApkLibraryEntry(entry)) {
                return null;
            }

            File containerFile = new File(containerPath);
            return containerFile.isAbsolute() && containerFile.isFile() ? path : null;
        }

        File libraryFile = new File(path);
        if (!libraryFile.isAbsolute()
                || !BACKTRACE_NATIVE_LIBRARY_NAME.equals(libraryFile.getName())
                || !libraryFile.isFile()) {
            return null;
        }
        return libraryFile.getAbsolutePath();
    }

    /**
     * Accepts {@code lib/<abi>/libbacktrace-native.so} for any single-segment ABI directory.
     */
    private static boolean isBacktraceApkLibraryEntry(String entry) {
        final String prefix = "lib/";
        final String suffix = "/" + BACKTRACE_NATIVE_LIBRARY_NAME;
        if (entry == null || !entry.startsWith(prefix) || !entry.endsWith(suffix)) {
            return false;
        }

        final int abiEnd = entry.length() - suffix.length();
        if (abiEnd <= prefix.length()) {
            return false;
        }

        final String abi = entry.substring(prefix.length(), abiEnd);
        return abi.indexOf('/') < 0;
    }

    private static String safelyResolveLoadedLibraryPath() {
        try {
            return resolveLoadedLibraryPath();
        } catch (LinkageError | SecurityException ignored) {
            return null;
        }
    }

    /**
     * Returns the filesystem or APK-backed path of the loaded Backtrace native library as reported by the platform linker,
     * or {@code null} when module metadata is unavailable.
     */
    private static native String resolveLoadedLibraryPath();

    private static String getExtractedLibraryPath(String nativeLibraryDirPath) {
        if (isNullOrEmpty(nativeLibraryDirPath)) {
            return null;
        }

        File extractedLibrary = new File(nativeLibraryDirPath, BACKTRACE_NATIVE_LIBRARY_NAME);
        return extractedLibrary.isFile() ? extractedLibrary.getAbsolutePath() : null;
    }

    /**
     * Selects the ABI split across {@code splitSourceDirs} and {@code splitPublicSourceDirs}
     * together, deduplicated by path and compared globally by match confidence, so a loose match in
     * one array can never outrank an exact base configuration split in the other. Two distinct
     * candidates of equal confidence cannot be told apart without opening the archives, so an
     * ambiguous result returns {@code null} and resolution falls through to the historical base-APK
     * fallback instead of picking by array order.
     */
    private static String findAbiSplitPath(ApplicationInfo appInfo, String arch) {
        final String[] splitNames = getSplitNames(appInfo);
        // Loose filename-token matching represents installs without split-name metadata
        // (pre-API-26). It is decided globally: when any split-name metadata exists, a candidate
        // without an aligned name (for example one beyond a truncated splitNames array) must not
        // regain the low-confidence score that named candidates correctly lose.
        final boolean allowLooseFilenameMatching = splitNames == null;

        final Map<String, Integer> candidates = new LinkedHashMap<>();
        collectAbiSplitCandidates(candidates, appInfo.splitSourceDirs, splitNames, arch, allowLooseFilenameMatching);
        collectAbiSplitCandidates(
                candidates, appInfo.splitPublicSourceDirs, splitNames, arch, allowLooseFilenameMatching);

        String bestPath = null;
        int bestScore = 0;
        boolean ambiguous = false;
        for (Map.Entry<String, Integer> candidate : candidates.entrySet()) {
            int score = candidate.getValue();
            if (score > bestScore) {
                bestPath = candidate.getKey();
                bestScore = score;
                ambiguous = false;
            } else if (score == bestScore && score > 0) {
                ambiguous = true;
            }
        }
        return ambiguous ? null : bestPath;
    }

    private static void collectAbiSplitCandidates(
            Map<String, Integer> candidates,
            String[] splitPaths,
            String[] splitNames,
            String arch,
            boolean allowLooseFilenameMatching) {
        if (splitPaths == null) {
            return;
        }

        for (int index = 0; index < splitPaths.length; index++) {
            String splitPath = splitPaths[index];
            if (isNullOrEmpty(splitPath)) {
                continue;
            }

            String splitName = splitNames != null && index < splitNames.length ? splitNames[index] : null;
            int score = getAbiMatchScore(splitPath, splitName, arch, allowLooseFilenameMatching);
            if (score <= 0) {
                continue;
            }

            File splitFile = new File(splitPath);
            if (!splitFile.isAbsolute() || !splitFile.isFile()) {
                continue;
            }

            String candidatePath = splitFile.getAbsolutePath();
            Integer existingScore = candidates.get(candidatePath);
            if (existingScore == null || existingScore < score) {
                candidates.put(candidatePath, score);
            }
        }
    }

    /**
     * Reads {@link ApplicationInfo#splitNames}, which only exists from API 26.
     * The field access itself lives in {@link Api26Impl} so the reference is confined to a class that is never loaded or verified on older platforms,
     * where resolving it would throw {@link NoSuchFieldError}.
     */
    private static String[] getSplitNames(ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        return Api26Impl.getSplitNames(appInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static final class Api26Impl {
        private Api26Impl() {}

        static String[] getSplitNames(ApplicationInfo appInfo) {
            return appInfo.splitNames;
        }
    }

    /**
     * Match confidence for one split candidate. An exact {@code config.<abi>} split name is the
     * strongest evidence, then the standard {@code split_config.<abi>.apk} filename. A loose ABI
     * token in the filename is accepted only when split-name metadata is globally unavailable
     * (pre-API-26 installs): a candidate that has a split name must match exactly or not at all,
     * so a dynamic-feature ABI split is never mistaken for the base configuration split.
     */
    private static int getAbiMatchScore(
            String splitPath, String splitName, String arch, boolean allowLooseFilenameMatching) {
        if (isNullOrEmpty(arch)) {
            return 0;
        }

        String normalizedArch = normalizeAbiToken(arch);
        String normalizedSplitName = isNullOrEmpty(splitName) ? null : normalizeAbiToken(splitName);
        String normalizedFileName = normalizeAbiToken(new File(splitPath).getName());

        if (("config." + normalizedArch).equals(normalizedSplitName)) {
            return 300;
        }
        if (("split_config." + normalizedArch + ".apk").equals(normalizedFileName)) {
            return 200;
        }
        if (allowLooseFilenameMatching && normalizedSplitName == null && containsAbiToken(normalizedFileName, arch)) {
            return 100;
        }
        return 0;
    }

    private static boolean containsAbiToken(String value, String arch) {
        if (isNullOrEmpty(value) || isNullOrEmpty(arch)) {
            return false;
        }

        String normalizedValue = normalizeAbiToken(value);
        String normalizedArch = normalizeAbiToken(arch);
        int startIndex = 0;
        while (startIndex < normalizedValue.length()) {
            int matchIndex = normalizedValue.indexOf(normalizedArch, startIndex);
            if (matchIndex < 0) {
                return false;
            }

            int endIndex = matchIndex + normalizedArch.length();
            boolean validStart = matchIndex == 0 || !isAbiTokenCharacter(normalizedValue.charAt(matchIndex - 1));
            boolean validEnd =
                    endIndex == normalizedValue.length() || !isAbiTokenCharacter(normalizedValue.charAt(endIndex));
            if (validStart && validEnd) {
                return true;
            }
            startIndex = matchIndex + 1;
        }
        return false;
    }

    private static boolean isAbiTokenCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private static String normalizeAbiToken(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String getApkLibraryEntry(String arch) {
        return "lib/" + arch + "/" + BACKTRACE_NATIVE_LIBRARY_NAME;
    }

    private static String getBacktraceNativeLibraryPath(String nativeLibraryDirPath, String apkPath, String arch) {
        String extractedLibraryPath = getExtractedLibraryPath(nativeLibraryDirPath);
        if (extractedLibraryPath != null) {
            return extractedLibraryPath;
        }
        if (isNullOrEmpty(apkPath) || isNullOrEmpty(arch)) {
            return null;
        }
        return apkPath + APK_LIBRARY_SEPARATOR + getApkLibraryEntry(arch);
    }

    private static List<String> copySystemEnvironment() {
        final List<String> environmentVariables = new ArrayList<>();
        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            environmentVariables.add(String.format("%s=%s", variable.getKey(), variable.getValue()));
        }
        return environmentVariables;
    }

    private static String buildNativeLibrarySearchPath(String nativeLibraryDirPath) {
        LinkedHashSet<String> searchPaths = new LinkedHashSet<>();
        addSearchPath(searchPaths, nativeLibraryDirPath);

        if (!isNullOrEmpty(nativeLibraryDirPath)) {
            File nativeLibraryDirectory = new File(nativeLibraryDirPath);
            File allNativeLibrariesDirectory = nativeLibraryDirectory.getParentFile();
            if (allNativeLibrariesDirectory != null) {
                addSearchPath(searchPaths, allNativeLibrariesDirectory.getPath());
            }
        }

        addSearchPath(searchPaths, System.getProperty("java.library.path"));
        addSearchPath(searchPaths, "/data/local");
        return TextUtils.join(File.pathSeparator, searchPaths);
    }

    private static void addSearchPath(Set<String> searchPaths, String path) {
        if (!isNullOrEmpty(path)) {
            searchPaths.add(path);
        }
    }

    private static String firstNonEmpty(String first, String second) {
        return !isNullOrEmpty(first) ? first : (!isNullOrEmpty(second) ? second : null);
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
