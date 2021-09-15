package backtraceio.library.metrics;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Metrics;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceAttributes;

public final class BacktraceMetrics implements Metrics {

    private static final transient String LOG_TAG = BacktraceMetrics.class.getSimpleName();

    /**
     * Default time interval in min
     */
    public final static int defaultTimeIntervalInMin = 30;

    /**
     * Default time interval in milliseconds
     */
    public final static long defaultTimeIntervalMillis = defaultTimeIntervalInMin * 60 * 1000;

    /**
     * Default unique event name that will be generated on app startup
     */
    public final String defaultUniqueEventName = "guid";

    /**
     * Name of the unique event that will be generated on app startup
     */
    private String startupUniqueEventName = defaultUniqueEventName;

    /**
     * Maximum number of events in store. If number of events in store hit the limit
     * BacktraceMetrics instance will send data to Backtrace.
     */
    private int maximumEvents = 350;

    /**
     * Name of the summed event that will be generated on app startup
     */
    private final String startupSummedEventName = "Application Launches";

    /**
     * Maximum number of attempts
     */
    public final static int maxNumberOfAttempts = 3;

    /**
     * Default time between retries in seconds
     */
    public final static int defaultTimeBetweenRetriesMillis = 10000;

    /**
     * Maximum time between requests in minutes
     */
    public final static int maxTimeBetweenRetriesMillis = 5 * 60 * 1000;

    /**
     * Submission url
     */
    private String baseUrl;

    /**
     * Default submission url
     */
    public final static String defaultBaseUrl = "https://events.backtrace.io/api";

    /**
     * User custom request method
     */
    private RequestHandler requestHandler = null;

    /**
     * Should http client ignore ssl validation?
     */
    private boolean ignoreSslValidation;

    /**
     * Unique Events handler
     */
    public UniqueEventsHandler uniqueEventsHandler;

    /**
     * Summed Events handler
     */
    public SummedEventsHandler summedEventsHandler;

    /**
     * How often we will send data to Backtrace
     */
    private final long timeIntervalMillis;

    /**
     * Base time between retries
     */
    private final int timeBetweenRetriesMillis;

    /**
     * Custom attributes provided by the user to BacktraceBase
     */
    protected Map<String, Object> customAttributes;

    /**
     * The application context
     */
    protected Context context;

    /**
     * The Backtrace submission token
     */
    private final String token;

    /**
     * The Backtrace universe associated with the collected metrics
     */
    private final String universe;

    /**
     * Create new Backtrace metrics instance
     * @param context                   Application context
     * @param customAttributes          Backtrace client
     * @param baseUrl                   Events submission base URL
     * @param universe                  Backtrace universe
     * @param token                     Backtrace submission token
     * @param timeIntervalMillis        Time interval between metrics submissions in ms, 0 disables auto-send
     * @param timeBetweenRetriesMillis  Base time between retries in ms, 0 disables retry
     */
    public BacktraceMetrics(Context context, Map<String, Object> customAttributes,
                            String baseUrl,
                            String universe, String token, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.customAttributes = customAttributes;
        this.timeIntervalMillis = timeIntervalMillis;
        this.token = token;
        this.universe = universe;
        this.timeBetweenRetriesMillis = timeBetweenRetriesMillis;
    }

    @Override
    public void startMetricsEventHandlers(Api backtraceApi) {
        uniqueEventsHandler = backtraceApi.enableUniqueEvents(context, baseUrl, customAttributes, universe, token, timeIntervalMillis, timeBetweenRetriesMillis);
        summedEventsHandler = backtraceApi.enableSummedEvents(context, baseUrl, customAttributes, universe, token, timeIntervalMillis, timeBetweenRetriesMillis);
    }

    void setStartupUniqueEventName(String StartupUniqueEventName) {
        this.startupUniqueEventName = StartupUniqueEventName;
    }

    String getStartupUniqueEventName() {
        return this.startupUniqueEventName;
    }

    void setMaximumEvents(int MaximumEvents) {
        this.maximumEvents = MaximumEvents;
    }

