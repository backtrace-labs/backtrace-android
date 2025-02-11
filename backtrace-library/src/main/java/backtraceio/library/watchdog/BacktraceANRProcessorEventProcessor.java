package backtraceio.library.watchdog;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.os.Build;

import java.util.List;

import backtraceio.library.BacktraceClient;

public class BacktraceANRProcessorEventProcessor implements BacktraceANRProcessor {
    private final BacktraceClient client;
    private final ActivityManager activityManager;
    private final String packageName;

    public BacktraceANRProcessorEventProcessor(ActivityManager activityManager, BacktraceClient client, String packageName) {
        this.client = client;
        this.activityManager = activityManager;
        this.packageName = packageName;
        this.run();
    }

    public void run() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        List<ApplicationExitInfo> applicationExitInfoList = this.activityManager.getHistoricalProcessExitReasons(packageName, 0, 0);
        if (applicationExitInfoList.isEmpty()) {
            return;
        }
        ApplicationExitInfo lastApplicationExit = applicationExitInfoList.get(0);
        if (lastApplicationExit.getReason() != ApplicationExitInfo.REASON_ANR) {
            return;
        }

        client.send(new BacktraceANRApplicationExitException(lastApplicationExit));

    }

    @Override
    public void stopMonitoringAnr() {
    }
}
