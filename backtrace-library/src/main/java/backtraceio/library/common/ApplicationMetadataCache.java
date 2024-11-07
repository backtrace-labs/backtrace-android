package backtraceio.library.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import backtraceio.library.logger.BacktraceLogger;

public class ApplicationMetadataCache {

    private static final transient String LOG_TAG = ApplicationMetadataCache.class.getSimpleName();

    private static volatile ApplicationMetadataCache instance;

    /**
     * Cached application name
     */
    private String applicationName;

    /**
     * Cached application version
     */
    private String applicationVersion;

    /**
     * Cached package name
     */
    private String packageName;

    /**
     * Returns current application cache. This instance is a singleton since we can only operate
     * in a single application scope.
     *
     * @param context Application context
     * @return Application metadata cache
     */
    public static ApplicationMetadataCache getInstance(Context context) {
        if (instance == null) {
            synchronized (ApplicationMetadataCache.class) {
                if (instance == null) {
                    instance = new ApplicationMetadataCache(context);
                }
            }
        }
        return instance;
    }

    private final Context context;

    public ApplicationMetadataCache(Context context) {
        this.context = context;
    }

    /**
     * Retrieves application name from context. The name will be cached over checks
     *
     * @return application name
     */
    public String getApplicationName() {
        if (!BacktraceStringHelper.isNullOrEmpty(applicationName)) {
            return applicationName;
        }

        applicationName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        return applicationName;
    }

    /**
     * Retrieves application version from the context. If the version name is not defined, the version code will be used instead.
     *
     * @return current application version.
     */
    public String getApplicationVersion() {
        if (!BacktraceStringHelper.isNullOrEmpty(applicationVersion)) {
            return applicationVersion;
        }
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            applicationVersion = BacktraceStringHelper.isNullOrEmpty(info.versionName) ? String.valueOf(info.versionCode) : info.versionName;

            return applicationVersion;
        } catch (PackageManager.NameNotFoundException e) {
            BacktraceLogger.e(LOG_TAG, "Could not resolve application version", e);
            return "";
        }
    }

    /**
     * Retrieves package name from the context.
     *
     * @return current package name.
     */
    public String getPackageName() {
        if (!BacktraceStringHelper.isNullOrEmpty(packageName)) {
            return packageName;
        }
        packageName = context.getApplicationContext().getPackageName();

        return packageName;
    }
}
