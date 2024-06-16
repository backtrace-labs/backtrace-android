package backtraceio.library.models.nativeHandler;

import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import backtraceio.library.common.AbiHelper;
import backtraceio.library.services.BacktraceCrashHandlerRunner;

public class CrashHandlerConfiguration {

    public static final String BACKTRACE_CRASH_HANDLER = "BACKTRACE_CRASH_HANDLER";
    public static final Set<String> UNSUPPORTED_ABIS = new HashSet<String>(Arrays.asList(new String[]{"x86"}));
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
        return getCrashHandlerEnvironmentVariables(applicationInfo.sourceDir, applicationInfo.nativeLibraryDir, AbiHelper.getCurrentAbi());
    }

    public List<String> getCrashHandlerEnvironmentVariables(String apkPath, String nativeLibraryDirPath, String arch) {
        final List<String> environmentVariables = new ArrayList<>();

        // convert available in the system environment variables
        for (Map.Entry<String, String> variable :
                System.getenv().entrySet()) {
            environmentVariables.add(String.format("%s=%s", variable.getKey(), variable.getValue()));
        }
        // extend system-specific environment variables, with variables needed to properly run app_process via crashpad
        File nativeLibraryDirectory = new File(nativeLibraryDirPath);

        String backtraceNativeLibraryPath = getBacktraceNativeLibraryPath(nativeLibraryDirPath, apkPath, arch);
        File allNativeLibrariesDirectory = nativeLibraryDirectory.getParentFile();
        String allPossibleLibrarySearchPaths = TextUtils.join(File.pathSeparator, new String[]{
                nativeLibraryDirPath,
                allNativeLibrariesDirectory.getPath(),
                System.getProperty("java.library.path"),
                "/data/local"});

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
}
