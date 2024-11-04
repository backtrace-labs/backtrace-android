package backtraceio.library.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import backtraceio.library.logger.BacktraceLogger;

public class ApplicationHelper {
    private static final transient String LOG_TAG = ApplicationHelper.class.getSimpleName();
    /**
     * Cached application name
     */
    private static String applicationName;

    /**
     * Cached application version
     */
    private static String applicationVersion;

    /**
     * Retrieves application name from context. The name will be cached over checks
     * @param context application context
     * @return application name
     */
    public static String getApplicationName(Context context) {
        if(!BacktraceStringHelper.isNullOrEmpty(applicationName)) {
            return applicationName;
        }

        applicationName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        return  applicationName;
    }

    public static String getApplicationVersion(Context context) {
        if(!BacktraceStringHelper.isNullOrEmpty(applicationVersion)) {
            return applicationVersion;
        }
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            applicationVersion = BacktraceStringHelper.isNullOrEmpty(info.versionName) ? String.valueOf(info.versionCode) : info.versionName;

            return applicationVersion;
        } catch (PackageManager.NameNotFoundException e) {
            BacktraceLogger.e(LOG_TAG, "Could not resolve application version");
            e.printStackTrace();
            return "";
        }
    }
}
