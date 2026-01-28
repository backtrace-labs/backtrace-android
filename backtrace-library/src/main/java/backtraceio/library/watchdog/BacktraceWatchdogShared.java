package backtraceio.library.watchdog;

import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.json.BacktraceReport;
import java.util.HashMap;

/**
 * Used to share code used by objects that detect different types of blocked threads
 */
class BacktraceWatchdogShared {

    /**
     * Send information about the blocked thread to the backtrace console or do a custom event if it is set
     *
     * @param thread                          thread that has been blocked
     * @param backtraceClient                 Instance of BacktraceClient
     * @param onApplicationNotRespondingEvent Event which will be executed instead of default handling ANR error
     * @param LOG_TAG                         log tag that facilitates analysis during debugging
     */
    static void sendReportCauseBlockedThread(
            BacktraceClient backtraceClient,
            Thread thread,
            OnApplicationNotRespondingEvent onApplicationNotRespondingEvent,
            String LOG_TAG) {
        BacktraceWatchdogTimeoutException exception = new BacktraceWatchdogTimeoutException();
        exception.setStackTrace(thread.getStackTrace());
        BacktraceLogger.e(LOG_TAG, "Blocked thread detected, sending a report", exception);
        if (onApplicationNotRespondingEvent != null) {
            onApplicationNotRespondingEvent.onEvent(exception);
        } else if (backtraceClient != null) {
            backtraceClient.addBreadcrumb("ANR detected - thread is blocked");
            BacktraceReport report = new BacktraceReport(exception, new HashMap<String, Object>() {
                {
                    put(BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.AnrAttributeType);
                }
            });
            backtraceClient.send(report);
        }
    }
}
