package backtraceio.library.anr;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

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

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean isSupportedTypeOfApplicationExit(int reason) {
        final List<Integer> supportedTypes = Collections.singletonList(ApplicationExitInfo.REASON_ANR);
        return supportedTypes.contains(reason);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean shouldProcessAppExitInfo(ApplicationExitInfo appExitInfo) {
        long lastAnrTimestamp = this.anrAppExitInfoState.getLastTimestamp();
        long anrTimestamp = appExitInfo.getTimestamp();

        if (lastAnrTimestamp >= anrTimestamp) {
            return false;
        }

        return isSupportedTypeOfApplicationExit(appExitInfo.getReason());
    }

    public void send() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            Log.w(LOG_TAG, ""); // TODO:
            return;
        }

        final List<ApplicationExitInfo> applicationExitInfoList = this.activityManager.getHistoricalProcessExitReasons(this.packageName, 0, 0);

        Collections.reverse(applicationExitInfoList);
        for (ApplicationExitInfo appExitInfo : applicationExitInfoList) {

            if (!this.shouldProcessAppExitInfo(appExitInfo)) {
                continue;
            }

            BacktraceANRApplicationExitException exception = new BacktraceANRApplicationExitException(appExitInfo);
            backtraceClient.send(exception, backtraceResult -> {
                if (backtraceResult.status == BacktraceResultStatus.Ok) {
                    this.anrAppExitInfoState.saveTimestamp(appExitInfo.getTimestamp());
                }
            });
        }
    }
}