    int getMaximumEvents() {
        return this.maximumEvents;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    String getBaseUrl() {
        return this.baseUrl;
    }

    // TODO: Finish implementation
    void setIgnoreSslValidation(boolean ignoreSslValidation) {
        // RequestHandler.ignoreSslValidation = value;
        // this.ignoreSslValidation = ignoreSslValidation;
    }

    // TODO: Finish implementation
    boolean getIgnoreSslValidation() {
        // return RequestHandler.IgnoreSslValidation;
        // return this.IgnoreSslValidation;
        return false;
    }

    @SuppressWarnings("unchecked, rawtypes")
    public ConcurrentLinkedDeque<UniqueEvent> getUniqueEvents() {
        for (Event event : uniqueEventsHandler.events) {
            if (!(event instanceof UniqueEvent)) {
                BacktraceLogger.e(LOG_TAG, "UniqueEventsHandler contains an event of type " + event.getClass() + " this is a bug");
                return new ConcurrentLinkedDeque<>();
            }
        }
        return (ConcurrentLinkedDeque) this.uniqueEventsHandler.events;
    }

    @SuppressWarnings("unchecked, rawtypes")
    public ConcurrentLinkedDeque<SummedEvent> getSummedEvents() {
        for (Event event : summedEventsHandler.events) {
            if (!(event instanceof SummedEvent)) {
                BacktraceLogger.e(LOG_TAG, "SummedEventsHandler contains an event of type " + event.getClass() + " this is a bug");
                return new ConcurrentLinkedDeque<>();
            }
        }
        return (ConcurrentLinkedDeque) this.summedEventsHandler.events;
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
     * @param attributeName Attribute name
     * @return true if success
     */
    public boolean addUniqueEvent(String attributeName) {
        return addUniqueEvent(attributeName, null);
    }

    /**
     * Add a unique event to the next Backtrace Metrics request
     * @param attributeName Attribute name
     * @param attributes    Event attributes
     * @return true if success
     */
    public boolean addUniqueEvent(String attributeName, Map<String, Object> attributes) {
        if (!shouldProcessEvent(attributeName))
        {
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
        if (localAttributes.get(attributeName) == null || localAttributes.get(attributeName).toString().trim().isEmpty())
        {
            BacktraceLogger.w(LOG_TAG, "Attribute name for Unique Event is not available in attribute scope");
            return false;
        }
        // skip already defined unique events
        for (Event event : uniqueEventsHandler.events) {
            if (!(event instanceof UniqueEvent)) {
                BacktraceLogger.e(LOG_TAG, "An event which is not a UniqueEvent found in UniqueEvents queue, this should not happen");
                continue;
            }
            UniqueEvent uniqueEvent = (UniqueEvent) event;
            if (uniqueEvent.getName().equals(attributeName)) {
                BacktraceLogger.w(LOG_TAG, "Already defined unique event with this attribute name, skipping");
                return false;
            }
        }

        UniqueEvent uniqueEvent = new UniqueEvent(attributeName, System.currentTimeMillis() / 1000, localAttributes);
        uniqueEventsHandler.events.addLast(uniqueEvent);

        if (count() == maximumEvents) {
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
        this.maximumEvents = maximumNumberOfEvents;
        uniqueEventsHandler.setMaxNumEvents(maximumNumberOfEvents);
        summedEventsHandler.setMaxNumEvents(maximumNumberOfEvents);
    }

    /**
     * Get number of stored events
     * @return number of stored events
     */
    public int count() {
        return getUniqueEvents().size() + getSummedEvents().size();
    }

    /**
     * Add a summed event to the next Backtrace Metrics request
     * @param metricGroupName
     * @return true if success
     */
    public boolean addSummedEvent(String metricGroupName) {
        return addSummedEvent(metricGroupName, null);
    }

    /**
     * Add a summed event to the next Backtrace Metrics request
     * @param metricGroupName
     * @param attributes
     * @return true if success
     */
    public boolean addSummedEvent(String metricGroupName, Map<String, Object> attributes) {
        if (!shouldProcessEvent(metricGroupName))
        {
            BacktraceLogger.w(LOG_TAG, "Skipping report: Reached store limit or event has empty name.");
            return false;
        }

        Map<String, Object> localAttributes = new HashMap<String, Object>();
        if (attributes != null) {
            localAttributes.putAll(attributes);
        }

        SummedEvent summedEvent = new SummedEvent(metricGroupName, System.currentTimeMillis() / 1000, localAttributes);
        summedEventsHandler.events.addLast(summedEvent);
        if (count() == maximumEvents) {
            uniqueEventsHandler.send();
            summedEventsHandler.send();
        }

        return true;
    }

    /**
     * Determine if Backtrace Metrics can add next event to store
     * @param name event name
     * @return true if we should process this event, otherwise false
     */
    private boolean shouldProcessEvent(String name) {
        return !(name == null || name.trim().isEmpty()) && (maximumEvents == 0 || (count() + 1 <= maximumEvents));
    }
}
