package backtraceio.library.events;

import backtraceio.library.models.BacktraceResult;

public interface OnAfterSendEventListener {
    void onEvent(BacktraceResult result);
}