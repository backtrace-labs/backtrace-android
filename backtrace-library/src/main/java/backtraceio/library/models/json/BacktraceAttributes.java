package backtraceio.library.models.json;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import backtraceio.library.BuildConfig;
import backtraceio.library.common.TypeHelper;
import backtraceio.library.enums.ScreenOrientation;

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
    private Map<String, Object> complexAttributes = new HashMap<>();

    /**
     * Application context
     */
    private Context context;

    /**
     * Create instance of Backtrace Attribute
     *
     * @param context          application context
     * @param report           received Backtrace report
     * @param clientAttributes client's attributes (report and client)
     */
    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object>
            clientAttributes) {
        this.context = context;
        if (report != null) {
            this.convertReportAttributes(report);
            this.setExceptionAttributes(report);
        }
        if (clientAttributes != null) {
            this.convertClientAttributes(clientAttributes);
        }
        if (report != null && clientAttributes != null) {
            BacktraceReport.concatAttributes(report, clientAttributes);
        }
        setAppInformation();
        setDeviceInformation();
        setScreenInformation();
    }

    public Map<String, Object> getComplexAttributes() {
        return complexAttributes;
    }

    /**
     * Set information about device eg. lang, model, brand, sdk, manufacturer, os version
     */
    private void setDeviceInformation() {
        this.attributes.put("uname.version", Build.VERSION.RELEASE);
        this.attributes.put("culture", Locale.getDefault().getDisplayLanguage());
        this.attributes.put("build.type", BuildConfig.DEBUG ? "Debug" : "Release");
        this.attributes.put("device.model", Build.MODEL);
        this.attributes.put("device.brand", Build.BRAND);
        this.attributes.put("device.product", Build.PRODUCT);
        this.attributes.put("device.sdk", String.valueOf(Build.VERSION.SDK_INT));
        this.attributes.put("device.manufacturer", Build.MANUFACTURER);

        this.attributes.put("device.os_version", System.getProperty("os.version"));
    }

    private void setAppInformation() {
        this.attributes.put("application.package", this.context.getApplicationContext()
                .getPackageName());

        this.attributes.put("application", this.context.getApplicationInfo().loadLabel(this.context
                .getPackageManager()).toString());

        try {
            this.attributes.put("version", this.context.getPackageManager()
                    .getPackageInfo(this.context.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set information about screen such as screen width, height, dpi, orientation
     */
    private void setScreenInformation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        this.attributes.put("screen.width", String.valueOf(metrics.widthPixels));
        this.attributes.put("screen.height", String.valueOf(metrics.heightPixels));
        this.attributes.put("screen.dpi", String.valueOf(metrics.densityDpi));
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
        this.attributes.put("classifier", report.exception.getClass().getName());
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
     * Divide client attributes into primitive and complex attributes and add to this object
     *
     * @param clientAttributes client's attributes
     */
    private void convertClientAttributes(Map<String, Object> clientAttributes) {
        convertAttributes(clientAttributes);
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

    /**
     * Divide custom user attributes into primitive and complex attributes and add to this object
     *
     * @param attributes client's attributes
     */
    private void convertAttributes(Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            Class type = value.getClass();
            if (TypeHelper.isPrimitiveOrPrimitiveWrapperOrString(type)) {
                this.attributes.put(entry.getKey(), value.toString());
            } else {
                this.complexAttributes.put(entry.getKey(), value);
            }
        }
    }
}
