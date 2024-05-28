package backtraceio.library.services;

import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BacktraceCrashHandler {

    public static native boolean handleCrash(String[] args);

    private static final String LOG_TAG = BacktraceCrashHandler.class.getSimpleName();
    private static final String BACKTRACE_CRASH_HANDLER = "BACKTRACE_CRASH_HANDLER";

    public static void main(String[] args) {
        Log.i(LOG_TAG, "Starting Backtrace crash handler code.");
        Map<String, String> environmentVariables = System.getenv();
        if (environmentVariables == null) {
            Log.e(LOG_TAG, "Cannot capture crash dump. Environment variables are undefined");
            return;
        }

        String crashHandlerLibrary = environmentVariables.get(BACKTRACE_CRASH_HANDLER);
        if (crashHandlerLibrary == null) {
            Log.e(LOG_TAG, String.format("Cannot capture crash dump. Cannot find %s environment variable", BACKTRACE_CRASH_HANDLER));
            return;
        }

        System.load(crashHandlerLibrary);
        boolean result = handleCrash(args);
        if (!result) {
            Log.e(LOG_TAG, String.format("Cannot capture crash dump. Invocation parameters: %s", String.join(" ", args)));
            return;
        }

        Log.i(LOG_TAG, "Successfully ran crash handler code.");
    }

    public String getClassPath() {
        return BacktraceCrashHandler.class.getCanonicalName();
    }

    public String[] setCrashHandlerEnvironmentVariables(ApplicationInfo applicationInfo) {
        List<String> environmentVariables = new ArrayList<>();

        for (Map.Entry<String, String> variable :
                System.getenv().entrySet()) {
            environmentVariables.add(variable.getKey() + "=" + variable.getValue());
        }

        environmentVariables.add("CLASSPATH=" + applicationInfo.sourceDir);
        environmentVariables.add(BACKTRACE_CRASH_HANDLER + "=" + applicationInfo.sourceDir + "!/lib/arm64-v8a/libbacktrace-native.so");
        environmentVariables.add("LD_LIBRARY_PATH=" + TextUtils.join(File.pathSeparator, new String[]{
                applicationInfo.nativeLibraryDir,
                new File(applicationInfo.nativeLibraryDir).getParentFile().getPath(),
                System.getProperty("java.library.path"),
                "/data/local"}));

        environmentVariables.add("ANDROID_DATA=/data");


        return environmentVariables.toArray(new String[0]);
    }
}
