package backtraceio.library.watchdog;

import android.os.Debug;
import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;

import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;


/**
 * This is the class that is responsible for monitoring the
 * user interface thread and sending an error if it is blocked
 */
public class BacktraceANRWatchdog extends Thread {

    private final static transient String LOG_TAG = BacktraceANRWatchdog.class.getSimpleName();

    /**
     * Default timeout value in milliseconds
     */
    private final static transient int DEFAULT_ANR_TIMEOUT = 5000;

    /**
     * Current Backtrace client instance which will be used to send information about exception
     */
    private final BacktraceClient backtraceClient;

    /**
     * Enable debug mode - errors will not be sent if the debugger is connected
     */
    private final boolean debug;

    /**
     * Handler for UI Thread - used to check if the thread is not blocked
     */
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Maximum time in milliseconds after which should check if the main thread is not hanged
     */
    private final int timeout;

    /**
     * Event which will be executed instead of default handling ANR error
     */
    private OnApplicationNotRespondingEvent onApplicationNotRespondingEvent;

    /**
     * Check if thread should stop
     */
    private volatile boolean shouldStop = false;

    /**
     * Initialize new instance of BacktraceANRWatchdog with default timeout
     *
     * @param client current Backtrace client instance which will be used to send information about exception
     */
    public BacktraceANRWatchdog(BacktraceClient client) {
        this(client, DEFAULT_ANR_TIMEOUT);
    }

    /**
     * Initialize new instance of BacktraceANRWatchdog without debugging
     *
     * @param client  current Backtrace client instance which will be used to send information about exception
     * @param timeout maximum time in milliseconds after which should check if the main thread is not hanged
     */
    public BacktraceANRWatchdog(BacktraceClient client, int timeout) {
        this(client, timeout, false);
    }

    /**
     * Initialize new instance of BacktraceANRWatchdog
     *
     * @param client  current Backtrace client instance which will be used to send information about exception
     * @param timeout maximum time in milliseconds after which should check if the main thread is not hanged
     * @param debug   enable debug mode - errors will not be sent if the debugger is connected
     */
    public BacktraceANRWatchdog(BacktraceClient client, int timeout, boolean debug) {
        BacktraceLogger.d(LOG_TAG, "Start monitoring ANR");
        this.backtraceClient = client;
        this.timeout = timeout;
        this.debug = debug;
        this.start();
    }

    public void setOnApplicationNotRespondingEvent(OnApplicationNotRespondingEvent
                                                           onApplicationNotRespondingEvent) {
        this.onApplicationNotRespondingEvent = onApplicationNotRespondingEvent;
    }

    /**
     * Method which is using to check if the user interface thread has been blocked
     */
    @Override
    public void run() {
        Boolean reported = false;
        while (!shouldStop && !isInterrupted()) {
            String dateTimeNow = Calendar.getInstance().getTime().toString();
            BacktraceLogger.d(LOG_TAG, "ANR WATCHDOG - " + dateTimeNow);
            final BacktraceThreadWatcher threadWatcher = new BacktraceThreadWatcher(0, 0);
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    threadWatcher.tickCounter();
                }
            });
            try {
                Thread.sleep(this.timeout);
            } catch (InterruptedException e) {
                BacktraceLogger.e(LOG_TAG, "Thread is interrupted", e);
                return;
            }
            threadWatcher.tickPrivateCounter();

            if (threadWatcher.getCounter() == threadWatcher.getPrivateCounter()) {
                reported = false;
                BacktraceLogger.d(LOG_TAG, "ANR is not detected");
                continue;
            }

            if (debug && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
                BacktraceLogger.w(LOG_TAG, "ANR detected but will be ignored because debug mode " +
                        "is on and connected debugger");
                continue;
            }
            if(reported) {
                // skiping, because we already reported an ANR report for current ANR
                continue;
            }
            reported = true;
            BacktraceWatchdogShared.sendReportCauseBlockedThread(backtraceClient,
                    Looper.getMainLooper().getThread(), onApplicationNotRespondingEvent, LOG_TAG);
        }
    }

    public void stopMonitoringAnr() {
        BacktraceLogger.d(LOG_TAG, "Stop monitoring ANR");
        shouldStop = true;
    }
}
