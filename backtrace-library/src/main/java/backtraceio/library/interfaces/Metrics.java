package backtraceio.library.interfaces;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.metrics.EventsOnServerResponseEventListener;
import backtraceio.library.metrics.EventsRequestHandler;
import backtraceio.library.metrics.SummedEvent;
import backtraceio.library.metrics.UniqueEvent;
import backtraceio.library.models.BacktraceMetricsSettings;

public interface Metrics {

    /**
     * Enable metrics
     *
     * @param settings for Backtrace metrics
     */
    void enable(BacktraceMetricsSettings settings);

    /**
     * Send all outgoing messages (unique and summed) currently queued
     */
    void send();

    /**
     * Add a unique event, such as UserID, SteamID and other attributes that uniquely identify a
     * user. This list is persistent, meaning that events will not be removed upon Send(), as they
     * are for summed events. For non-standard unique events, server side configuration needs to be
     * done.
     * Please refer to the online documentation at https://support.backtrace.io
     *
     * @param name the name of the Unique event
     * @return true if added successfully, otherwise false.
     */
    boolean addUniqueEvent(String name);

    /**
     * Add a unique event, such as UserID, SteamID and other attributes that uniquely identify a
     * user. This list is persistent, meaning that events will not be removed upon Send(), as they
     * are for summed events. For non-standard unique events, server side configuration needs to be
     * done.
     * Please refer to the online documentation at https://support.backtrace.io
     *
     * @param name       the name of the Unique event
     * @param attributes linked attributes which will update this unique event on update
     * @return true if added successfully, otherwise false.
     */
    boolean addUniqueEvent(String name, Map<String, Object> attributes);

    /**
     * Get the pending list of unique events
     *
     * @return list of pending unique events to send
     */
    ConcurrentLinkedDeque<UniqueEvent> getUniqueEvents();

    /**
     * Adds a summed event to the outgoing queue.
     *
     * @param metricGroupName name of the metric group to be incremented. This metric group must
     *                        be configured on the server side as well.
     *                        Please refer to the online documentation at https://support.backtrace.io
     * @return true if added successfully, otherwise false.
     * @see backtraceio.library.metrics.BacktraceMetrics#send()
     */
    boolean addSummedEvent(String metricGroupName);

    /**
     * Adds a summed event to the outgoing queue.
     *
     * @param metricGroupName name of the metric group to be incremented. This metric group must
     *                        be configured on the server side as well.
     *                        Please refer to the online documentation at https://support.backtrace.io
     * @param attributes      Custom attributes to add. Will be merged with the default
     *                        attributes, with attribute values provided here overriding any defaults.
     * @return true if added successfully, otherwise false.
     * @see backtraceio.library.metrics.BacktraceMetrics#send()
     */
    boolean addSummedEvent(String metricGroupName, Map<String, Object> attributes);

    /**
     * Get the pending list of summed events
     *
     * @return list of pending summed events to send
     */
    ConcurrentLinkedDeque<SummedEvent> getSummedEvents();

    /**
     * Set the maximum number of events to store. Once the maximum is hit we will send events to
     * the API
     *
     * @param maximumNumberOfEvents Maximum number of events to store before sending events to
     *                              the API
     */
    void setMaximumNumberOfEvents(int maximumNumberOfEvents);

    /**
     * Return the total number of events in store
     *
     * @return Total number of events in store
     */
    int count();

    /**
     * Send the startup event
     */
    void sendStartupEvent();

    /**
     * Custom request handler for sending Backtrace unique events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    void setUniqueEventsRequestHandler(EventsRequestHandler eventsRequestHandler);

    /**
     * Custom request handler for sending Backtrace summed events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    void setSummedEventsRequestHandler(EventsRequestHandler eventsRequestHandler);

    /**
     * Custom callback to be executed on server response to a unique events submission request
     *
     * @param callback object with method which will be executed
     */
    void setUniqueEventsOnServerResponse(EventsOnServerResponseEventListener callback);

    /**
     * Custom callback to be executed on server response to a summed events submission request
     *
     * @param callback object with method which will be executed
     */
    void setSummedEventsOnServerResponse(EventsOnServerResponseEventListener callback);
}
