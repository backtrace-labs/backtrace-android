package backtraceio.library.events;

import backtraceio.library.models.BacktraceData;

public interface OnBeforeSendEventListener {
    BacktraceData onEvent(BacktraceData data);
}