package backtraceio.library.services;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;

/**
 * Backtrace Api class that allows to send a diagnostic data to server
 */
public class BacktraceApi implements Api {

    private final static transient String LOG_TAG = BacktraceApi.class.getSimpleName();

    private transient BacktraceHandlerThread threadSender;

    /**
     * URL to server
     */
    private String serverUrl;


    /**
     * Event triggered when server respond with error
     */
    private OnServerErrorEventListener onServerError = null;

    /**
     * User custom request method
     */
    private RequestHandler requestHandler = null;

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
        this.serverUrl = credentials.getSubmissionUrl().toString();

        threadSender = new BacktraceHandlerThread(BacktraceHandlerThread.class.getSimpleName(),
                this.serverUrl);
    }

    public void setOnServerError(OnServerErrorEventListener onServerError) {
        this.onServerError = onServerError;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     */
    public void send(BacktraceData data, OnServerResponseEventListener callback) {
        BacktraceHandlerInput input = new BacktraceHandlerInput(data, callback,
                this.onServerError, this.requestHandler);
        threadSender.sendReport(input);
    }
}
