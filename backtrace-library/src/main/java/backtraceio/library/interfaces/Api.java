package backtraceio.library.interfaces;

import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.metrics.SummedEventsPayload;
import backtraceio.library.models.metrics.UniqueEventsPayload;
import backtraceio.library.services.BacktraceMetrics;
import backtraceio.library.services.SummedEventsHandler;
import backtraceio.library.services.UniqueEventsHandler;

/**
 * API sender interface
 */
public interface Api {

    /**
     * Send a Backtrace report to Backtrace report submission API
     *
     * @param data diagnostic data
     */
    void send(BacktraceData data, OnServerResponseEventListener callback);

    /**
     * Send a Backtrace unique events payload to Backtrace events submission API
     *
     * @param payload unique events payload
     */
    void sendEventsPayload(UniqueEventsPayload payload);

    /**
     * Send a Backtrace summed events payload to Backtrace events submission API
     *
     * @param payload summed events payload
     */
    void sendEventsPayload(SummedEventsPayload payload);

    /**
     * Set an event executed when received bad request, unauthorized request or other information
     * from server
     */
    void setOnServerError(OnServerErrorEventListener onServerError);

    /**
     * Set custom request method to prepare HTTP request to Backtrace report API
     *
     * @param requestHandler event which will be executed instead of default request to Backtrace report API
     */
    void setRequestHandler(RequestHandler requestHandler);

    /**
     * Create metrics events handler for unique events
     *
     * @param backtraceMetrics  Backtrace metrics object
     * @return Reference to the created UniqueEventsHandler
     */
    UniqueEventsHandler enableUniqueEvents(BacktraceMetrics backtraceMetrics);

    /**
     * Create metrics events handler for summed events
     *
     * @param backtraceMetrics  Backtrace metrics object
     * @return Reference to the created SummedEventsHandler
     */
    SummedEventsHandler enableSummedEvents(BacktraceMetrics backtraceMetrics);

    /**
     * Set the request handler for unique events
     *
     * @param eventsRequestHandler
     */
    void setUniqueEventsRequestHandler(EventsRequestHandler eventsRequestHandler);

    /**
     * Set a custom event to trigger when a unique events request gets a response from the Backtrace API
     *
     * @param callback The custom event to trigger on an API response for a unique events request
     */
    void setUniqueEventsOnServerResponse(EventsOnServerResponseEventListener callback);

    /**
     * Set the request handler for summed events
     *
     * @param eventsRequestHandler
     */
    void setSummedEventsRequestHandler(EventsRequestHandler eventsRequestHandler);

    /**
     * Set a custom event to trigger when a si,,ed events request gets a response from the Backtrace API
     *
     * @param callback The custom event to trigger on an API response for a unique events request
     */
    void setSummedEventsOnServerResponse(EventsOnServerResponseEventListener callback);

    /**
     * Check if a request handler has been set
     *
     * @return if a custom request handler is set
     */
    boolean usesCustomRequestHandler();
}