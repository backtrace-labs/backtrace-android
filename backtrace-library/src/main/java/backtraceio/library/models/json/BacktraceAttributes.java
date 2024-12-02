package backtraceio.library.models.json;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
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
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.enums.ScreenOrientation;
import backtraceio.library.models.attributes.ReportDataAttributes;
import backtraceio.library.models.attributes.ReportDataBuilder;

/**
 * Class instance to get a built-in attributes from current application
 */
public class BacktraceAttributes {
    /**
     * Get built-in primitive attributes
     */
    public Map<String, String> attributes = new HashMap<>();

    /**
     * Get built-in complex attributes
     */
    private final Map<String, Object> complexAttributes = new HashMap<>();

    /**
     * Application context
     */
    private final Context context;

    /**
     * Metrics session ID
     */
    private static final String sessionId = UUID.randomUUID().toString();

    /**
     * Create instance of Backtrace Attribute
     *
     * @param context          application context
     * @param report           received Backtrace report
     * @param clientAttributes client's attributes (report and client)
     */
    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object>
            clientAttributes) {
        this(context, report, clientAttributes, true);
    }

    public BacktraceAttributes(Context context, Map<String, Object> clientAttributes) {
        this(context, null, clientAttributes, false);
    }

    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object>
            clientAttributes, Boolean includeDynamicAttributes) {
        this.context = context;
        if (report != null) {
            this.convertReportAttributes(report);
            this.setExceptionAttributes(report);
        }
        if (clientAttributes != null) {
            this.convertAttributes(clientAttributes);
        }
        if (report != null && clientAttributes != null) {
            BacktraceReport.concatAttributes(report, clientAttributes);
        }
        setAppInformation();
        setDeviceInformation(includeDynamicAttributes);
        setScreenInformation(includeDynamicAttributes);
    }

    public Map<String, Object> getComplexAttributes() {
        return complexAttributes;
    }

    /**
     * Set information about device eg. lang, model, brand, sdk, manufacturer, os version
     */
    private void setDeviceInformation(Boolean includeDynamicAttributes) {
        this.attributes.put("uname.version", Build.VERSION.RELEASE);
        this.attributes.put("culture", Locale.getDefault().getDisplayLanguage());
        this.attributes.put("build.type", backtraceio.library.BuildConfig.DEBUG ? "Debug" : "Release");
        this.attributes.put("device.model", Build.MODEL);
        this.attributes.put("device.brand", Build.BRAND);
        this.attributes.put("device.product", Build.PRODUCT);
        this.attributes.put("device.sdk", String.valueOf(Build.VERSION.SDK_INT));
        this.attributes.put("device.manufacturer", Build.MANUFACTURER);

        this.attributes.put("device.os_version", System.getProperty("os.version"));

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(this.context);
        this.attributes.putAll(deviceAttributesHelper.getDeviceAttributes(includeDynamicAttributes));
    }

    private void setAppInformation() {
        ApplicationMetadataCache applicationMetadata = ApplicationMetadataCache.getInstance(this.context);
        this.attributes.put("application.package", applicationMetadata.getPackageName());
        this.attributes.put("application", applicationMetadata.getApplicationName());
        String version = applicationMetadata.getApplicationVersion();
        if (!BacktraceStringHelper.isNullOrEmpty(version)) {
            // We want to standardize application.version attribute name
            this.attributes.put("application.version", version);
            // But we keep version attribute name as to not break any customer workflows
            this.attributes.put("version", version);
        }
        this.attributes.put("application.session", sessionId);
        this.attributes.put("backtrace.agent", "backtrace-android");
        this.attributes.put("backtrace.version", BacktraceClient.version);
    }

    /**
     * Set information about screen such as screen width, height, dpi, orientation
     */
    private void setScreenInformation(Boolean includeDynamicAttributes) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        this.attributes.put("screen.width", String.valueOf(metrics.widthPixels));
        this.attributes.put("screen.height", String.valueOf(metrics.heightPixels));
        this.attributes.put("screen.dpi", String.valueOf(metrics.densityDpi));
        if (!includeDynamicAttributes) {
            return;
        }
        this.attributes.put("screen.orientation", getScreenOrientation().toString());
        this.attributes.put("screen.brightness", String.valueOf(getScreenBrightness()));
    }

    /**
     * Set information about exception (message and classifier)
     *
     * @param report received report
     */
    private void setExceptionAttributes(BacktraceReport report) {
        //there is no information to analyse
        if (report == null) {
            return;
        }
        if (!report.exceptionTypeReport) {
            this.attributes.put("error.message", report.message);
            return;
        }
        this.attributes.put("classifier", report.classifier);
        this.attributes.put("error.message", report.exception.getMessage());
    }

    /**
     * Get screen orientation
     *
     * @return screen orientation (portrait, landscape, undefined)
     */
    private ScreenOrientation getScreenOrientation() {
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return ScreenOrientation.PORTRAIT;
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return ScreenOrientation.LANDSCAPE;
        }
        return ScreenOrientation.UNDEFINED;
    }

    /**
     * Get screen brightness value
     *
     * @return screen backlight brightness between 0 and 255
     */
    private int getScreenBrightness() {
        return Settings.System.getInt(
                this.context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                0);
    }

    /**
     * Divide report attributes into primitive and complex attributes and add to this object
     *
     * @param report report to extract attributes from
     */
    private void convertReportAttributes(BacktraceReport report) {
        Map<String, Object> attributes = BacktraceReport.concatAttributes(report, null);
        convertAttributes(attributes);
        // add exception information to Complex attributes.
        if (report.exceptionTypeReport) {
            this.complexAttributes.put("Exception properties", report.exception);
        }
    }

    private void convertAttributes(Map<String, Object> clientAttributes) {
        if (clientAttributes == null || clientAttributes.isEmpty()) {
            return;
        }
        ReportDataAttributes data = ReportDataBuilder.getReportAttributes(clientAttributes);
        this.attributes.putAll(data.getAttributes());
        this.complexAttributes.putAll(data.getAnnotations());
    }


    public Map<String, Object> getAllAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(this.attributes);
        attributes.putAll(this.complexAttributes);
        return attributes;
    }
}
