package backtraceio.library.anr;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;

import java.util.Collections;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.ApplicationMetadataCache;

public class BacktraceAppExitInfoSender {
    private final static transient String LOG_TAG = BacktraceAppExitInfoSender.class.getSimpleName();

    private final BacktraceClient backtraceClient;
    private final String packageName;

    private final ActivityManager activityManager;

    public BacktraceAppExitInfoSender(BacktraceClient client, Context context) {
        this.backtraceClient = client;
        this.packageName = ApplicationMetadataCache.getInstance(context).getPackageName();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public void send() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            return;
        }

        List<ApplicationExitInfo> applicationExitInfoList = this.activityManager.getHistoricalProcessExitReasons(this.packageName, 0, 0);

        Collections.reverse(applicationExitInfoList);
        for (ApplicationExitInfo appExitInfo : applicationExitInfoList) {
            BacktraceANRApplicationExitException exception = new BacktraceANRApplicationExitException(appExitInfo);
            backtraceClient.send(exception);
        }
    }
}
