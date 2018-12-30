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

public class DeviceAttributesHelper {
    private Context context;
    private PermissionHelper permissionHelper;

    public DeviceAttributesHelper(Context context) {
        this.context = context;
        permissionHelper = new PermissionHelper(this.context);
        getDeviceAttributes();
    }

    public void getDeviceAttributes() {
        boolean airplane = isAirplaneModeOn();
        boolean location = isLocationServicesEnabled();
        String nfc = getNfcStatus();
        boolean gps = isGpsEnabled();
        String bluetooth = isBluetoothEnabled();
        float cpuTemp = getCpuTemperature();
//        String cpuDetails = getCPUDetails();
        boolean powerSavingMode = isPowerSavingMode();
    }

    /**
     * Gets the state of Airplane Mode.
     *
     * @return true if enabled.
     */
    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private boolean isLocationServicesEnabled() {
        int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        final boolean enabled = (mode != android.provider.Settings.Secure.LOCATION_MODE_OFF);
        return enabled;
    }

    private String getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this.context);

        if (nfcAdapter == null) {
            return "not available";
        } else if (!nfcAdapter.isEnabled()) {
            // NFC is available for device but not enabled
            return "disabled";
        }
        return "enabled";
    }

    @SuppressLint("MissingPermission")
    public String isBluetoothEnabled() {

        if(!permissionHelper.isPermissionForBluetoothGranted())
        {
            return "not permitted";
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter.isEnabled())
        {
            return "enabled";
        }
        return "disabled";
    }

    public float getCpuTemperature() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine();
            float temp = Float.parseFloat(line) / 1000.0f;

            return temp;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    private boolean isGpsEnabled() {
        LocationManager manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isWifiEnabled() {
        WifiManager mng = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return mng.isWifiEnabled();
    }

    private boolean isPowerSavingMode() {
        if (Build.VERSION.SDK_INT < 21) {
            return true;
        }
        PowerManager powerManager = (PowerManager) this.context.getSystemService(this.context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }
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
