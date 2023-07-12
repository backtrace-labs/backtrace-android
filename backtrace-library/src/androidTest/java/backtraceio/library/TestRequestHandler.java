package backtraceio.library;

import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;

public abstract class TestRequestHandler implements RequestHandler {

    public BacktraceResult onRequest(BacktraceData data) {
        return null;
    }

    public BacktraceResult onNativeRequest(BacktraceNativeData data) {
        return null;
    }
}
