package backtraceio.library.anr;

public class BacktraceThreadWatcher {
    private int counter;
    private int privateCounter;
    private int timeout;
    private int delay;
    private long lastTimestamp;
    private boolean active;

    BacktraceThreadWatcher(int timeout, int delay) {
        this.timeout = timeout;
        this.delay = delay;
        setActive(true);
    }

    int getTimeout() {
        return timeout;
    }

    int getDelay() {
        return delay;
    }

    long getLastTimestamp() {
        return lastTimestamp;
    }

    void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    synchronized boolean isActive() {
        return active;
    }

    synchronized void setActive(boolean active) {
        this.active = active;
    }

    void tickPrivateCounter() {
        privateCounter++;
    }

    int getPrivateCounter() {
        return privateCounter;
    }

    void setPrivateCounter(int privateCounter) {
        this.privateCounter = privateCounter;
    }

    synchronized int getCounter() {
        return counter;
    }

    public synchronized void tickCounter() {
        counter++;
    }
}
