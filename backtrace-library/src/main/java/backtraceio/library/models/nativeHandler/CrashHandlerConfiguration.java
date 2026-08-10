package backtraceio.library.models.nativeHandler;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import backtraceio.library.base.NativeLibraryLoader;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.services.BacktraceCrashHandlerRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CrashHandlerConfiguration {

    public static final String BACKTRACE_CRASH_HANDLER = "BACKTRACE_CRASH_HANDLER";
    public static final Set<String> UNSUPPORTED_ABIS = new HashSet<String>(Arrays.asList(new String[] {"x86"}));
    private static final String CRASHPAD_DIRECTORY_PATH = "/crashpad";
    private static final String APK_LIBRARY_SEPARATOR = "!/";
    private static final String BACKTRACE_NATIVE_LIBRARY_NAME = "libbacktrace-native.so";

    interface NativeLibraryPathProvider {
        String getLoadedLibraryPath();
    }

    private final NativeLibraryPathProvider nativeLibraryPathProvider;

    public CrashHandlerConfiguration() {
        this(NativeLibraryLoader::getLoadedLibraryPath);
    }

    CrashHandlerConfiguration(NativeLibraryPathProvider nativeLibraryPathProvider) {
        this.nativeLibraryPathProvider = nativeLibraryPathProvider;
    }

    public Boolean isSupportedAbi() {
        return isSupportedAbi(AbiHelper.getCurrentAbi());
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

        final String arch = AbiHelper.getCurrentAbi();
        final String loadedLibraryPath = getLoadedLibraryPath();
        final String backtraceNativeLibraryPath =
                resolveBacktraceNativeLibraryPath(applicationInfo, arch, loadedLibraryPath);

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

    public String useCrashpadDirectory(String databaseDirectory) {
        String databasePath = databaseDirectory + CRASHPAD_DIRECTORY_PATH;
        File crashHandlerDir = new File(databasePath);
        // Create the crashpad directory if it doesn't exist
        if (!crashHandlerDir.exists()) {
            crashHandlerDir.mkdir();
        }
        return databasePath;
    }

    /**
     * Resolves the native library path without reading the base or split APK ZIP central directory.
     *
     * <p>The exact path reported by Android's native linker is authoritative. If linker metadata is
     * unavailable, the resolver uses extracted-library and ABI-split metadata before retaining the
     * historical base APK fallback.
     */
    String resolveBacktraceNativeLibraryPath(ApplicationInfo appInfo, String arch, String loadedLibraryPath) {
        if (appInfo == null) {
            throw new IllegalArgumentException("ApplicationInfo cannot be null");
        }
        if (isNullOrEmpty(arch)) {
            throw new IllegalArgumentException("ABI cannot be null or empty");
        }

        final String entry = getApkLibraryEntry(arch);
        final String validatedLoadedPath = validateLoadedLibraryPath(loadedLibraryPath, entry);
        if (validatedLoadedPath != null) {
            return validatedLoadedPath;
        }

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

    private static String validateLoadedLibraryPath(String loadedLibraryPath, String expectedEntry) {
        if (isNullOrEmpty(loadedLibraryPath)) {
            return null;
        }

        final String path = loadedLibraryPath.trim();
        final int apkSeparatorIndex = path.indexOf(APK_LIBRARY_SEPARATOR);
        if (apkSeparatorIndex >= 0) {
            final String apkPath = path.substring(0, apkSeparatorIndex);
            final String entry = path.substring(apkSeparatorIndex + APK_LIBRARY_SEPARATOR.length());
            if (!expectedEntry.equals(entry)) {
                return null;
            }

            File apkFile = new File(apkPath);
            return apkFile.isAbsolute() && apkFile.isFile() ? path : null;
        }

        File libraryFile = new File(path);
        if (!libraryFile.isAbsolute()
                || !BACKTRACE_NATIVE_LIBRARY_NAME.equals(libraryFile.getName())
                || !libraryFile.isFile()) {
            return null;
        }
        return libraryFile.getAbsolutePath();
    }

    private static String getExtractedLibraryPath(String nativeLibraryDirPath) {
        if (isNullOrEmpty(nativeLibraryDirPath)) {
            return null;
        }

        File extractedLibrary = new File(nativeLibraryDirPath, BACKTRACE_NATIVE_LIBRARY_NAME);
        return extractedLibrary.isFile() ? extractedLibrary.getAbsolutePath() : null;
    }

    private static String findAbiSplitPath(ApplicationInfo appInfo, String arch) {
        final String[] splitNames = getSplitNames(appInfo);

        String privateSplit = findAbiSplitPath(appInfo.splitSourceDirs, splitNames, arch);
        if (privateSplit != null) {
            return privateSplit;
        }
        return findAbiSplitPath(appInfo.splitPublicSourceDirs, splitNames, arch);
    }

    /**
     * Reads {@link ApplicationInfo#splitNames}, which only exists from API 26. The field access is
     * isolated in this method so that it is never executed on older platforms, where resolving it
     * would throw {@link NoSuchFieldError}.
     */
    private static String[] getSplitNames(ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        return appInfo.splitNames;
    }

    private static String findAbiSplitPath(String[] splitPaths, String[] splitNames, String arch) {
        if (splitPaths == null) {
            return null;
        }

        for (int index = 0; index < splitPaths.length; index++) {
            String splitPath = splitPaths[index];
            String splitName = splitNames != null && index < splitNames.length ? splitNames[index] : null;
            if (!matchesAbi(splitPath, splitName, arch)) {
                continue;
            }

            File splitFile = new File(splitPath);
            if (splitFile.isAbsolute() && splitFile.isFile()) {
                return splitFile.getAbsolutePath();
            }
        }
        return null;
    }

    private static boolean matchesAbi(String splitPath, String splitName, String arch) {
        String splitFileName = isNullOrEmpty(splitPath) ? null : new File(splitPath).getName();
        return containsAbiToken(splitFileName, arch) || containsAbiToken(splitName, arch);
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
