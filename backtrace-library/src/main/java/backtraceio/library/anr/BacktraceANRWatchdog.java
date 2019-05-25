package backtraceio.library.anr;

import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Calendar;

import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;


public class BacktraceANRWatchdog extends Thread {

    private final static transient String LOG_TAG = BacktraceANRWatchdog.class.getSimpleName();
    private final static transient int DEFAULT_ANR_TIMEOUT = 5000;

    private final BacktraceClient backtraceClient;
    private final boolean debug;
    private int timeout = 5000;

    public BacktraceANRWatchdog(BacktraceClient client) {
        this(client, DEFAULT_ANR_TIMEOUT);
    }

    public BacktraceANRWatchdog(BacktraceClient client, int timeout) {
        this(client, timeout, false);
    }

    public BacktraceANRWatchdog(BacktraceClient client, int timeout, boolean debug){
        this.backtraceClient = client;
        this.timeout = timeout;
        this.debug = debug;
    }

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    public void run() {
        while(!isInterrupted()){
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

            if(threadWatcher.getCounter() == threadWatcher.getPrivateCounter()){
                BacktraceLogger.d(LOG_TAG, "ANR is not detected");
                continue;
            }

            if(debug && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())){
                BacktraceLogger.w(LOG_TAG, "ANR detected but will be ignored because debug mode is on and connected debugger");
                continue;
            }

            sendReportCauseBlockedThread(Looper.getMainLooper().getThread());
        }
    }


    //TODO: remove duplicate code
    private void sendReportCauseBlockedThread(Thread thread) {
        BacktraceWatchdogTimeoutException exception = new BacktraceWatchdogTimeoutException();
        exception.setStackTrace(thread.getStackTrace());
        BacktraceLogger.e(LOG_TAG, "Blocked thread detected, sending a report", exception);
        if(backtraceClient != null) {
            backtraceClient.send(exception);
        }
    }
}
