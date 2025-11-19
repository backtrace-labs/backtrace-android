package backtraceio.library.anr;

import static backtraceio.library.anr.AppExitInfoDetailsExtractor.getANRAttributes;

import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import backtraceio.library.BacktraceClient;
import backtraceio.library.common.ApplicationMetadataCache;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BacktraceAppExitInfoSenderHandler extends Thread implements BacktraceANRHandler {
    private static final String THREAD_NAME = "main-anr-appexit";
    private static final String LOG_TAG = BacktraceAppExitInfoSenderHandler.class.getSimpleName();
    private static final String ANR_COMPLEX_ATTR_KEY = "ANR annotations";
    private static final String ANR_STACKTRACE_PARSED_ATTR_KEY = "ANR parsed stacktrace";
    private static final String ANR_STACKTRACE_ATTR_KEY = "ANR stacktrace";

    private final BacktraceClient backtraceClient;
    private final String packageName;

    private final ProcessExitInfoProvider activityManager;

    private final AnrExitInfoState anrAppExitInfoState;

    public BacktraceAppExitInfoSenderHandler(BacktraceClient client, Context context) {
        this(
                client,
                ApplicationMetadataCache.getInstance(context).getPackageName(),
                new AnrExitInfoState(context),
                new ActivityManagerExitInfoProvider(context));
    }

    protected BacktraceAppExitInfoSenderHandler(
            BacktraceClient client,
            String packageName,
            AnrExitInfoState anrAppExitInfoState,
            ProcessExitInfoProvider activityManager) {
        super(THREAD_NAME);
        this.backtraceClient = client;
        this.packageName = packageName;
        this.anrAppExitInfoState = anrAppExitInfoState;
        this.activityManager = activityManager;

        this.start();
    }

    @Override
    public void run() {
        send();
    }

    private void send() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            BacktraceLogger.d(
                    LOG_TAG,
                    "Unsupported Android version " + android.os.Build.VERSION.SDK_INT
                            + " to send ANR based on applicationExitInfoList");
            return;
        }

        final List<ExitInfo> applicationExitInfoList =
                this.activityManager.getHistoricalExitInfo(this.packageName, 0, 0);

        Collections.reverse(applicationExitInfoList);
        for (ExitInfo appExitInfo : applicationExitInfoList) {
            synchronized (this.anrAppExitInfoState) {
                if (!this.shouldProcessAppExitInfo(appExitInfo)) {
                    continue;
                }

                sendApplicationExitInfoReport(appExitInfo);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void sendApplicationExitInfoReport(ExitInfo appExitInfo) {
        final BacktraceReport report = generateBacktraceReport(appExitInfo);

        if (report == null) {
            synchronized (this.anrAppExitInfoState) {
                this.anrAppExitInfoState.saveTimestamp(appExitInfo.getTimestamp());
            }
            return;
        }

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
    private BacktraceReport generateBacktraceReport(ExitInfo appExitInfo) {
        ;
        final Map<String, Object> anrAttributes = getANRAttributes(appExitInfo);
        final String stackTrace = AppExitInfoDetailsExtractor.getStackTraceInfo(appExitInfo);

        if (stackTrace == null || stackTrace.isEmpty()) {
            BacktraceLogger.w(LOG_TAG, "Empty stacktrace for ApplicationExitInfo");
            return null;
        }

        final Map<String, Object> parsedStackTraceAttributes = this.getAttributesFromStacktrace(stackTrace);
        final StackTraceElement[] anrStackTrace =
                ExitInfoStackTraceParser.parseMainThreadStackTrace(parsedStackTraceAttributes);

        final HashMap<String, Object> attributes = new HashMap<String, Object>() {
            {
                put(BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.AnrAttributeType);
                put(ANR_COMPLEX_ATTR_KEY, anrAttributes);
                put(ANR_STACKTRACE_ATTR_KEY, stackTrace);
                put(ANR_STACKTRACE_PARSED_ATTR_KEY, parsedStackTraceAttributes);
            }
        };

        return new BacktraceReport(new BacktraceANRExitInfoException(appExitInfo, anrStackTrace), attributes);
    }

    private Map<String, Object> getAttributesFromStacktrace(String stacktrace) {
        try {
            return ExitInfoStackTraceParser.parseANRStackTrace(stacktrace);
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Error during parsing ExitInfoStackTrace", ex);
            return new HashMap<>();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean isSupportedTypeOfApplicationExit(ExitInfo appExitInfo) {
        final List<Integer> supportedTypes = this.activityManager.getSupportedTypesOfExitInfo();
        return supportedTypes.contains(appExitInfo.getReason());
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean shouldProcessAppExitInfo(ExitInfo appExitInfo) {
        long lastAnrTimestamp = this.anrAppExitInfoState.getLastTimestamp();
        long anrTimestamp = appExitInfo.getTimestamp();

        if (lastAnrTimestamp >= anrTimestamp) {
            return false;
        }

        return isSupportedTypeOfApplicationExit(appExitInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean shouldUpdateLastTimestamp(ExitInfo appExitInfo) {
        return this.anrAppExitInfoState.getLastTimestamp() < appExitInfo.getTimestamp();
    }

    @Override
    public void setOnApplicationNotRespondingEvent(OnApplicationNotRespondingEvent onApplicationNotRespondingEvent) {}

    @Override
    public void stopMonitoringAnr() {
        if (!this.isInterrupted()) {
            BacktraceLogger.d(LOG_TAG, "ANR thread will be interrupted.");
            this.interrupt();
        }
    }
}
