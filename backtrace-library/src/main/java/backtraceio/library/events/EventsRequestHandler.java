package backtraceio.library.events;

import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.EventsResult;

/**
 * Interface definition for a callback to be invoked instead of default request to Backtrace API
 */
public interface EventsRequestHandler {
    /**
     * Event which will be executed instead of default request to Backtrace API
     *
     * @param data which should be send to Backtrace API
     * @return response on request
     */
    EventsResult onRequest(EventsPayload data);
}
