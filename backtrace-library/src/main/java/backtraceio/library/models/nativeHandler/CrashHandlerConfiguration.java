package backtraceio.library.models.nativeHandler;

import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import backtraceio.library.common.AbiHelper;
import backtraceio.library.services.BacktraceCrashHandlerRunner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CrashHandlerConfiguration {

    public static final String BACKTRACE_CRASH_HANDLER = "BACKTRACE_CRASH_HANDLER";
    public static final Set<String> UNSUPPORTED_ABIS = new HashSet<String>(Arrays.asList(new String[] {"x86"}));
    private static final String CRASHPAD_DIRECTORY_PATH = "/crashpad";

    private static final String BACKTRACE_NATIVE_LIBRARY_NAME = "libbacktrace-native.so";

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
        final String classPathApk = applicationInfo.sourceDir;
        final String nativeLibraryDirPath = applicationInfo.nativeLibraryDir;
        final String arch = AbiHelper.getCurrentAbi();

        final List<String> environmentVariables = new ArrayList<>();

        // system environment variables
        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            environmentVariables.add(String.format("%s=%s", variable.getKey(), variable.getValue()));
        }

        // LD_LIBRARY_PATH
        File nativeLibraryDirectory = new File(nativeLibraryDirPath);
        File allNativeLibrariesDirectory = nativeLibraryDirectory.getParentFile();
        String allPossibleLibrarySearchPaths = TextUtils.join(File.pathSeparator, new String[] {
            nativeLibraryDirPath,
            allNativeLibrariesDirectory.getPath(),
            System.getProperty("java.library.path"),
            "/data/local"
        });

        final String backtraceNativeLibraryPath = resolveBacktraceNativeLibraryPath(applicationInfo, arch);

        environmentVariables.add(String.format("CLASSPATH=%s", classPathApk));
        environmentVariables.add(String.format("%s=%s", BACKTRACE_CRASH_HANDLER, backtraceNativeLibraryPath));
        environmentVariables.add(String.format("LD_LIBRARY_PATH=%s", allPossibleLibrarySearchPaths));
        environmentVariables.add("ANDROID_DATA=/data");

        return environmentVariables;
    }

    /**
     * @deprecated Prefer {@link #getCrashHandlerEnvironmentVariables(android.content.pm.ApplicationInfo)} which correctly resolves split APKs on GooglePlay/AAB installs.
     * This method may be removed in a future release.
     */
    @Deprecated
    public List<String> getCrashHandlerEnvironmentVariables(String apkPath, String nativeLibraryDirPath, String arch) {
        final List<String> environmentVariables = new ArrayList<>();

        // convert available in the system environment variables
        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            environmentVariables.add(String.format("%s=%s", variable.getKey(), variable.getValue()));
        }
        // extend system-specific environment variables, with variables needed to properly run app_process via crashpad
        File nativeLibraryDirectory = new File(nativeLibraryDirPath);

        String backtraceNativeLibraryPath = getBacktraceNativeLibraryPath(nativeLibraryDirPath, apkPath, arch);
        File allNativeLibrariesDirectory = nativeLibraryDirectory.getParentFile();
        String allPossibleLibrarySearchPaths = TextUtils.join(File.pathSeparator, new String[] {
            nativeLibraryDirPath,
            allNativeLibrariesDirectory.getPath(),
            System.getProperty("java.library.path"),
            "/data/local"
        });

        environmentVariables.add(String.format("CLASSPATH=%s", apkPath));
        environmentVariables.add(String.format("%s=%s", BACKTRACE_CRASH_HANDLER, backtraceNativeLibraryPath));
        environmentVariables.add(String.format("LD_LIBRARY_PATH=%s", allPossibleLibrarySearchPaths));
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

    private String getBacktraceNativeLibraryPath(String nativeLibraryDirPath, String apkPath, String arch) {
        String backtraceNativeLibraryPath = String.format("%s/%s", nativeLibraryDirPath, BACKTRACE_NATIVE_LIBRARY_NAME);
        File backtraceNativeLibrary = new File(backtraceNativeLibraryPath);

        // If ndk libraries are already extracted, we shouldn't use libraries from the apk.
        // Otherwise. We need to find a path in the apk to use compressed libraries from there.
        return backtraceNativeLibrary.exists()
                ? backtraceNativeLibraryPath
                : String.format("%s!/lib/%s/%s", apkPath, arch, BACKTRACE_NATIVE_LIBRARY_NAME);
    }

    /**
     * Resolve native lib container:
     *   extracted dir if present,
     *   base.apk if it contains the entry,
     *   first split that contains the entry,
     *   fallback to base.apk path format.
     */
    private String resolveBacktraceNativeLibraryPath(ApplicationInfo appInfo, String arch) {
        final String entry = "lib/" + arch + "/" + BACKTRACE_NATIVE_LIBRARY_NAME;

        // extracted dir if present
        if (appInfo.nativeLibraryDir != null) {
            File extracted = new File(appInfo.nativeLibraryDir, BACKTRACE_NATIVE_LIBRARY_NAME);
            if (extracted.exists()) {
                return extracted.getAbsolutePath();
            }
        }

        // base.apk if it contains the lib
        if (apkContains(appInfo.sourceDir, entry)) {
            return appInfo.sourceDir + "!/" + entry;
        }

        // first split that contains the entry
        if (appInfo.splitSourceDirs != null) {
            for (String split : appInfo.splitSourceDirs) {
                if (apkContains(split, entry)) {
                    return split + "!/" + entry;
                }
            }
        }

        // fallback to base.apk path format
        return appInfo.sourceDir + "!/" + entry;
    }

    private static boolean apkContains(String apkPath, String entry) {
        if (apkPath == null || apkPath.isEmpty()) return false;
        try (ZipFile zf = new ZipFile(apkPath)) {
            ZipEntry ze = zf.getEntry(entry);
            return ze != null;
        } catch (IOException ignored) {
            return false;
        }
    }
}
