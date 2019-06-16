package backtraceio.library.watchdog;

public interface OnApplicationNotRespondingEvent {
    void onEvent(BacktraceWatchdogTimeoutException exception);
}
