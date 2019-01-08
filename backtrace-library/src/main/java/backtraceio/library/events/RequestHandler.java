package backtraceio.library.events;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;

public interface RequestHandler {
    /**
     * Event which will be executed instead of default request to Backtrace API
     * @param data which should be send to Backtrace API
     * @return response on request
     */
    BacktraceResult onRequest(BacktraceData data);
}
