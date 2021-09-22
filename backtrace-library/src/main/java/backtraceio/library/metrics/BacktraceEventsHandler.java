package backtraceio.library.metrics;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceMathHelper;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.services.BacktraceHandlerThread;
import backtraceio.library.services.BacktraceReportSender;

public abstract class BacktraceEventsHandler extends Handler {

    private final static transient String LOG_TAG = BacktraceEventsHandler.class.getSimpleName();

    protected final BacktraceHandlerThread backtraceHandlerThread;

    /**
     * Number of events in the queue
     */
    public int getCount() {
        return events.size();
    }

    /**
     * Maximum number of events in store. If number of events in store hit the limit
     * BacktraceMetrics instance will send data to Backtrace.
     */
    private int maximumNumberOfEvents = 350;

    public void setMaximumNumberOfEvents(int maximumNumberOfEvents) {
        this.maximumNumberOfEvents = maximumNumberOfEvents;
    }

    public int getMaximumNumberOfEvents() {
        return this.maximumNumberOfEvents;
    }

    /**
     * Time between retries if metrics submission fails
     */
    private final int timeBetweenRetriesMillis;

    /**
     * List of events in the event queue
     */
    protected ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<Event>();

    /**
     * Http client
     */
    private final Api api;

    /**
     * Submission url
     */
    private final String submissionUrl;

    /**
     * User provided custom attributes
     */
    protected final Map<String, Object> customAttributes;

    /**
     * The application context. We need this in our derived classes to get the BacktraceAttributes
     */
    protected final Context context;

    /**
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
        if (backtraceHandlerThread.getLooper() == null) {
            throw new NullPointerException("Expected nonnull looper, this should not happen");
        }
        this.context = context;
        this.customAttributes = customAttributes;
        this.backtraceHandlerThread = backtraceHandlerThread;
        this.api = api;
        this.submissionUrl = settings.getBaseUrl() + "/" + urlPrefix + "/submit?token=" + settings.getToken() + "&universe=" + settings.getUniverseName();
        this.timeBetweenRetriesMillis = settings.getTimeBetweenRetriesMillis();

        long timeIntervalMillis = settings.getTimeIntervalMillis();

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

    public abstract void sendStartupEvent(String eventName);

    public void send() {
        sendEvents(events);
    }

    private void sendEvents(ConcurrentLinkedDeque<Event> events) {
        if (events == null) {
            return;
        }
        if (events.size() == 0) {
            return;
        }
        try {
            EventsPayload payload = getEventsPayload();
            api.sendEventsPayload(payload);
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "Could not create events payload for metrics submission");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        final BacktraceHandlerInputEvents input = (BacktraceHandlerInputEvents) msg.obj;
        EventsResult result;

        if (input.eventsRequestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
            result = input.eventsRequestHandler.onRequest(input.payload);
        } else {
            BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
            String json = BacktraceSerializeHelper.toJson(input.payload);
            result = BacktraceReportSender.sendEvents(submissionUrl, json, input.payload, input.serverErrorEventListener);
        }

        if (input.eventsOnServerResponseEventListener != null) {
            BacktraceLogger.d(LOG_TAG, "Processing result using custom event");
            input.eventsOnServerResponseEventListener.onEvent(result);
        }

        int statusCode = result.getStatusCode();
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
                    EventsPayload payload = input.payload;
                    payload.setDroppedEvents(numRetries);
                    api.sendEventsPayload(payload);
                }
            }, calculateNextRetryTime(numRetries));
        }
    }

    protected void onMaximumAttemptsReached(ConcurrentLinkedDeque<Event> events) {
        return;
    }

    protected abstract EventsPayload getEventsPayload();

    private long calculateNextRetryTime(int numRetries) {
        final int jitterFraction = 1;
        final int backoffBase = 10;
        double value = timeBetweenRetriesMillis * Math.pow(backoffBase, numRetries - 1);
        double retryLower = BacktraceMathHelper.clamp(value, 0, BacktraceMetrics.maxTimeBetweenRetriesMillis);
        double retryUpper = retryLower + retryLower * jitterFraction;
        return (long) BacktraceMathHelper.uniform(retryLower, retryUpper);
    }
}
