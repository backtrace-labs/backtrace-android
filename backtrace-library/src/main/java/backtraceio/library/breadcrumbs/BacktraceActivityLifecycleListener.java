package backtraceio.library.breadcrumbs;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import backtraceio.library.enums.BacktraceBreadcrumbType;

public class BacktraceActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    private BacktraceBreadcrumbs backtraceBreadcrumbs;

    public BacktraceActivityLifecycleListener(BacktraceBreadcrumbs backtraceBreadcrumbs) {
        this.backtraceBreadcrumbs = backtraceBreadcrumbs;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        String message = activity.getLocalClassName() + " onActivityCreated()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        String message = activity.getLocalClassName() + " onActivityStarted()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        String message = activity.getLocalClassName() + " onActivityResumed()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        String message = activity.getLocalClassName() + " onActivityPaused()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        String message = activity.getLocalClassName() + " onActivityStopped()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        String message = activity.getLocalClassName() + " onActivitySaveInstanceState()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        String message = activity.getLocalClassName() + " onActivityDestroyed()";
        backtraceBreadcrumbs.addBreadcrumb(message, BacktraceBreadcrumbType.SYSTEM);
    }
}
