package backtraceio.library.anr;

import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

public interface BacktraceANRHandler {
    void setOnApplicationNotRespondingEvent(OnApplicationNotRespondingEvent onApplicationNotRespondingEvent);

    void stopMonitoringAnr();
}
