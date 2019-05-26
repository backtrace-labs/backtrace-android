package backtraceio.library.anr;

import backtraceio.library.anr.BacktraceWatchdogTimeoutException;

public interface OnApplicationNotRespondingEvent {
    void onEvent(BacktraceWatchdogTimeoutException exception);
}
