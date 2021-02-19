package backtraceio.library.breadcrumbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceBroadcastReceiver extends BroadcastReceiver {

    private BacktraceBreadcrumbs backtraceBreadcrumbs;

    private static transient String LOG_TAG = BacktraceBroadcastReceiver.class.getSimpleName();

    public BacktraceBroadcastReceiver(@NonNull BacktraceBreadcrumbs backtraceBreadcrumbs)
    {
        this.backtraceBreadcrumbs = backtraceBreadcrumbs;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
        {
            BacktraceLogger.e(LOG_TAG, "Null action received. This is a bug");
            return;
        }

        Map<String, Object> attributes = null;
        Bundle extras = intent.getExtras();

        if (extras != null && extras.keySet() != null) {
            Set<String> keys = extras.keySet();

            attributes = new HashMap<String, Object>();
            for (String key : keys) {
                attributes.put(key, extras.get(key));
            }
        }

        backtraceBreadcrumbs.addBreadcrumb(action, attributes, BacktraceBreadcrumbType.SYSTEM);
    }

    public IntentFilter getIntentFilter(Set<BacktraceBreadcrumbType> enabledBreadcrumbTypes) {
        IntentFilter filter = new IntentFilter();

        if (enabledBreadcrumbTypes == null) {
            return filter;
        }

        if (enabledBreadcrumbTypes.contains(BacktraceBreadcrumbType.USER)) {
            filter.addAction("android.appwidget.action.APPWIDGET_DELETED");
            filter.addAction("android.appwidget.action.APPWIDGET_DISABLED");
            filter.addAction("android.appwidget.action.APPWIDGET_ENABLED");
            filter.addAction("android.intent.action.CAMERA_BUTTON");
            filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            filter.addAction("android.intent.action.DOCK_EVENT");
            filter.addAction("android.intent.action.AIRPLANE_MODE");
        }

        if (enabledBreadcrumbTypes.contains(BacktraceBreadcrumbType.SYSTEM)) {
            filter.addAction("android.appwidget.action.APPWIDGET_HOST_RESTORED");
            filter.addAction("android.appwidget.action.APPWIDGET_RESTORED");
            filter.addAction("android.appwidget.action.APPWIDGET_UPDATE");
            filter.addAction("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
            filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            filter.addAction("android.intent.action.ACTION_SHUTDOWN");
            filter.addAction("android.intent.action.BATTERY_LOW");
            filter.addAction("android.intent.action.BATTERY_OKAY");
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction("android.intent.action.CONTENT_CHANGED");
            filter.addAction("android.intent.action.DATE_CHANGED");
            filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            filter.addAction("android.intent.action.DEVICE_STORAGE_OK");
            filter.addAction("android.intent.action.INPUT_METHOD_CHANGED");
            filter.addAction("android.intent.action.LOCALE_CHANGED");
            filter.addAction("android.intent.action.REBOOT");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        }

        if (enabledBreadcrumbTypes.contains(BacktraceBreadcrumbType.NAVIGATION)) {
            filter.addAction("android.intent.action.DREAMING_STARTED");
            filter.addAction("android.intent.action.DREAMING_STOPPED");
        }

        return filter;
    }
}
