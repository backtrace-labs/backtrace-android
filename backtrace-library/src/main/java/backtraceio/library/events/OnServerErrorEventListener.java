package backtraceio.library.events;

public interface OnServerErrorEventListener {
    void onEvent(Exception exception);
}
