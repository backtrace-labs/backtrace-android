package backtraceio.library.models.json;

import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * @param context              application context
     * @param report               received Backtrace report
     * @param clientAttributes     client's attributes (report and client)
     * @param staticAttributes     pre-initialized static attributes
     * @param includeDynamicAttributes whether to include dynamic attributes
     */
    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object>
            clientAttributes, BacktraceStaticAttributes staticAttributes, Boolean includeDynamicAttributes) {
        this.context = context;
        
        // Start with static attributes
        this.attributes.putAll(staticAttributes.getAttributes());
        
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
        
        // Set session ID and dynamic attributes
        this.attributes.put("application.session", sessionId);
        setDynamicAttributes(includeDynamicAttributes);
    }

    private void setDynamicAttributes(Boolean includeDynamicAttributes) {
        if (includeDynamicAttributes) {
            setDynamicDeviceInformation();
            setDynamicScreenInformation();
        }
    }

    public Map<String, Object> getComplexAttributes() {
        return complexAttributes;
    }

    /**
     * Set dynamic device information (only attributes that can change)
     */
    private void setDynamicDeviceInformation() {
        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(this.context);
        Map<String, String> dynamicAttributes = deviceAttributesHelper.getDeviceAttributes(true);
        this.attributes.putAll(dynamicAttributes);
    }

    /**
     * Set dynamic screen information (only attributes that can change)
     */
    private void setDynamicScreenInformation() {
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
