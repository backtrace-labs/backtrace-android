package backtraceio.library.breadcrumbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import backtraceio.library.models.types.BacktraceBreadcrumbType;

public class BacktraceBroadcastReceiver extends BroadcastReceiver {

    private BacktraceBreadcrumbs backtraceBreadcrumbs;

    public BacktraceBroadcastReceiver(@NonNull BacktraceBreadcrumbs backtraceBreadcrumbs)
    {
        this.backtraceBreadcrumbs = backtraceBreadcrumbs;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
        {
            action = "Null action received. This is a bug";
        }

        Map<String, Object> attributes = null;
        Bundle extras = intent.getExtras();

        if (extras != null) {
            Set<String> keys = extras.keySet();
            if (keys != null) {
                attributes = new HashMap<String, Object>();
                for (String key : keys) {
                    attributes.put(key, extras.get(key));
                }
            }
        }

        backtraceBreadcrumbs.addBreadcrumb(action, attributes, BacktraceBreadcrumbType.SYSTEM);
    }

    public IntentFilter getMyIntentFilter() {
        List<String> defaultSystemEventsToCapture = new ArrayList<String>();
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_DELETED");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_DISABLED");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_ENABLED");
        defaultSystemEventsToCapture.add("android.intent.action.CAMERA_BUTTON");
        defaultSystemEventsToCapture.add("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        defaultSystemEventsToCapture.add("android.intent.action.DOCK_EVENT");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_HOST_RESTORED");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_RESTORED");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_UPDATE");
        defaultSystemEventsToCapture.add("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
        defaultSystemEventsToCapture.add("android.intent.action.ACTION_POWER_CONNECTED");
        defaultSystemEventsToCapture.add("android.intent.action.ACTION_POWER_DISCONNECTED");
        defaultSystemEventsToCapture.add("android.intent.action.ACTION_SHUTDOWN");
        defaultSystemEventsToCapture.add("android.intent.action.AIRPLANE_MODE");
        defaultSystemEventsToCapture.add("android.intent.action.BATTERY_LOW");
        defaultSystemEventsToCapture.add("android.intent.action.BATTERY_OKAY");
        defaultSystemEventsToCapture.add("android.intent.action.BOOT_COMPLETED");
        defaultSystemEventsToCapture.add("android.intent.action.CONTENT_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.DATE_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.DEVICE_STORAGE_LOW");
        defaultSystemEventsToCapture.add("android.intent.action.DEVICE_STORAGE_OK");
        defaultSystemEventsToCapture.add("android.intent.action.INPUT_METHOD_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.LOCALE_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.REBOOT");
        defaultSystemEventsToCapture.add("android.intent.action.SCREEN_OFF");
        defaultSystemEventsToCapture.add("android.intent.action.SCREEN_ON");
        defaultSystemEventsToCapture.add("android.intent.action.TIMEZONE_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.TIME_SET");
        defaultSystemEventsToCapture.add("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        defaultSystemEventsToCapture.add("android.os.action.POWER_SAVE_MODE_CHANGED");
        defaultSystemEventsToCapture.add("android.intent.action.DREAMING_STARTED");
        defaultSystemEventsToCapture.add("android.intent.action.DREAMING_STOPPED");

        IntentFilter filter = new IntentFilter();

        for (String event : defaultSystemEventsToCapture) {
            filter.addAction(event);
        }

        return filter;
    }

}
