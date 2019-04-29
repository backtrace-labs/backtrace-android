package backtraceio.library.services;

import android.os.AsyncTask;

import java.util.List;
import java.util.UUID;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.wrappers.AsyncTaskRequestHandlerWrapper;

/**
 * Backtrace Api class that allows to send a diagnostic data to server
 */
public class BacktraceApi implements IBacktraceApi {

    private final static transient String LOG_TAG = BacktraceApi.class.getSimpleName();

    private transient  BacktraceHandlerThread threadSender;

    /**
     * URL to server
     */
    private String serverUrl;

    /**
     * Data format
     */
    private final String format = "json";

    /**
     * Event triggered when server respond to diagnostic data
     */
    private OnServerResponseEventListener onServerResponse = null;

    /**
     * Event triggered when server respond with error
     */
    private OnServerErrorEventListener onServerError = null;

    /**
     * Event triggered after sending diagnostic data to server
     */
    private OnAfterSendEventListener afterSend = null;

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

        threadSender = new BacktraceHandlerThread(BacktraceHandlerThread.class.getSimpleName(), this.serverUrl, this.onServerResponse);
    }

    public void setOnServerResponse(OnServerResponseEventListener onServerResponse) {
        this.onServerResponse = onServerResponse;
    }

    public void setOnServerError(OnServerErrorEventListener onServerError) {
        this.onServerError = onServerError;
    }

    public void setAfterSend(OnAfterSendEventListener afterSend) {
        this.afterSend = afterSend;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    private BacktraceResult send(UUID requestId, String json, List<String> attachments,
                                 BacktraceReport report) {
        BacktraceResult result;
        try {
            AsyncTask<Void, Void, BacktraceResult> task = sendAsync(requestId, json,
                    attachments, report);
            result = task.get();
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "Error during sending report", e);
            return BacktraceResult.OnError(report, e);
        }
        return result;
    }

    private AsyncTask<Void, Void, BacktraceResult> sendAsync(UUID requestId, String json,
                                                             List<String> attachments,
                                                             BacktraceReport report) {
        return new BacktraceHttpAsyncTask(serverUrl, requestId, json, attachments, report,
                this.onServerResponse, this.onServerError, this.afterSend).execute();
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     * @return server response
     */
    public BacktraceResult send(BacktraceData data) {
        if (this.requestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
            return this.requestHandler.onRequest(data);
        }
        BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();
        return send(UUID.randomUUID(), json, attachments, data.report);
    }

    public void sendUncaughted(BacktraceData data){
        BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();
        BacktraceReportSender.sendReport(serverUrl, json, attachments, data.report);
    }


    public void sendWithThreadHandler(BacktraceData data, OnServerResponseEventListener serverResponseEventListener) {
        if (this.requestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending using custom request handler");
            this.requestHandler.onRequest(data);
        }
        BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();

        threadSender.sendReport(UUID.randomUUID(), json, attachments, data.report, serverResponseEventListener);
    }


    /**
     * Sending asynchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     * @return AsyncTask which returns server response after execution
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceData data) {
        if (this.requestHandler != null) {
            BacktraceLogger.d(LOG_TAG, "Sending report using custom request handler");
            return new AsyncTaskRequestHandlerWrapper(this.requestHandler, data).execute();
        }
        BacktraceLogger.d(LOG_TAG, "Sending report using default request handler");
        String json = BacktraceSerializeHelper.toJson(data);
        List<String> attachments = data.getAttachments();
        return sendAsync(UUID.randomUUID(), json, attachments, data.report);
    }
}
