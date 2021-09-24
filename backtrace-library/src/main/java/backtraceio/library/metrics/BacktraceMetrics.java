package backtraceio.library.metrics;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Metrics;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;

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
    protected Map<String, Object> customAttributes;

    /**
     * The application context
     */
    protected Context context;

    /**
     * Backtrace metrics settings
     */
    protected BacktraceMetricsSettings settings;

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
     * User custom request method
     */
    private RequestHandler requestHandler = null;

    /**
     * Create new Backtrace metrics instance
     *
     * @param context          Application context
     * @param customAttributes Backtrace client
     * @param settings
     */
    public BacktraceMetrics(Context context, Map<String, Object> customAttributes, BacktraceMetricsSettings settings) {
        this.context = context;
        this.customAttributes = customAttributes;
        this.settings = settings;
    }

    @Override
    public void startMetricsEventHandlers(Api backtraceApi) {
        uniqueEventsHandler = backtraceApi.enableUniqueEvents(context, customAttributes, settings);
        summedEventsHandler = backtraceApi.enableSummedEvents(context, customAttributes, settings);
    }

    String getStartupUniqueEventName() {
        return this.startupUniqueEventName;
    }

    void setStartupUniqueEventName(String startupUniqueEventName) {
        this.startupUniqueEventName = startupUniqueEventName;
    }

    public String getBaseUrl() {
        return this.settings.getBaseUrl();
    }

    public ConcurrentLinkedDeque<UniqueEvent> getUniqueEvents() {
        return this.uniqueEventsHandler.events;
    }

    public ConcurrentLinkedDeque<SummedEvent> getSummedEvents() {
        return this.summedEventsHandler.events;
    }

    /**
     * Send startup event to Backtrace
     */
    public void sendStartupEvent() {
        uniqueEventsHandler.sendStartupEvent(startupUniqueEventName);
        summedEventsHandler.sendStartupEvent(startupSummedEventName);
    }

    /**
     * Send all outgoing messages (unique and summed) currently queued
     */
    public void send() {
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
        if (!shouldProcessEvent(attributeName)) {
            BacktraceLogger.w(LOG_TAG, "Skipping report: Reached store limit or event has empty name.");
            return false;
        }

        Map<String, Object> localAttributes = new HashMap<String, Object>();

        if (attributes != null) {
            localAttributes.putAll(attributes);
        }

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        localAttributes.putAll(backtraceAttributes.getAllBacktraceAttributes());

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        localAttributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        // validate if unique event attribute is available and
        // prevent undefined attributes
        Object value = localAttributes.get(attributeName);
        if (!BacktraceStringHelper.isObjectValidString(value)) {
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

        UniqueEvent uniqueEvent = new UniqueEvent(attributeName, System.currentTimeMillis() / 1000, localAttributes);
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
            BacktraceLogger.w(LOG_TAG, "Skipping report: Reached store limit or event has empty name.");
            return false;
        }

        Map<String, Object> localAttributes = new HashMap<String, Object>();
        if (attributes != null) {
            localAttributes.putAll(attributes);
        }

        SummedEvent summedEvent = new SummedEvent(metricGroupName, System.currentTimeMillis() / 1000, localAttributes);
        summedEventsHandler.events.addLast(summedEvent);
        if (count() == maximumNumberOfEvents) {
            uniqueEventsHandler.send();
            summedEventsHandler.send();
        }

        return true;
    }

    /**
     * Determine if Backtrace Metrics can add next event to store
     *
     * @param name event name
     * @return true if we should process this event, otherwise false
     */
    private boolean shouldProcessEvent(String name) {
        return !(BacktraceStringHelper.isNullOrEmpty(name)) && (maximumNumberOfEvents == 0 || (count() + 1 <= maximumNumberOfEvents));
    }
}
