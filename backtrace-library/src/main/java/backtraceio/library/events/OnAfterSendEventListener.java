package backtraceio.library.events;

import backtraceio.library.models.BacktraceResult;


public interface OnAfterSendEventListener {
    /**
     * Event which will be executed after send report to Backtrace API
     * @param result server response
     */
    void onEvent(BacktraceResult result);
}