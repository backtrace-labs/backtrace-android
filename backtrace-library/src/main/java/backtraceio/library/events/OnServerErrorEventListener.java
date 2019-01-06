package backtraceio.library.events;

public interface OnServerErrorEventListener {
    /**
     * Event which will be executed when exception appear during sending request to Backtrace API
     * @param exception exception that has been reported
     */
    void onEvent(Exception exception);
}