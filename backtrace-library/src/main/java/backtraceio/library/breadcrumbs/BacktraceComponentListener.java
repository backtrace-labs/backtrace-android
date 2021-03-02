package backtraceio.library.breadcrumbs;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;

public class BacktraceComponentListener implements ComponentCallbacks2 {

    private BacktraceBreadcrumbs backtraceBreadcrumbs;

    public BacktraceComponentListener(BacktraceBreadcrumbs backtraceBreadcrumbs)
    {
        this.backtraceBreadcrumbs = backtraceBreadcrumbs;
    }

    private String getMemoryWarningString(final int level)
    {
        switch (level)
        {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                return "TRIM MEMORY UI HIDDEN";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                return "TRIM MEMORY RUNNING MODERATE";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                return "TRIM MEMORY RUNNING LOW";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                return "TRIM MEMORY RUNNING CRITICAL";
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                return "TRIM MEMORY BACKGROUND";
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                return "TRIM MEMORY MODERATE";
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                return "TRIM MEMORY COMPLETE";
            default:
                return "Generic memory warning";
        }
    }

    private BacktraceBreadcrumbLevel getMemoryWarningLevel(final int level)
    {
        switch (level)
        {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                return BacktraceBreadcrumbLevel.ERROR;
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                return BacktraceBreadcrumbLevel.FATAL;
            default:
                return BacktraceBreadcrumbLevel.WARNING;
        }
    }

    @Override
    public void onTrimMemory(int level) {
        String messageString = getMemoryWarningString(level);
        BacktraceBreadcrumbLevel breadcrumbLevel = getMemoryWarningLevel(level);
        backtraceBreadcrumbs.addBreadcrumb(messageString,
                                        BacktraceBreadcrumbType.SYSTEM,
                                        breadcrumbLevel);
    }

    private String stringifyOrientation(final int orientation)
    {
        switch (orientation)
        {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "portrait";
            default:
                return "unknown orientation";
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        String orientation = stringifyOrientation(newConfig.orientation);
        attributes.put("orientation", orientation);
        backtraceBreadcrumbs.addBreadcrumb("Configuration changed",
                                        attributes,
                                        BacktraceBreadcrumbType.SYSTEM,
                                        BacktraceBreadcrumbLevel.INFO);
    }

    @Override
    public void onLowMemory() {
        backtraceBreadcrumbs.addBreadcrumb("Critical low memory warning!",
                                        BacktraceBreadcrumbType.SYSTEM,
                                        BacktraceBreadcrumbLevel.FATAL);
    }
}
