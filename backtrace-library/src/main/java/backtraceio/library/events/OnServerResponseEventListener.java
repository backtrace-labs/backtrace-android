package backtraceio.library.events;

import backtraceio.library.models.BacktraceResult;

public interface OnServerResponseEventListener {
    /**
     * Event which will be executed when server return response from Backtrace API
     * @param backtraceResult server response
     */
    void onEvent(BacktraceResult backtraceResult);
}