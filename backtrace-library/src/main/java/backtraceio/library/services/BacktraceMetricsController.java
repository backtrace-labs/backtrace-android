package backtraceio.library.services;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.UniqueEvent;

public class BacktraceMetricsController {
    private static final transient String LOG_TAG = BacktraceMetricsController.class.getSimpleName();
    private final BacktraceMetrics metrics;
    private final boolean areMetricsAvailable;
    private final Api backtraceApi;
    public BacktraceMetricsController(Context context, Map<String, Object> attributes, Api backtraceApi, BacktraceCredentials credentials) {
        this.areMetricsAvailable = areMetricsAvailable(credentials);
        this.backtraceApi = backtraceApi;
        this.metrics = areMetricsAvailable ? new BacktraceMetrics(context, attributes, backtraceApi, credentials) : null;
    }

    /**
     * Enables gathering metrics
     */
    public void enableMetrics() throws Exception {
        validIfMetricsAvailable();
        this.metrics.enable();
    }

    /**
     * Enables gathering metrics
     * @param defaultUniqueEventName custom session user identifier
     */
    public void enableMetrics(String defaultUniqueEventName) throws Exception{
        validIfMetricsAvailable();
        this.metrics.enable(defaultUniqueEventName);
    }
    public void validIfMetricsAvailable() throws Exception {
        if (this.metrics == null) {
            BacktraceLogger.e(LOG_TAG, "Metrics not available");
            throw new Exception("Metrics not available");
        }
    }
    /**
     * Enables gathering metrics
     *
     * @param settings for Backtrace metrics
     */
    public void enableMetrics(BacktraceMetricsSettings settings) throws Exception {
        validIfMetricsAvailable();
        this.metrics.enable(settings);
    }

    /**
     * Enables gathering metrics
     *
     * @param settings for Backtrace metrics
     * @param defaultUniqueEventName custom session user identifier
     */
    public void enableMetrics(BacktraceMetricsSettings settings, String defaultUniqueEventName) throws Exception {
        validIfMetricsAvailable();
        this.metrics.enable(settings, defaultUniqueEventName);
    }

    private static boolean areMetricsAvailable(BacktraceCredentials credentials) {
        return credentials.isBacktraceServerUrl();
    }

    /**
     * Custom callback to be executed on server response to a unique events submission request
     *
     * @param callback object with method which will be executed
     */
    public void setUniqueEventsOnServerResponse(EventsOnServerResponseEventListener callback) throws Exception{
        validIfMetricsAvailable();
        backtraceApi.setUniqueEventsOnServerResponse(callback);
    }

    /**
     * Custom callback to be executed on server response to a summed events submission request
     *
     * @param callback object with method which will be executed
     */
    public void setSummedEventsOnServerResponse(EventsOnServerResponseEventListener callback) throws Exception {
        validIfMetricsAvailable();
        backtraceApi.setSummedEventsOnServerResponse(callback);
    }

    /**
     * Custom request handler for sending Backtrace unique events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    public void setUniqueEventsRequestHandler(EventsRequestHandler eventsRequestHandler) throws Exception{
        validIfMetricsAvailable();
        backtraceApi.setUniqueEventsRequestHandler(eventsRequestHandler);
    }

    /**
     * Custom request handler for sending Backtrace summed events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    public void setSummedEventsRequestHandler(EventsRequestHandler eventsRequestHandler) throws Exception{
        validIfMetricsAvailable();
        backtraceApi.setSummedEventsRequestHandler(eventsRequestHandler);
    }

    private void startMetricsEventHandlers() {
        validIfMetricsAvailable();
        this.metrics.setUniqueEventsHandler(this.backtraceApi.enableUniqueEvents(this.metrics));
        this.metrics.setSummedEventsHandler(backtraceApi.enableSummedEvents(this.metrics));
    }

    protected String getStartupUniqueEventName() {
        return this.startupUniqueEventName;
    }

    public void setStartupUniqueEventName(String startupUniqueEventName) {
        this.startupUniqueEventName = startupUniqueEventName;
    }

    public String getBaseUrl() {
        return this.settings.getBaseUrl();
    }

    /**
     * Send startup event to Backtrace
     */
    public void sendStartupEvent() {
        addUniqueEvent(startupUniqueEventName);
        addSummedEvent(startupSummedEventName);
        uniqueEventsHandler.send();
        summedEventsHandler.send();
    }

    /**
     * Send all outgoing messages (unique and summed) currently queued
     */
    public void send() throws Exception {
        validIfMetricsAvailable();
        this.metrics.uniqueEventsHandler.send();
        this.metrics.summedEventsHandler.send();
    }

    /**
     * Add a unique event to the next Backtrace Metrics request
     *
     * @param attributeName Attribute name
     * @return true if success
     */
    public boolean addUniqueEvent(String attributeName) throws Exception {
        validIfMetricsAvailable();
        return addUniqueEvent(attributeName, null);
    }

    /**
     * Add a unique event to the next Backtrace Metrics request
     *
     * @param attributeName Attribute name
     * @param attributes    Event attributes
     * @return true if success
     */
    public boolean addUniqueEvent(String attributeName, Map<String, Object> attributes) {
        validIfMetricsAvailable();
        return this.metrics.addUniqueEvent(attributeName, attributes);
    }

    /**
     * Set the maximum number of events to store. Once the maximum is hit we will send events to
     * the API
     *
     * @param maximumNumberOfEvents Maximum number of events to store before sending events to
     *                              the API
     */
    public void setMaximumNumberOfEvents(int maximumNumberOfEvents) throws Exception {
        validIfMetricsAvailable();
        this.metrics.setMaximumNumberOfEvents(maximumNumberOfEvents);

        this.metrics.uniqueEventsHandler.setMaximumNumberOfEvents(maximumNumberOfEvents);
        this.metrics.summedEventsHandler.setMaximumNumberOfEvents(maximumNumberOfEvents);
    }

    /**
     * Get number of stored events
     *
     * @return number of stored events
     */
    public int count() throws Exception {
        validIfMetricsAvailable();
        return this.metrics.count();
        return getUniqueEvents().size() + getSummedEvents().size();
    }

    /**
     * Add a summed event to the next Backtrace Metrics request
     *
     * @param metricGroupName
     * @return true if success
     */
    public boolean addSummedEvent(String metricGroupName) {
        return addSummedEvent(metricGroupName, null);
    }

    /**
     * Add a summed event to the next Backtrace Metrics request
     *
     * @param metricGroupName
     * @param attributes
     * @return true if success
     */
    public boolean addSummedEvent(String metricGroupName, Map<String, Object> attributes) {
        if (!shouldProcessEvent(metricGroupName)) {
            BacktraceLogger.w(LOG_TAG, "Skipping report");
            return false;
        }

        Map<String, Object> localAttributes = new HashMap<String, Object>();
        if (attributes != null) {
            localAttributes.putAll(attributes);
        }

        SummedEvent summedEvent = new SummedEvent(metricGroupName, BacktraceTimeHelper.getTimestampSeconds(), localAttributes);
        summedEventsHandler.events.addLast(summedEvent);
        if (count() == maximumNumberOfEvents) {
            uniqueEventsHandler.send();
            summedEventsHandler.send();
        }

        return true;
    }

    public ConcurrentLinkedDeque<UniqueEvent> getUniqueEvents() {
        return this.uniqueEventsHandler.events;
    }

    public ConcurrentLinkedDeque<SummedEvent> getSummedEvents() {
        return this.summedEventsHandler.events;
    }
}
