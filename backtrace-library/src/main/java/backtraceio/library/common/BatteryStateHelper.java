package backtraceio.library.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;


import java.util.HashMap;

import backtraceio.library.BacktraceDatabase;
import backtraceio.library.interfaces.Database;
import backtraceio.library.logger.BacktraceLogger;

public class BatteryStateHelper extends BroadcastReceiver {
    private static Context context;
    private static BacktraceDatabase database;
    private static boolean enabled;
    private static String batteryHealth, batteryLevel, batteryChargingSource, batteryChargingStatus, batteryTechnology, batteryTemperature;
    private static IntentFilter intentFilter;
    private static BatteryStateHelper batteryStateHelper;
    private static String TAG = "BatteryStateHelper";

    private BatteryStateHelper(Context context, Database database) {
        if (BatteryStateHelper.context == null){
            BatteryStateHelper.context = context;
            if (database != null) {
                BatteryStateHelper.database = (BacktraceDatabase)database;
            }
        }
        if (intentFilter == null) {
            intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        }
    }

    public static BatteryStateHelper getInstance(Context context, Database database) {
        if (batteryStateHelper == null) {
            if (context == null)
                return null;
            batteryStateHelper = new BatteryStateHelper(context, database);
        }
        return  batteryStateHelper;
    }

    public static void setDatabase(Database database) {
        batteryStateHelper.database = (BacktraceDatabase) null;
    }

    public static HashMap<String, String> getValues() {
        HashMap<String, String> result = new HashMap<String, String>();
        if (!enabled) {
            result.put("battery.health", "N/A");
            result.put("battery.level", "N/A");
            result.put("battery.charging.source", "N/A");
            result.put("battery.charging.status", "N/A");
            result.put("battery.technology", "N/A");
            result.put("battery.temperature", "N/A");
            return result;
        }
        result.put("battery.health", batteryHealth);
        result.put("battery.level", batteryLevel);
        result.put("battery.charging.source", batteryChargingStatus);
        result.put("battery.charging.status", batteryChargingSource);
        result.put("battery.technology", batteryTechnology);
        result.put("battery.temperature", batteryTemperature);
        return result;
    }

    public void disable() {
        enabled = false;
        if (getInstance(null, null) != null) {
            context.unregisterReceiver(batteryStateHelper);
        }
    }

    public void enable() {
        if (getInstance(null, null) != null) {
            enabled = true;
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            getBatteryHealth(batteryStatus);
            getBatteryChargeSource(batteryStatus);
            getBatteryChargingStatus(batteryStatus);
            getBatteryLevel(batteryStatus);
            getBatteryTechnology(batteryStatus);
            getBatteryTemp(batteryStatus);
            if (database != null) {
                updateDatabase();
            }
            context.registerReceiver(batteryStateHelper, intentFilter);
        } else {
            BacktraceLogger.d(TAG,"Context not set.  Run getInstance(context, database)");
        }
    }

    private void updateDatabase() {
        if (database != null) {
            database.addAttribute("battery.health", batteryHealth);
            database.addAttribute("battery.level", batteryLevel);
            database.addAttribute("battery.charge.source", batteryChargingSource);
            database.addAttribute("battery.charge.status", batteryChargingStatus);
            database.addAttribute("battery.technology", batteryTechnology);
            database.addAttribute("battery.temperature", batteryTemperature);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);

        if (present) {
            getBatteryHealth(intent);
            getBatteryLevel(intent);
            getBatteryChargeSource(intent);
            getBatteryChargingStatus(intent);
            getBatteryTechnology(intent);
            getBatteryTemp(intent);
        }
        updateDatabase();
    }

    private void getBatteryHealth(Intent intent) {
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        if (health > 0) {
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_COLD:
                    BatteryStateHelper.batteryHealth = "Cold";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    BatteryStateHelper.batteryHealth = "Dead";
                    break;
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    BatteryStateHelper.batteryHealth = "Good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    BatteryStateHelper.batteryHealth = "Over voltage";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    BatteryStateHelper.batteryHealth = "Overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    BatteryStateHelper.batteryHealth = "Unspecified failure";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                default:
                    BatteryStateHelper.batteryHealth = "Unknown";
            }
        } else {
            BatteryStateHelper.batteryHealth = "";
        }
    }

    private void getBatteryLevel(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level == -1 || scale == -1){
            BatteryStateHelper.batteryLevel = "-1";
        } else {
            BatteryStateHelper.batteryLevel = String.valueOf(level / (float)scale);
        }
    }

    private void getBatteryChargeSource(Intent intent) {
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        switch(plugged) {
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                BatteryStateHelper.batteryChargingSource = "Wireless";
                break;

            case BatteryManager.BATTERY_PLUGGED_USB:
                BatteryStateHelper.batteryChargingSource = "USB";
                break;

            case BatteryManager.BATTERY_PLUGGED_AC:
                BatteryStateHelper.batteryChargingSource = "AC";
                break;
            default:
                BatteryStateHelper.batteryChargingSource = "";
                break;
        }
    }

    private void getBatteryChargingStatus(Intent intent) {
        int chargeSource = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        switch (chargeSource) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                BatteryStateHelper.batteryChargingStatus = "Charging";
                break;

            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                BatteryStateHelper.batteryChargingStatus = "Discharging";
                break;

            case BatteryManager.BATTERY_STATUS_FULL:
                BatteryStateHelper.batteryChargingStatus = "Full";
                break;

            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                BatteryStateHelper.batteryChargingStatus = "Unknown";
                break;

            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            default:
                BatteryStateHelper.batteryChargingStatus = "Discharging";
                break;
        }
    }

    private void getBatteryTechnology(Intent intent) {
        if (intent.getExtras() != null) {
            BatteryStateHelper.batteryTechnology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
        }
    }

    private void getBatteryTemp(Intent intent) {
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temp > -1) {
            BatteryStateHelper.batteryTemperature = String.valueOf(temp / 10f);
        }
    }
}