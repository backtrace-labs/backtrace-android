package backtraceio.library.services;

import java.util.List;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace Api class that allows to send a diagnostic data to server
 */
public class BacktraceApi implements IBacktraceApi {

    private final static transient String LOG_TAG = BacktraceApi.class.getSimpleName();

    private transient BacktraceHandlerThread threadSender;

    /**
     * URL to server
     */
    private String serverUrl;

    /**
     * Data format
     */
    private final String format = "json";

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
            BacktraceLogger.e(LOG_TAG, "BacktraceCredentials parameter passed to BacktraceApi constructor is null");
            throw new IllegalArgumentException("BacktraceCredentials cannot be null");
        }
        this.serverUrl = String.format("%spost?format=%s&token=%s", credentials.getEndpointUrl(),
                this.format, credentials.getSubmissionToken());

        threadSender = new BacktraceHandlerThread(BacktraceHandlerThread.class.getSimpleName(), this.serverUrl);
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
        if (this.requestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
            this.requestHandler.onRequest(data);
        }
        BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();
        send(json, attachments, data.report, callback);
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param json
     * @param attachments
     * @param report
     */
    private void send(String json, List<String> attachments,
                      BacktraceReport report, OnServerResponseEventListener callback) {
        threadSender.sendReport(json, attachments, report, callback, this.onServerError);
    }
}
