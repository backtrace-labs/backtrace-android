package backtraceio.library.metrics;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceMathHelper;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.services.BacktraceHandlerThread;
import backtraceio.library.services.BacktraceReportSender;

public abstract class BacktraceEventsHandler<T extends Event> extends Handler {

    private final static transient String LOG_TAG = BacktraceEventsHandler.class.getSimpleName();

    protected final BacktraceHandlerThread backtraceHandlerThread;

    /**
     * User provided custom attributes
     */
    protected final Map<String, Object> customAttributes;

    /**
     * The application context. We need this in our derived classes to get the BacktraceAttributes
     */
    protected final Context context;

    /**
     * Time between retries if metrics submission fails
     */
    private final int timeBetweenRetriesMillis;

    /**
     * Http client
     */
    protected final Api api;

    /**
     * Submission url
     */
    private final String submissionUrl;

    /**
     * List of events in the event queue
     */
    protected ConcurrentLinkedDeque<T> events = new ConcurrentLinkedDeque<T>();

    /**
     * Maximum number of events in store. If number of events in store hit the limit
     * BacktraceMetrics instance will send data to Backtrace.
     */
    private int maximumNumberOfEvents = 350;

    /**
     * The application name
     */
    protected String application;

    /**
     * The application version
     */
    protected String appVersion;

    /**
     * Create BacktraceEventsHandler instance
     *
     * @param context
     * @param api
     * @param backtraceHandlerThread
     * @param urlPrefix
     * @param settings
     */
    public BacktraceEventsHandler(Context context, Map<String, Object> customAttributes,
                                  Api api, final BacktraceHandlerThread backtraceHandlerThread, String urlPrefix, BacktraceMetricsSettings settings) {
        // This should always have a nonnull looper because BacktraceHandlerThread starts in the
        // constructor and getLooper blocks until the looper is ready if the thread is started
        //
        // We cannot cache the looper here since the super call has to go first (i.e: before
        // declaring and assigning a Looper variable)
        super(backtraceHandlerThread.getLooper());
        if (!backtraceHandlerThread.isAlive()) {
            throw new NullPointerException("Handler thread is not alive, this should not happen");
        }
        this.context = context;
        this.customAttributes = customAttributes;
        this.backtraceHandlerThread = backtraceHandlerThread;
        this.api = api;
        this.submissionUrl = settings.getSubmissionUrl(urlPrefix);
        this.timeBetweenRetriesMillis = settings.getTimeBetweenRetriesMillis();

        long timeIntervalMillis = settings.getTimeIntervalMillis();

        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, null);
        this.application = backtraceAttributes.getApplicationName();
        this.appVersion = backtraceAttributes.getApplicationVersionOrEmpty();

        if (timeIntervalMillis != 0) {
            final BacktraceEventsHandler handler = this;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handler.send();
                    handler.postDelayed(this, timeIntervalMillis);
                }
            }, timeIntervalMillis);
        }
    }

    /**
     * Number of events in the queue
     */
    public int getCount() {
        return events.size();
    }

    public int getMaximumNumberOfEvents() {
        return this.maximumNumberOfEvents;
    }

    public void setMaximumNumberOfEvents(int maximumNumberOfEvents) {
        this.maximumNumberOfEvents = maximumNumberOfEvents;
    }

    public abstract void sendStartupEvent(String eventName);

    public void send() {
        if (events == null || events.size() == 0) {
            return;
        }
        sendEvents(events);
    }

    protected abstract void sendEvents(ConcurrentLinkedDeque<T> events);

    protected abstract void sendEventsPayload(EventsPayload<T> payload);

    @Override
    public void handleMessage(Message msg) {
        final BacktraceHandlerInputEvents input = (BacktraceHandlerInputEvents) msg.obj;
        EventsResult result = getEventsResult(input);

        if (input.eventsOnServerResponseEventListener != null) {
            BacktraceLogger.d(LOG_TAG, "Processing result using custom event");
            input.eventsOnServerResponseEventListener.onEvent(result);
        }

        retrySendEvents(input, result.getStatusCode());
    }

    protected void onMaximumAttemptsReached(ConcurrentLinkedDeque<T> events) {
        return;
    }

    protected Map<String, Object> getAttributes() {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        Map<String, Object> attributes = backtraceAttributes.getAllAttributes();

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        return attributes;
    }

    protected abstract EventsPayload<T> getEventsPayload();

    private long calculateNextRetryTime(int numRetries) {
        final int jitterFraction = 1;
        final int backoffBase = 10;
        double value = timeBetweenRetriesMillis * Math.pow(backoffBase, numRetries - 1);
        double retryLower = BacktraceMathHelper.clamp(value, 0, BacktraceMetrics.maxTimeBetweenRetriesMs);
        double retryUpper = retryLower + retryLower * jitterFraction;
        return (long) BacktraceMathHelper.uniform(retryLower, retryUpper);
    }

    private EventsResult getEventsResult(BacktraceHandlerInputEvents input) {
        EventsResult result;

        if (input.eventsRequestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
            result = input.eventsRequestHandler.onRequest(input.payload);
        } else {
            BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
            String json = BacktraceSerializeHelper.toJson(input.payload);
            result = BacktraceReportSender.sendEvents(submissionUrl, json, input.payload, input.serverErrorEventListener);
        }

        return result;
    }

    private void retrySendEvents(BacktraceHandlerInputEvents input, int statusCode) {
        if (statusCode > HttpURLConnection.HTTP_NOT_IMPLEMENTED && statusCode != HttpURLConnection.HTTP_VERSION) {
            int numRetries = ++input.payload.numRetries;
            if (numRetries >= BacktraceMetrics.maxNumberOfAttempts || timeBetweenRetriesMillis == 0) {
                onMaximumAttemptsReached(input.payload.getEvents());
                return;
            }
            final BacktraceEventsHandler handler = this;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    EventsPayload<T> payload = input.payload;
                    payload.setDroppedEvents(numRetries);
                    sendEventsPayload(payload);
                }
            }, calculateNextRetryTime(numRetries));
        }
    }
}
