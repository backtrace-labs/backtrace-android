package backtraceio.library.services;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Metrics;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.UniqueEvent;

public final class BacktraceMetrics implements Metrics {

    /**
     * Default time interval in min
     */
    public final static int defaultTimeIntervalInMin = 30;

    /**
     * Default time interval in milliseconds
     */
    public final static long defaultTimeIntervalMs = defaultTimeIntervalInMin * 60 * 1000;

    /**
     * Maximum number of attempts
     */
    public final static int maxNumberOfAttempts = 3;

    /**
     * Default time between retries in milliseconds
     */
    public final static int defaultTimeBetweenRetriesMs = 10000;

    /**
     * Maximum time between requests in milliseconds
     */
    public final static int maxTimeBetweenRetriesMs = 5 * 60 * 1000;

    /**
     * Default submission url
     */
    public final static String defaultBaseUrl = "https://events.backtrace.io/api";

    private static final transient String LOG_TAG = BacktraceMetrics.class.getSimpleName();

    /**
     * Default unique event name that will be generated on app startup
     */
    public final String defaultUniqueEventName = "guid";

    /**
     * Name of the summed event that will be generated on app startup
     */
    private final String startupSummedEventName = "Application Launches";

    /**
     * Is gathering and sending metrics enabled, supported only on Backtrace servers
     */
    public boolean enabled;

    /**
     * Unique Events handler
     */
    public UniqueEventsHandler uniqueEventsHandler;

    /**
     * Summed Events handler
     */
    public SummedEventsHandler summedEventsHandler;

    /**
     * Custom attributes provided by the user to BacktraceBase
     */
    Map<String, Object> customReportAttributes;

    /**
     * The application context
     */
    private final Context context;

    /**
     * Backtrace metrics settings
     */
    BacktraceMetricsSettings settings = null;

    /**
     * Name of the unique event that will be generated on app startup
     */
    private String startupUniqueEventName = defaultUniqueEventName;

    /**
     * Maximum number of events in store. If number of events in store hit the limit
     * BacktraceMetrics instance will send data to Backtrace.
     */
    private int maximumNumberOfEvents = 350;

    /**
     * Backtrace API class for metrics sending
     */
    private final Api backtraceApi;

    /**
     * Backtrace API credentials
     */
    private final BacktraceCredentials credentials;

    /**
     * Create new Backtrace metrics instance
     *
     * @param context                Application context
     * @param customReportAttributes Backtrace client custom report attributes (must be nonnull)
     * @param backtraceApi           Backtrace API for metrics sending
     */
    public BacktraceMetrics(Context context, @NotNull Map<String, Object> customReportAttributes, Api backtraceApi, BacktraceCredentials credentials) {
        this.context = context;
        this.customReportAttributes = customReportAttributes;
        this.backtraceApi = backtraceApi;
        this.credentials = credentials;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Enables metrics with BacktraceClient's credentials.
     */
    public void enable() {
        enable(new BacktraceMetricsSettings(this.credentials), defaultUniqueEventName);
    }

    /**
     * Enables metrics with BacktraceClient's credentials and overrides default unique event name.
     */
    public void enable(String defaultUniqueEventName) {
        enable(new BacktraceMetricsSettings(this.credentials, defaultUniqueEventName));
    }

    /**
     * Enable metrics
     *
     * @param settings for Backtrace metrics
     */
    public void enable(BacktraceMetricsSettings settings) {
        enable(settings, defaultUniqueEventName);
    }

    public void enable(BacktraceMetricsSettings settings, String uniqueEventName) {
        if (!settings.isBacktraceServer()) {
            throw new IllegalArgumentException("Unsupported metrics server " + settings.getBaseUrl());
        }

        if (uniqueEventName == null || uniqueEventName.length() == 0) {
            throw new IllegalArgumentException("Unique event name must be defined!");
        }

        setStartupUniqueEventName(uniqueEventName);
        this.settings = settings;
        this.enabled = true;
        try {
            startMetricsEventHandlers(backtraceApi);
            sendStartupEvent();
            BacktraceLogger.d(LOG_TAG, "Metrics enabled");
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "Could not enable metrics, exception " + e.getMessage());
        }
    }

    private void verifyIfMetricsAvailable() {
        if (!enabled) {
            throw new IllegalArgumentException("Unique event name must be defined!");
        }
    }

    private void startMetricsEventHandlers(Api backtraceApi) {
        verifyIfMetricsAvailable();
        uniqueEventsHandler = backtraceApi.enableUniqueEvents(this);
        summedEventsHandler = backtraceApi.enableSummedEvents(this);
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
        verifyIfMetricsAvailable();
        addUniqueEvent(startupUniqueEventName);
        addSummedEvent(startupSummedEventName);
        uniqueEventsHandler.send();
        summedEventsHandler.send();
    }

    /**
     * Send all outgoing messages (unique and summed) currently queued
     */
    public void send() {
        verifyIfMetricsAvailable();
        uniqueEventsHandler.send();
        summedEventsHandler.send();
    }

