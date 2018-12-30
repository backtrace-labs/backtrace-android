package backtraceio.library.models.json;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BacktraceAttributes {

    /// <summary>
    /// Get built-in primitive attributes
    /// </summary>
    public Map<String, Object> attributes = new HashMap<>();

    /// <summary>
    /// Get built-in complex attributes
    /// </summary>
    public Map<String, Object> complexAttributes = new HashMap<>();

    private Context context;

    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object> clientAttributes) {
        this.context = context;
        if (report != null) {
            this.convertAttributes(report, clientAttributes);
            this.setExceptionAttributes(report);
        }
        setAppInformation();
        setDeviceInformation();
        setScreenInformation();
    }

    private void setDeviceInformation() {
        this.attributes.put("device.lang", Locale.getDefault().getDisplayLanguage());
        this.attributes.put("device.model", Build.MODEL);
        this.attributes.put("device.brand", Build.BRAND);
        this.attributes.put("device.product", Build.PRODUCT);
        this.attributes.put("device.sdk", Build.VERSION.SDK_INT);
        this.attributes.put("device.manufacturer", Build.MANUFACTURER);
        this.attributes.put("device.os", System.getProperty("os.version"));
    }

    private void setAppInformation() {
        this.attributes.put("app.package.name", this.context.getApplicationContext().getPackageName());
        // TODO:
//        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//        int versionNumber = pInfo.versionCode;
//        String versionName = pInfo.versionName;
//
//        Attributes.put("app.version_number", Integer.toString(versionNumber));
//        Attributes.put("app.version_name", versionName);
    }

    private void setScreenInformation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        this.attributes.put("device.screen.width", metrics.widthPixels);
        this.attributes.put("device.screen.height", metrics.heightPixels);
        this.attributes.put("device.screen.dpi", metrics.densityDpi);
        this.attributes.put("device.screen.orientation", getScreenOrientation());
    }

    private void setExceptionAttributes(BacktraceReport report)
    {
        //there is no information to analyse
        if (report == null)
        {
            return;
        }
        if (!report.ExceptionTypeReport)
        {
            this.attributes.put("error.message", report.Message);
            return;
        }
        this.attributes.put("classifier", report.Exception.getClass().getName());
        this.attributes.put("error.message", report.Exception.getMessage());
    }

    private String getScreenOrientation() {
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return "portrait";
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return "landscape";
        }
        return "undefined";
    }

    private void convertAttributes(BacktraceReport report, Map<String, Object> clientAttributes)
    {
        Map<String, Object> attributes = BacktraceReport.concatAttributes(report, clientAttributes);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            Class type = value.getClass();
            if(type.isPrimitive() || value instanceof String || type.isEnum()) {
                this.attributes.put(entry.getKey(), value);
            } else {
                this.complexAttributes.put(entry.getKey(), value);
            }
        }
        // add exception information to Complex attributes.
        if(report.ExceptionTypeReport)
        {
            this.complexAttributes.put("Exception properties", report.Exception);
        }
    }
}
