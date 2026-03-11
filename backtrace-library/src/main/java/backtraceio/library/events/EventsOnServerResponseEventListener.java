package backtraceio.library.events;

import backtraceio.library.models.metrics.EventsResult;

/**
 * Interface definition for a callback to be invoked when server return responses from Backtrace API
 * for an events submission request
 */
public interface EventsOnServerResponseEventListener {
    /**
     * Event which will be executed when server returns response from Backtrace API
     *
     * @param result server response
     */
    void onEvent(EventsResult result);
}
