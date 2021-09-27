package backtraceio.library.interfaces;

import android.content.Context;

import java.util.Map;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.metrics.EventsOnServerResponseEventListener;
import backtraceio.library.metrics.EventsRequestHandler;
import backtraceio.library.metrics.SummedEventsHandler;
import backtraceio.library.metrics.SummedEventsPayload;
import backtraceio.library.metrics.UniqueEventsHandler;
import backtraceio.library.metrics.UniqueEventsPayload;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceMetricsSettings;

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
     * @param context          The application context
     * @param customAttributes Backtrace base object instance
     * @param settings
     * @return Reference to the created UniqueEventsHandler
     */
    UniqueEventsHandler enableUniqueEvents(Context context, Map<String, Object> customAttributes, BacktraceMetricsSettings settings);

    /**
     * Create metrics events handler for summed events
     *
     * @param context          The application context
     * @param customAttributes Backtrace base object instance
     * @param settings
     * @return Reference to the created SummedEventsHandler
     */
    SummedEventsHandler enableSummedEvents(Context context, Map<String, Object> customAttributes, BacktraceMetricsSettings settings);

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
}