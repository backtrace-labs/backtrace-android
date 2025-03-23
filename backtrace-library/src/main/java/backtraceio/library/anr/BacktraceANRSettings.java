package backtraceio.library.anr;

import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

public class BacktraceANRSettings {
    private int timeout = 0;
    private boolean debug = false;
    private OnApplicationNotRespondingEvent onApplicationNotRespondingEvent = null;

    public BacktraceANRSettings() { }

    public BacktraceANRSettings(int timeout, OnApplicationNotRespondingEvent onApplicationNotRespondingEvent, boolean debug) {
        super();
        this.debug = debug;
        this.onApplicationNotRespondingEvent = onApplicationNotRespondingEvent;
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isDebug() {
        return debug;
    }

    public OnApplicationNotRespondingEvent getOnApplicationNotRespondingEvent() {
        return onApplicationNotRespondingEvent;
    }
}
