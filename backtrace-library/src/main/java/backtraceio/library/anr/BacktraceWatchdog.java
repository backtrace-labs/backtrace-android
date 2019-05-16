package backtraceio.library.anr;

import java.util.HashMap;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;

public class BacktraceWatchdog {

    private final static transient String LOG_TAG = BacktraceWatchdog.class.getSimpleName();
    private final BacktraceClient backtraceClient;
    private Map<Thread, BacktraceThreadWatcher> threadsIdWatcher = new HashMap<>();

    public BacktraceWatchdog(BacktraceClient client) {
        this.backtraceClient = client;
    }

    public boolean checkWatchdog() {
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

            if (!currentThread.isAlive() || currentWatcher.isActive()) {
                continue;
            }

            // Thread is in an idle state, we can ignore.
            if (currentWatcher.getPrivateCounter() == currentWatcher.getCounter()) {
                continue;
            }

            if (currentWatcher.getCounter() != currentWatcher.getPrivateCounter()) {
                currentWatcher.setPrivateCounter(currentWatcher.getCounter());
                currentWatcher.setLastTimestamp(now);
            }

            BacktraceLogger.w(LOG_TAG, String.format("Thread %s %s  might be hung, timestamp: %s",
                    Long.toString(currentThread.getId()), currentThread.getName(), now_str));

            // Otherwise, the thread has not made forward progress.
            // Determine whether the timeout has been exceeded.
            long timestamp = currentWatcher.getLastTimestamp();
            long timeout = timestamp == 0 ? currentWatcher.getTimeout() :
                    currentWatcher.getTimeout() + currentWatcher.getDelay();

            if (now - timestamp > timeout) {
                sendReportCauseBlockedThread(currentThread);
                return false;
            }
        }

        return true;
    }

    private void sendReportCauseBlockedThread(Thread thread) {
        BacktraceWatchdogTimeoutException exception = new BacktraceWatchdogTimeoutException();
        exception.setStackTrace(thread.getStackTrace());
        BacktraceLogger.e(LOG_TAG, "Blocked thread detected, sending a report", exception);
        backtraceClient.send(exception);
    }


    public void registerThread(Thread thread, int timeout, int delay) {
        threadsIdWatcher.put(thread, new BacktraceThreadWatcher(timeout, delay));
    }

    public void unRegisterThread(Thread thread) {
        threadsIdWatcher.remove(thread);
    }

    public void tick(Thread thread) {

        threadsIdWatcher.get(thread).tickPrivateCounter();
    }

    public void activateWatcher(Thread thread) {
        threadsIdWatcher.get(thread).setActive(true);
    }

    public void deactivateWatcher(Thread thread) {
        threadsIdWatcher.get(thread).setActive(false);
    }
}
