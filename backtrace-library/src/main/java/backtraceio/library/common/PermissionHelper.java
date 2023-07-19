package backtraceio.library.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/***
 * Helper class for checking permissions
 */
public class PermissionHelper {

    /**
     * Check if permission for Bluetooth is granted (permission.BLUETOOTH)
     *
     * @return true if permission is granted
     * For Build versions prior to Android 6.0, permissions are granted during installation
     */
    public static boolean isPermissionForBluetoothGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Check if permission for Internet is granted (permission.INTERNET)
     *
     * @return true if permission is granted
     * For Build versions prior to Android 6.0, permissions are granted during installation
     */
    public static boolean isPermissionForInternetGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Check if permission for Wifi state is granted (permission.ACCESS_WIFI_STATE)
     *
     * @return true if permission is granted
     * For Build versions prior to Android 6.0, permissions are granted during installation
     */
    public static boolean isPermissionForAccessWifiStateGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Check if permission to Read external storage is granted (permission.READ_EXTERNAL_STORAGE)
     *
     * @return true if permission is granted
     * For Build versions prior to Android 6.0, permissions are granted during installation
     */
    public static boolean isPermissionForReadExternalStorageGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
}
