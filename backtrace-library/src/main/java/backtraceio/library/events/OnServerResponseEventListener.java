package backtraceio.library.events;

import backtraceio.library.models.BacktraceResult;

public interface OnServerResponseEventListener {
    void onEvent(BacktraceResult backtraceResult);
}
