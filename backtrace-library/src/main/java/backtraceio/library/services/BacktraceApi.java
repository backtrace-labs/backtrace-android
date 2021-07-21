package backtraceio.library.services;

import android.content.Context;

import java.util.Map;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.metrics.BacktraceHandlerInputEvents;
import backtraceio.library.metrics.EventsOnServerResponseEventListener;
import backtraceio.library.metrics.EventsPayload;
import backtraceio.library.metrics.EventsRequestHandler;
import backtraceio.library.metrics.SummedEventsHandler;
import backtraceio.library.metrics.SummedEventsPayload;
import backtraceio.library.metrics.UniqueEventsHandler;
import backtraceio.library.metrics.UniqueEventsPayload;
import backtraceio.library.models.BacktraceData;

/**
 * Backtrace Api class that allows to send data to Backtrace endpoints
 */
public class BacktraceApi implements Api {

    private final static transient String LOG_TAG = BacktraceApi.class.getSimpleName();

    private transient BacktraceHandlerThread threadSender;

    /**
     * URL to report submission endpoint
     */
    private String reportSubmissionUrl;

    /**
     * URL to unique events endpoint
     */
    private String uniqueEventsSubmissionUrl;

    /**
     * URL to summed events endpoint
     */
    private String summedEventsSubmissionUrl;

    /**
     * Event triggered when server respond with error
     */
    private OnServerErrorEventListener onServerError = null;

    /**
     * User custom request method
     */
    private RequestHandler requestHandler = null;

    /**
     * User custom unique events submission request method
     */
    private EventsRequestHandler uniqueEventsRequestHandler = null;

    /**
     * User custom summed events submission request method
     */
    private EventsRequestHandler summedEventsRequestHandler = null;

    /**
     * User custom unique events response listener method
     */
    private EventsOnServerResponseEventListener uniqueEventsServerResponse = null;

    /**
     * User custom summed events response listener method
     */
    private EventsOnServerResponseEventListener summedEventsServerResponse = null;

    @Override
    public void setUniqueEventsRequestHandler(EventsRequestHandler uniqueEventsRequestHandler) {
        this.uniqueEventsRequestHandler = uniqueEventsRequestHandler;
    }

    public void setSummedEventsRequestHandler(EventsRequestHandler summedEventsRequestHandler) {
        this.summedEventsRequestHandler = summedEventsRequestHandler;
    }

    public void setUniqueEventsOnServerResponse(EventsOnServerResponseEventListener callback) {
        this.uniqueEventsServerResponse = callback;
    }

    public void setSummedEventsOnServerResponse(EventsOnServerResponseEventListener callback) {
        this.summedEventsServerResponse = callback;
    }

    /**
     * Create a new instance of Backtrace API
     *
     * @param credentials API credentials
     */
    public BacktraceApi(BacktraceCredentials credentials) {
        if (credentials == null) {
            BacktraceLogger.e(LOG_TAG, "BacktraceCredentials parameter passed to BacktraceApi " +
                    "constructor is null");
            throw new IllegalArgumentException("BacktraceCredentials cannot be null");
        }
        this.reportSubmissionUrl = credentials.getSubmissionUrl().toString();

        threadSender = new BacktraceHandlerThread(BacktraceHandlerThread.class.getSimpleName(),
                this.reportSubmissionUrl);
    }

    public void setOnServerError(OnServerErrorEventListener onServerError) {
        this.onServerError = onServerError;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public UniqueEventsHandler enableUniqueEvents(Context context, Map<String, Object> customAttributes, String universeName, String token, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        return threadSender.createUniqueEventsHandler(context, customAttributes, universeName, token, this, timeIntervalMillis, timeBetweenRetriesMillis);
    }

    @Override
    public SummedEventsHandler enableSummedEvents(Context context, Map<String, Object> customAttributes, String universeName, String token, long timeIntervalMillis, int timeBetweenRetriesMillis) {
        return threadSender.createSummedEventsHandler(context, customAttributes, universeName, token, this, timeIntervalMillis, timeBetweenRetriesMillis);
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     */
    public void send(BacktraceData data, OnServerResponseEventListener callback) {
        BacktraceHandlerInputReport input = new BacktraceHandlerInputReport(data, callback,
                this.onServerError, this.requestHandler);
        threadSender.sendReport(input);
    }

    public void sendEventsPayload(EventsPayload payload) {
        BacktraceHandlerInputEvents input;
        if (payload instanceof UniqueEventsPayload) {
            input = new BacktraceHandlerInputEvents(payload, this.uniqueEventsServerResponse,
                    this.onServerError, this.uniqueEventsRequestHandler);
        } else if (payload instanceof SummedEventsPayload) {
            input = new BacktraceHandlerInputEvents(payload, this.summedEventsServerResponse,
                    this.onServerError, this.summedEventsRequestHandler);
        }
        else {
            BacktraceLogger.e(LOG_TAG, "sendEventsPayload not implemented for payload of type " + payload.getClass());
            return;
        }
        threadSender.send(input);
    }
}
