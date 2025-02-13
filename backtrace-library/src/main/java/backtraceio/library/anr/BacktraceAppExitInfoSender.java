package backtraceio.library.anr;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.util.Log;

import java.util.Collections;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.ApplicationMetadataCache;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceAppExitInfoSender {
    private final static String LOG_TAG = BacktraceAppExitInfoSender.class.getSimpleName();

    private final BacktraceClient backtraceClient;
    private final String packageName;

    private final ActivityManager activityManager;

    private final AnrAppExitInfoState anrAppExitInfoState;

    public BacktraceAppExitInfoSender(BacktraceClient client, Context context) {
        this.backtraceClient = client;
        this.packageName = ApplicationMetadataCache.getInstance(context).getPackageName();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.anrAppExitInfoState = new AnrAppExitInfoState(context);
    }

    public void send() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            Log.w(LOG_TAG, ""); // TODO:
            return;
        }

        final List<ApplicationExitInfo> applicationExitInfoList = this.activityManager.getHistoricalProcessExitReasons(this.packageName, 0, 0);

        Collections.reverse(applicationExitInfoList);
        for (ApplicationExitInfo appExitInfo : applicationExitInfoList) {
            long anrTimestamp = appExitInfo.getTimestamp();
            long lastAnrTimestamp = this.anrAppExitInfoState.getLastTimestamp();

            if (lastAnrTimestamp >= anrTimestamp) {
                continue;
            }
            BacktraceANRApplicationExitException exception = new BacktraceANRApplicationExitException(appExitInfo);
            backtraceClient.send(exception, backtraceResult -> {
                if (backtraceResult.status == BacktraceResultStatus.Ok) {
                    this.anrAppExitInfoState.saveTimestamp(anrTimestamp);
                }
            });
        }
    }
}