    /**
     * Add a unique event to the next Backtrace Metrics request
     *
     * @param attributeName Attribute name
     * @return true if success
     */
    public boolean addUniqueEvent(String attributeName) {
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
        verifyIfMetricsAvailable();

        if (!shouldProcessEvent(attributeName)) {
            BacktraceLogger.w(LOG_TAG, "Skipping report");
            return false;
        }

        Map<String, Object> localAttributes = createLocalAttributes(attributes);

        // validate if unique event attribute is available and
        // prevent undefined attributes
        Object value = localAttributes.get(attributeName);
        if (!BacktraceStringHelper.isObjectNotNullOrNotEmptyString(value)) {
            BacktraceLogger.w(LOG_TAG, "Attribute name for Unique Event is not available in attribute scope");
            return false;
        }
        // skip already defined unique events
        for (UniqueEvent uniqueEvent : uniqueEventsHandler.events) {
            if (uniqueEvent.getName().equals(attributeName)) {
                BacktraceLogger.w(LOG_TAG, "Already defined unique event with this attribute name, skipping");
                return false;
            }
        }

        UniqueEvent uniqueEvent = new UniqueEvent(attributeName, BacktraceTimeHelper.getTimestampSeconds(), localAttributes);
        uniqueEventsHandler.events.addLast(uniqueEvent);

        if (count() == maximumNumberOfEvents) {
            uniqueEventsHandler.send();
            summedEventsHandler.send();
        }
        return true;
    }

    /**
     * Set the maximum number of events to store. Once the maximum is hit we will send events to
     * the API
     *
     * @param maximumNumberOfEvents Maximum number of events to store before sending events to
     *                              the API
     */
    @Override
    public void setMaximumNumberOfEvents(int maximumNumberOfEvents) {
        this.maximumNumberOfEvents = maximumNumberOfEvents;
        uniqueEventsHandler.setMaximumNumberOfEvents(maximumNumberOfEvents);
        summedEventsHandler.setMaximumNumberOfEvents(maximumNumberOfEvents);
    }

    /**
     * Get number of stored events
     *
     * @return number of stored events
     */
    public int count() {
        return getUniqueEvents().size() + getSummedEvents().size();
    }

    /**
     * Add a summed event to the next Backtrace Metrics request
     *
     * @param metricGroupName
     * @return true if success
     */
    public boolean addSummedEvent(String metricGroupName) {
        verifyIfMetricsAvailable();

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
        verifyIfMetricsAvailable();

        if (!shouldProcessEvent(metricGroupName)) {
            BacktraceLogger.w(LOG_TAG, "Skipping report");
            return false;
        }

        Map<String, Object> localAttributes = new HashMap<>();
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

    /**
     * Custom request handler for sending Backtrace unique events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    public void setUniqueEventsRequestHandler(EventsRequestHandler eventsRequestHandler) {
        backtraceApi.setUniqueEventsRequestHandler(eventsRequestHandler);
    }

    /**
     * Custom request handler for sending Backtrace summed events to server
     *
     * @param eventsRequestHandler object with method which will be executed
     */
    public void setSummedEventsRequestHandler(EventsRequestHandler eventsRequestHandler) {
        backtraceApi.setSummedEventsRequestHandler(eventsRequestHandler);
    }

    /**
     * Determine if Backtrace Metrics can add next event to store
     *
     * @param name event name
     * @return true if we should process this event, otherwise false
     */
    private boolean shouldProcessEvent(String name) {
        if (BacktraceStringHelper.isNullOrEmpty(name)) {
            BacktraceLogger.e(LOG_TAG, "Cannot process event, attribute name is null or empty");
            return false;
        }
        if (maximumNumberOfEvents > 0 && (count() + 1 > maximumNumberOfEvents)) {
            BacktraceLogger.e(LOG_TAG, "Cannot process event, reached maximum number of events: " + maximumNumberOfEvents + " events count: " + count());
            return false;
        }

        return true;
    }

    Map<String, Object> createLocalAttributes(Map<String, Object> attributes) {
        Map<String, Object> localAttributes = new HashMap<>();

        if (attributes != null) {
            localAttributes.putAll(attributes);
        }

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customReportAttributes);
        localAttributes.putAll(backtraceAttributes.getAllAttributes());

        return localAttributes;
    }

    /**
     * Custom callback to be executed on server response to a unique events submission request
     *
     * @param callback object with method which will be executed
     */
    public void setUniqueEventsOnServerResponse(EventsOnServerResponseEventListener callback) {
        backtraceApi.setUniqueEventsOnServerResponse(callback);
    }

    /**
     * Custom callback to be executed on server response to a summed events submission request
     *
     * @param callback object with method which will be executed
     */
    public void setSummedEventsOnServerResponse(EventsOnServerResponseEventListener callback) {
        backtraceApi.setSummedEventsOnServerResponse(callback);
    }
}
