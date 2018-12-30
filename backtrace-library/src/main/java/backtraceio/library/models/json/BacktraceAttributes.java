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
    public Map<String, Object> Attributes = new HashMap<>();

    /// <summary>
    /// Get built-in complex attributes
    /// </summary>
    public Map<String, Object> ComplexAttributes = new HashMap<>();

    private Context context;

    public BacktraceAttributes(Context context, BacktraceReport report, Map<String, Object> clientAttributes) {
        this.context = context;
        if (report != null) {

        }
        setAppInformation();
        setDeviceInformation();
        setScreenInformation();
    }

    private void setDeviceInformation() {
        Attributes.put("device.lang", Locale.getDefault().getDisplayLanguage());
        Attributes.put("device.model", Build.MODEL);
        Attributes.put("device.brand", Build.BRAND);
        Attributes.put("device.product", Build.PRODUCT);
        Attributes.put("device.sdk", Build.VERSION.SDK_INT);
        Attributes.put("device.manufacturer", Build.MANUFACTURER);
        Attributes.put("device.os", System.getProperty("os.version"));
    }

    private void setAppInformation() {
        Attributes.put("app.package.name", this.context.getApplicationContext().getPackageName());
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
        Attributes.put("device.screen.width", metrics.widthPixels);
        Attributes.put("device.screen.height", metrics.heightPixels);
        Attributes.put("device.screen.dpi", metrics.densityDpi);
        Attributes.put("device.screen.orientation", getScreenOrientation());
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
}
