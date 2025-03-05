package backtraceio.library.anr;

import static backtraceio.library.anr.AppExitInfoDetailsExtractor.getANRAttributes;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.ApplicationMetadataCache;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

public class BacktraceAppExitInfoSenderHandler extends Thread implements BacktraceANRHandler {
    private final static String LOG_TAG = BacktraceAppExitInfoSenderHandler.class.getSimpleName();
    private final static String ANR_COMPLEX_ATTR_KEY = "ANR_ANNOTATIONS";

    private final BacktraceClient backtraceClient;
    private final String packageName;

    private final ActivityManager activityManager;

    private final AnrAppExitInfoState anrAppExitInfoState;

    public BacktraceAppExitInfoSenderHandler(BacktraceClient client, Context context) {
        this.backtraceClient = client;
        this.packageName = ApplicationMetadataCache.getInstance(context).getPackageName();
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.anrAppExitInfoState = new AnrAppExitInfoState(context);
        this.start();
    }

    @Override
    public void run() {
        send();
    }

    private void send() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            BacktraceLogger.d(LOG_TAG, "Unsupported Android version " + android.os.Build.VERSION.SDK_INT + " to send ANR based on applicationExitInfoList");
            return;
        }

        final List<ApplicationExitInfo> applicationExitInfoList = this.activityManager.getHistoricalProcessExitReasons(this.packageName, 0, 0);

        Collections.reverse(applicationExitInfoList);
        for (ApplicationExitInfo appExitInfo : applicationExitInfoList) {

            synchronized (this.anrAppExitInfoState) {
                if (!this.shouldProcessAppExitInfo(appExitInfo)) {
                    continue;
                }

                sendApplicationExitInfoReport(appExitInfo);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void sendApplicationExitInfoReport(ApplicationExitInfo appExitInfo) {
        final BacktraceReport report = generateBacktraceReport(appExitInfo);
        BacktraceLogger.d(LOG_TAG, "Sending ApplicationExitInfo ANR: " + report.message);
        backtraceClient.send(report, backtraceResult -> {
            synchronized (this.anrAppExitInfoState) {
                if (backtraceResult.status == BacktraceResultStatus.Ok && shouldUpdateLastTimestamp(appExitInfo)) {
                    this.anrAppExitInfoState.saveTimestamp(appExitInfo.getTimestamp());
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private BacktraceReport generateBacktraceReport(ApplicationExitInfo appExitInfo) {
        BacktraceANRApplicationExitException exception = new BacktraceANRApplicationExitException(appExitInfo);

        HashMap<String, Object> attributes = new HashMap<>();

        attributes.put(BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.AnrAttributeType);
        attributes.put(ANR_COMPLEX_ATTR_KEY, getANRAttributes(appExitInfo));

        return new BacktraceReport(exception, attributes);
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

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean shouldUpdateLastTimestamp(ApplicationExitInfo appExitInfo) {
        long lastAnrTimestamp = this.anrAppExitInfoState.getLastTimestamp();

        return lastAnrTimestamp < appExitInfo.getTimestamp();
    }

    @Override
    public void setOnApplicationNotRespondingEvent(
            OnApplicationNotRespondingEvent onApplicationNotRespondingEvent) {}

    @Override
    public void stopMonitoringAnr() {}
}
