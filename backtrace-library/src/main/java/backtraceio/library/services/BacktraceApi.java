package backtraceio.library.services;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.EventsOnServerResponseEventListener;
import backtraceio.library.events.EventsRequestHandler;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.metrics.SummedEventsPayload;
import backtraceio.library.models.metrics.UniqueEventsPayload;

/**
 * Backtrace Api class that allows to send data to Backtrace endpoints
 */
public class BacktraceApi implements Api {

    private final static transient String LOG_TAG = BacktraceApi.class.getSimpleName();

    private final transient BacktraceHandlerThread threadSender;

    /**
     * URL to report submission endpoint
     */
    private final String reportSubmissionUrl;

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

    public void setOnServerError(OnServerErrorEventListener onServerError) {
        this.onServerError = onServerError;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public UniqueEventsHandler enableUniqueEvents(BacktraceMetrics backtraceMetrics) {
        return threadSender.createUniqueEventsHandler(backtraceMetrics, this);
    }

    @Override
    public SummedEventsHandler enableSummedEvents(BacktraceMetrics backtraceMetrics) {
        return threadSender.createSummedEventsHandler(backtraceMetrics, this);
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     */
    @Override
    public void send(BacktraceData data, OnServerResponseEventListener callback) {
        BacktraceHandlerInputReport input = new BacktraceHandlerInputReport(data, callback,
                this.onServerError, this.requestHandler);
        threadSender.sendReport(input);
    }

    @Override
    public void sendEventsPayload(UniqueEventsPayload payload) {
        BacktraceHandlerInputEvents input = new BacktraceHandlerInputEvents(payload, this.uniqueEventsServerResponse,
                this.onServerError, this.uniqueEventsRequestHandler);
        threadSender.sendUniqueEvents(input);
    }

    @Override
    public void sendEventsPayload(SummedEventsPayload payload) {
        BacktraceHandlerInputEvents input = new BacktraceHandlerInputEvents(payload, this.summedEventsServerResponse,
                this.onServerError, this.summedEventsRequestHandler);
        threadSender.sendSummedEvents(input);
    }
}
