package backtraceio.library.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/***
 * Helper class for checking permissions
 */
public class PermissionHelper {

    /**
     * Check is permission for Bluetooth is granted (permission.BLUETOOTH)
     *
     * @return true if permission is granted
     */
    public static boolean isPermissionForBluetoothGranted(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Check is permission for Bluetooth is granted (permission.INTERNET)
     *
     * @return true if permission is granted
     */
    public static boolean isPermissionForInternetGranted(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPermissionForAccessWifiStateGranted(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check is permission for Read external storage grande
     *
     * @return true if permission is granted
     */
    public static boolean isPermissionForReadExternalStorageGranted(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
