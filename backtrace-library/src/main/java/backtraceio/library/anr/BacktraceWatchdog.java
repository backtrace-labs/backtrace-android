package backtraceio.library.anr;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceWatchdog {

    private final static transient String LOG_TAG = BacktraceWatchdog.class.getSimpleName();
    private final BacktraceClient backtraceClient;
    private final boolean sendException;
    private Map<Thread, BacktraceThreadWatcher> threadsIdWatcher = new HashMap<>();

    /**
     * Event which will be executed instead of default handling ANR error
     */
    private OnApplicationNotRespondingEvent onApplicationNotRespondingEvent;

    /**
     * Initialize new instance of BacktraceWatchdog
     *
     * @param client        current Backtrace client instance which will be used to send information about exception
     * @param sendException
     */
    public BacktraceWatchdog(BacktraceClient client, boolean sendException) {
        this.sendException = sendException;
        this.backtraceClient = client;
    }

    public BacktraceWatchdog(BacktraceClient client) {
        this(client, true);
    }

    public void setOnApplicationNotRespondingEvent(OnApplicationNotRespondingEvent
                                                           onApplicationNotRespondingEvent) {
        this.onApplicationNotRespondingEvent = onApplicationNotRespondingEvent;
    }

    /**
     * Check if any of the registered threads are blocked
     *
     * @return if any thread is blocked
     */
    public boolean checkIsAnyThreadIsBlocked() {
        final long now = System.currentTimeMillis();
        final String now_str = Long.toString(now);

        BacktraceLogger.d(LOG_TAG, "Checking watchdog. Timestamp: " + now_str);
        for (Map.Entry<Thread, BacktraceThreadWatcher> entry : this.threadsIdWatcher.entrySet()) {
            final Thread currentThread = entry.getKey();
            final BacktraceThreadWatcher currentWatcher = entry.getValue();

            if (currentThread == null || currentWatcher == null ||
                    currentThread == Thread.currentThread()) {
                continue;
            }

            if (!currentThread.isAlive() || !currentWatcher.isActive()) {
                continue;
            }

            if (currentWatcher.getCounter() != currentWatcher.getPrivateCounter()) {
                currentWatcher.setPrivateCounter(currentWatcher.getCounter());
                currentWatcher.setLastTimestamp(now);
                continue;
            }

            BacktraceLogger.w(LOG_TAG, String.format("Thread %s %s  might be hung, timestamp: %s",
                    Long.toString(currentThread.getId()), currentThread.getName(), now_str));

            // Otherwise, the thread has not made forward progress.
            // Determine whether the timeout has been exceeded.
            long timestamp = currentWatcher.getLastTimestamp();
            long timeout = timestamp == 0 ? currentWatcher.getTimeout() :
                    currentWatcher.getTimeout() + currentWatcher.getDelay();

            if (now - timestamp > timeout) {
                if (this.sendException) {
                    sendReportCauseBlockedThread(currentThread);
                }
                return true;
            }
        }

        return false;
    }

    private void sendReportCauseBlockedThread(Thread thread) {
        BacktraceWatchdogTimeoutException exception = new BacktraceWatchdogTimeoutException();
        exception.setStackTrace(thread.getStackTrace());
        BacktraceLogger.e(LOG_TAG, "Blocked thread detected, sending a report", exception);
        if (this.onApplicationNotRespondingEvent != null) {
            this.onApplicationNotRespondingEvent.onEvent(exception);
        } else if (backtraceClient != null) {
            backtraceClient.send(exception);
        }
    }


    /**
     * Register a thread to monitor it
     *
     * @param thread  thread which should be monitored
     * @param timeout time in milliseconds after which we consider the thread to be blocked
     * @param delay
     */
    public void registerThread(Thread thread, int timeout, int delay) {
        threadsIdWatcher.put(thread, new BacktraceThreadWatcher(timeout, delay));
    }

    /**
     * @param thread thread which should stop being monitored
     */
    public void unRegisterThread(Thread thread) {
        threadsIdWatcher.remove(thread);
    }

    /**
     * Increase the counter associated with the thread
     *
     * @param thread thread whose counter should be increased
     */
    public void tick(Thread thread) {
        if (!threadsIdWatcher.containsKey(thread)) {
            return;
        }
        BacktraceThreadWatcher threadWatcher = threadsIdWatcher.get(thread);
        if (threadWatcher == null) {
            return;
        }
        threadWatcher.tickCounter();
    }

    /**
     * Activate the watcher associated with the thread to resume monitoring the thread
     *
     * @param thread thread whose counter should be activate
     */
    public void activateWatcher(Thread thread) {
        if (!threadsIdWatcher.containsKey(thread)) {
            return;
        }
        BacktraceThreadWatcher threadWatcher = threadsIdWatcher.get(thread);
        if (threadWatcher == null) {
            return;
        }
        threadWatcher.setActive(true);
    }

    /**
     * Deactivate the thread watcher associated with the thread to temporarily stop monitoring the thread
     *
     * @param thread thread whose watcher should be deactivate
     */
    public void deactivateWatcher(Thread thread) {
        if (!threadsIdWatcher.containsKey(thread)) {
            return;
        }
        BacktraceThreadWatcher threadWatcher = threadsIdWatcher.get(thread);
        if (threadWatcher == null) {
            return;
        }
        threadWatcher.setActive(false);
    }
}
