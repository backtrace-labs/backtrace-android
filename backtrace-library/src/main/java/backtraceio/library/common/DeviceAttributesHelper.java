package backtraceio.library.common;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import backtraceio.library.enums.BluetoothStatus;
import backtraceio.library.enums.NfcStatus;

public class DeviceAttributesHelper {
    private Context context;
    private PermissionHelper permissionHelper;

    public DeviceAttributesHelper(Context context) {
        this.context = context;
        permissionHelper = new PermissionHelper(this.context);
        getDeviceAttributes();
    }

    private void getDeviceAttributes() {
        boolean airplane = isAirplaneModeOn();
        boolean location = isLocationServicesEnabled();
        String nfc = getNfcStatus().toString();
        boolean gps = isGpsEnabled();
        String bluetooth = isBluetoothEnabled().toString();
        float cpuTemp = getCpuTemperature();
//        String cpuDetails = getCPUDetails();
        boolean powerSavingMode = isPowerSavingMode();
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

        if(!permissionHelper.isPermissionForBluetoothGranted())
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

    private boolean isWifiEnabled() {
        WifiManager mng = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return mng.isWifiEnabled();
    }

    /**
     * Check is power saving mode activated
     * @return TODO:
     */
    // TODO: replace
    private boolean isPowerSavingMode() {
        if (Build.VERSION.SDK_INT < 21) {
            return true;
        }
        PowerManager powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }

    // TODO: remove
//    public static String getCPUDetails(){
//        ProcessBuilder processBuilder;
//        String cpuDetails = "";
//        String[] DATA = {"/system/bin/cat", "/proc/cpuinfo"};
//        InputStream is;
//        Process process ;
//        byte[] bArray ;
//        bArray = new byte[1024];
//
//        try{
//            processBuilder = new ProcessBuilder(DATA);
//
//            process = processBuilder.start();
//
//            is = process.getInputStream();
//
//            while(is.read(bArray) != -1){
//                cpuDetails = cpuDetails + new String(bArray);   //Stroing all the details in cpuDetails
//            }
//            is.close();
//
//        } catch(IOException ex){
//            ex.printStackTrace();
//        }
//
//        return cpuDetails;
//    }
}
