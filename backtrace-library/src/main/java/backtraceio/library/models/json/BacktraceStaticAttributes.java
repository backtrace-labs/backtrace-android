package backtraceio.library.models.json;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.ApplicationMetadataCache;
import backtraceio.library.common.BacktraceStringHelper;

/**
 * Class to hold static attributes that don't change between reports.
 * These attributes are initialized once when the SDK is initialized.
 * This is a singleton since static attributes are the same for all instances.
 */
public class BacktraceStaticAttributes {

    /**
     * Singleton instance
     */
    private static volatile BacktraceStaticAttributes instance;

    /**
     * Cached device UUID
     */
    private static String uuid;

    /**
     * Static attributes that remain constant for all reports
     */
    private final Map<String, String> staticAttributes = new HashMap<>();

    /**
     * Private constructor to enforce singleton pattern
     *
     * @param context application context
     */
    private BacktraceStaticAttributes(Context context) {
        setAppInformation(context);
        setDeviceInformation(context);
        setScreenInformation(context);
    }

    /**
     * Init instance of BacktraceStaticAttributes
     *
     * @param context application context (used only for first initialization)
     */
    public static void init(Context context) {
        synchronized (BacktraceStaticAttributes.class) {
            instance = new BacktraceStaticAttributes(context);
        }
    }

    public static BacktraceStaticAttributes getInstance() {
        return instance;
    }

    /**
     * Set application information (package name, application name, version)
     */
    private void setAppInformation(Context context) {
        ApplicationMetadataCache applicationMetadata = ApplicationMetadataCache.getInstance(context);
        this.staticAttributes.put("application.package", applicationMetadata.getPackageName());
        this.staticAttributes.put("application", applicationMetadata.getApplicationName());
        String version = applicationMetadata.getApplicationVersion();
        if (!backtraceio.library.common.BacktraceStringHelper.isNullOrEmpty(version)) {
            // We want to standardize application.version attribute name
            this.staticAttributes.put("application.version", version);
            // But we keep version attribute name as to not break any customer workflows
            this.staticAttributes.put("version", version);
        }
        this.staticAttributes.put("backtrace.agent", "backtrace-android");
        this.staticAttributes.put("backtrace.version", BacktraceClient.version);
    }

    /**
     * Set device information (model, brand, SDK, manufacturer, OS version, etc.)
     */
    private void setDeviceInformation(Context context) {
        this.staticAttributes.put("guid", this.generateDeviceId(context));
        this.staticAttributes.put("uname.sysname", "Android");
        this.staticAttributes.put("uname.machine", System.getProperty("os.arch"));

        this.staticAttributes.put("uname.version", android.os.Build.VERSION.RELEASE);
        this.staticAttributes.put("culture", Locale.getDefault().getDisplayLanguage());
        this.staticAttributes.put("build.type", backtraceio.library.BuildConfig.DEBUG ? "Debug" : "Release");
        this.staticAttributes.put("device.model", android.os.Build.MODEL);
        this.staticAttributes.put("device.brand", android.os.Build.BRAND);
        this.staticAttributes.put("device.product", android.os.Build.PRODUCT);
        this.staticAttributes.put("device.sdk", String.valueOf(android.os.Build.VERSION.SDK_INT));
        this.staticAttributes.put("device.manufacturer", android.os.Build.MANUFACTURER);
        this.staticAttributes.put("device.os_version", System.getProperty("os.version"));
    }

    /**
     * Generate unique identifier to unambiguously identify the device
     *
     * @param context application context
     * @return unique device identifier
     */
    private String generateDeviceId(Context context) {
        if (!BacktraceStringHelper.isNullOrEmpty(uuid)) {
            return uuid;
        }

        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // if the android id is not defined we want to cache at least guid
        // for the current session
        uuid = TextUtils.isEmpty(androidId)
                ? UUID.randomUUID().toString()
                : UUID.nameUUIDFromBytes(androidId.getBytes()).toString();

        return uuid;
    }

    /**
     * Set screen information (width, height, DPI - these represent device
     * capabilities)
     */
    private void setScreenInformation(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        this.staticAttributes.put("screen.width", String.valueOf(metrics.widthPixels));
        this.staticAttributes.put("screen.height", String.valueOf(metrics.heightPixels));
        this.staticAttributes.put("screen.dpi", String.valueOf(metrics.densityDpi));
    }

    /**
     * Get all static attributes
     *
     * @return map of static attributes
     */
    public Map<String, String> getAttributes() {
        return new HashMap<>(staticAttributes);
    }
}
