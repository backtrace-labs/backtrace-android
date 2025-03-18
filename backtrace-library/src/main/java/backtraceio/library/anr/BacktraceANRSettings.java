package backtraceio.library.anr;

import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

public class BacktraceANRSettings {
    public int timeout = 0;
    public boolean debug = false;
    public OnApplicationNotRespondingEvent onApplicationNotRespondingEvent = null;

    public BacktraceANRSettings() { }

    public BacktraceANRSettings(int timeout, OnApplicationNotRespondingEvent onApplicationNotRespondingEvent, boolean debug) {
        this.debug = debug;
        this.onApplicationNotRespondingEvent = onApplicationNotRespondingEvent;
        this.timeout = timeout;
    }
}
