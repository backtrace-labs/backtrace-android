package backtraceio.library.common;

import static android.content.Context.ACTIVITY_SERVICE;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import backtraceio.library.enums.BatteryState;
import backtraceio.library.enums.BluetoothStatus;
import backtraceio.library.enums.GpsStatus;
import backtraceio.library.enums.LocationStatus;
import backtraceio.library.enums.NfcStatus;
import backtraceio.library.enums.WifiStatus;
import backtraceio.library.logger.BacktraceLogger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 * Helper class for extract a device attributes
 */
public class DeviceAttributesHelper {
    private final Context context;
    private final transient String LOG_TAG = DeviceAttributesHelper.class.getSimpleName();

    public DeviceAttributesHelper(Context context) {
        this.context = context;
    }

    /**
     * Get dynamic attributes about device such as GPS status, Bluetooth status, NFC status
     *
     * @return dynamic device attributes (only when includeDynamicAttributes is true)
     */
    public HashMap<String, String> getDeviceAttributes(Boolean includeDynamicAttributes) {
        HashMap<String, String> result = new HashMap<>();
        if (includeDynamicAttributes == false) {
            return result;
        }
        result.put("device.airplane_mode", String.valueOf(isAirplaneModeOn()));
        result.put("device.location", getLocationServiceStatus().toString());
        result.put("device.nfc.status", getNfcStatus().toString());
        result.put("device.gps.enabled", getGpsStatus().toString());
        result.put("device.bluetooth_status", isBluetoothEnabled().toString());
        result.put("device.cpu.temperature", String.valueOf(getCpuTemperature()));
        result.put("device.is_power_saving_mode", String.valueOf(isPowerSavingMode()));
        result.put("device.wifi.status", getWifiStatus().toString());
        result.put("app.storage_used", getAppUsedStorageSize());
        result.put("battery.level", String.valueOf(getBatteryLevel()));
        result.put("battery.state", getBatteryState().toString());
        result.put(
                "cpu.boottime",
                String.valueOf(java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()));

        ActivityManager.MemoryInfo memoryInfo = getMemoryInformation();
        result.put("system.memory.total", Long.toString(memoryInfo.totalMem));
        result.put("system.memory.free", Long.toString(memoryInfo.availMem));
        result.put("system.memory.active", Long.toString(memoryInfo.totalMem - memoryInfo.availMem));
        return result;
    }

    /**
     * Gets the state of Airplane Mode
     *
     * @return true if enabled.
     */
    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Check is location service is enabled
     *
     * @return location status (enabled/disabled)
     */
    private LocationStatus getLocationServiceStatus() {
        int mode = Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        if (mode != android.provider.Settings.Secure.LOCATION_MODE_OFF) {
            return LocationStatus.ENABLED;
        }
        return LocationStatus.DISABLED;
    }

    /**
     * Check is nfc available and enabled
     *
     * @return NFC status (not available, disabled, enabled)
     */
    private NfcStatus getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.context);

        if (nfcAdapter == null) {
            return NfcStatus.NOT_AVAILABLE;
        } else if (!nfcAdapter.isEnabled()) {
            // NFC is available for device but not enabled
            return NfcStatus.DISABLED;
        }
        return NfcStatus.ENABLED;
    }

    /**
     * Check is bluetooth permitted and enabled
     *
     * @return Bluetooth status (not permitted, disabled, enabled)
     */
    @SuppressLint("MissingPermission")
    private BluetoothStatus isBluetoothEnabled() {

        if (!PermissionHelper.isPermissionForBluetoothGranted(this.context)) {
            return BluetoothStatus.NOT_PERMITTED;
        }
        BluetoothManager mBluetoothManager =
                (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                return BluetoothStatus.ENABLED;
            }
        }
        return BluetoothStatus.DISABLED;
    }

    /**
     * Get device CPU temperature
     *
     * @return measured temperature value in degrees Celsius
     */
    private float getCpuTemperature() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            String line = reader.readLine();
            if (line == null) {
                return 0.0f;
            }
            reader.close();

            return Float.parseFloat(line) / 1000.0f;
        } catch (Exception e) {
            return 0.0f;
        }
    }

    /**
     * Get GPS status
     *
     * @return GPS status (enabled/disabled)
     */
    private GpsStatus getGpsStatus() {
        LocationManager manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? GpsStatus.ENABLED : GpsStatus.DISABLED;
    }

    /**
     * Check Wifi status ('enabled', 'disabled', 'not permitted' to get wifi status)
     * Requires permission.ACCESS_WIFI_STATE
     *
     * @return Wifi status
     */
    private WifiStatus getWifiStatus() {
        if (!PermissionHelper.isPermissionForAccessWifiStateGranted(this.context)) {
            return WifiStatus.NOT_PERMITTED;
        }

        WifiManager mng = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mng.isWifiEnabled()) {
            return WifiStatus.ENABLED;
        }
        return WifiStatus.DISABLED;
    }

    /**
     * Check is power saving mode activated
     *
     * @return is power saving mode activated
     */
    private boolean isPowerSavingMode() {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }
        PowerManager powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }

    /**
     * Get a battery level in float value (from 0.0 to 1.0) or -1 if error occurs
     *
     * @return battery level from 0.0 to 1.0 or -1 if error occurs
     */
    private float getBatteryLevel() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.context.registerReceiver(null, intentFilter);
        if (batteryStatus == null) {
            return -1.0f;
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float) scale;
    }

    /**
     * Get battery state
     *
     * @return battery state (full, charging, unplaggeed, unknown)
     */
    private BatteryState getBatteryState() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);

        if (batteryStatus == null) {
            return BatteryState.UNKNOWN;
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        switch (status) {
            case BatteryManager.BATTERY_STATUS_FULL:
                return BatteryState.FULL;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return BatteryState.CHARGING;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return BatteryState.UNPLUGGED;
            default:
                return BatteryState.UNKNOWN;
        }
    }

    private ActivityManager.MemoryInfo getMemoryInformation() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) this.context.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memInfo);
        return memInfo;
    }

    private String getAppUsedStorageSize() {
        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "Error during getting app used storage size", e);
        }
        return Long.toString(usedSize);
    }
}
