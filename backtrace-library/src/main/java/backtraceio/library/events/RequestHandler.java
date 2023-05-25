package backtraceio.library.events;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;

/**
 * Interface definition for a callback to be invoked instead of default request to Backtrace API
 */
public interface RequestHandler {
    /**
     * Event which will be executed instead of default request to Backtrace API
     *
     * @param data which should be send to Backtrace API
     * @return response on request
     */
    BacktraceResult onRequest(String url, BacktraceData data);

    BacktraceResult onNativeRequest(String url, BacktraceNativeData data);
}
