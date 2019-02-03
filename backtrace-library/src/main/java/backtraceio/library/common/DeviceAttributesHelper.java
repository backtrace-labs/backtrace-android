package backtraceio.library.common;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;

import backtraceio.library.enums.BluetoothStatus;
import backtraceio.library.enums.NfcStatus;
import backtraceio.library.enums.WifiStatus;

import static android.content.Context.ACTIVITY_SERVICE;

public class DeviceAttributesHelper {
    private Context context;

    public DeviceAttributesHelper(Context context) {
        this.context = context;
    }

    /**
     * Get attributes about device such as GPS status, Bluetooth status, NFC status
     * @return device attributes
     */
    public HashMap<String, Object> getDeviceAttributes() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("device.airplane_mode", isAirplaneModeOn());
        result.put("device.location_enabled", isLocationServicesEnabled());
        result.put("device.nfc_status", getNfcStatus().toString());
        result.put("device.gps_enabled", isGpsEnabled());
        result.put("device.bluetooth_status", isBluetoothEnabled().toString());
        result.put("device.cpu_temperature", getCpuTemperature());
        result.put("device.is_power_saving_mode", isPowerSavingMode());
        result.put("device.wifi_status", getWifiStatus().toString());
        result.put("device.ram_max", getMaxRamSize());
        result.put("device.ram_free", getDeviceFreeRam());
        result.put("device.ram_%_available", getDeviceRamPercentageAvailable());
        result.put("app.memory_used", getAppUsedMemorySize());
        result.put("guid", this.generateDeviceId());
        return result;
    }

    /**
     * Gets the state of Airplane Mode
     * @return true if enabled.
     */
    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Check is location service is enabled
     * @return true if location service is enabled
     */
    private boolean isLocationServicesEnabled() {
        int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        return (mode != android.provider.Settings.Secure.LOCATION_MODE_OFF);
    }

    /**
     * Check is nfc available and enabled
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
     * @return Bluetooth status (not permitted, disabled, enabled)
     */
    @SuppressLint("MissingPermission")
    private BluetoothStatus isBluetoothEnabled() {

        if(!PermissionHelper.isPermissionForBluetoothGranted(this.context))
        {
            return BluetoothStatus.NOT_PERMITTED;
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isEnabled())
        {
            return BluetoothStatus.ENABLED;
        }
        return BluetoothStatus.DISABLED;
    }

    /**
     * Get device CPU temperature
     * @return measured temperature value in degrees Celsius
     */
    private float getCpuTemperature() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine();
            return Float.parseFloat(line) / 1000.0f;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    /**
     * Check is GPS enabled
     * @return true if GPS is enabled
     */
    private boolean isGpsEnabled() {
        LocationManager manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Check Wifi status ('enabled', 'disabled', 'not permitted' to get wifi status)
     * Requires permission.ACCESS_WIFI_STATE
     * @return Wifi status
     */
    private WifiStatus getWifiStatus() {
        if (!PermissionHelper.isPermissionForAccessWifiStateGranted(this.context))
        {
            return WifiStatus.NOT_PERMITTED;
        }

        WifiManager mng = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(mng.isWifiEnabled())
        {
            return WifiStatus.ENABLED;
        }
        return WifiStatus.DISABLED;
    }

    /**
     * Check is power saving mode activated
     * @return is power saving mode activated
     */
    // TODO: replace bool to enum
    private boolean isPowerSavingMode() {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }
        PowerManager powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }

    /**
     * Generate unique identifier to unambiguously identify the device
     * @return unique device identifier
     */
    private String generateDeviceId(){
        String androidId = Settings.Secure.getString(this.context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        if(TextUtils.isEmpty(androidId))
        {
            return null;
        }

        return UUID.nameUUIDFromBytes(androidId.getBytes()).toString();
    }

    /**
     * Get RAM size of current device
     * available from API 16
     * @return device RAM size
     */
    private String getMaxRamSize()
    {
        return Long.toString(getMemoryInformation().totalMem);
    }

    private String getDeviceFreeRam(){
        return Double.toString(getMemoryInformation().availMem);
    }

    private String getDeviceRamPercentageAvailable(){
        ActivityManager.MemoryInfo mi = getMemoryInformation();
        double percentageAvailable = mi.availMem / (double)mi.totalMem * 100.0;
        return String.format("%.2f", percentageAvailable) + "%";
    }

    private ActivityManager.MemoryInfo getMemoryInformation(){
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) this.context.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memInfo);
        return memInfo;
    }

    private String getAppUsedMemorySize(){
        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Long.toString(usedSize);
    }
}
