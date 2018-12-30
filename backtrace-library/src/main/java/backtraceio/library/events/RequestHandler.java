package backtraceio.library.events;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;

public interface RequestHandler {
    BacktraceResult onRequest(BacktraceData data);
}
