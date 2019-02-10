package backtraceio.library.base;

import android.content.Context;
import android.os.AsyncTask;


import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.IBacktraceClient;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceApi;

/**
 * Base Backtrace Android client
 */
public class BacktraceBase implements IBacktraceClient {

    private BacktraceApi backtraceApi;
    protected Context context;
    private OnBeforeSendEventListener beforeSendEventListener = null;

    /**
     * Initialize new client instance with BacktraceCredentials
     * @param context context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials)
    {
        this.context = context;
        this.backtraceApi = new BacktraceApi(credentials);
    }

    /**
     * Set event executed before sending data to Backtrace API
     * @param eventListener object with method which will be executed
     */
    public void setOnBeforeSendEventListener(OnBeforeSendEventListener eventListener){
        this.beforeSendEventListener = eventListener;
    }

    /**
     * Set an event executed when Backtrace API return information about send report
     * @param eventListener object with method which will be executed
     */
    public void setOnServerResponseEventListner(OnServerResponseEventListener eventListener){
        this.backtraceApi.setOnServerResponse(eventListener);
    }

    /**
     * Set an event executed after sending data to Backtrace API
     * @param eventListener object with method which will be executed
     */
    public void setOnAfterSendEventListener(OnAfterSendEventListener eventListener)
    {
        this.backtraceApi.setAfterSend(eventListener);
    }

    /**
     * Set an event executed when received bad request, unauthorize request or other information from server
     * @param eventListener object with method which will be executed
     */
    public void setOnServerErrorEventListener(OnServerErrorEventListener eventListener){
        this.backtraceApi.setOnServerError(eventListener);
    }

    /**
     * Custom request handler for call to server
     * @param requestHandler object with method which will be executed
     */
    public void setOnRequestHandler(RequestHandler requestHandler){
        this.backtraceApi.setRequestHandler(requestHandler);
    }

    /**
     * Sending an exception to Backtrace API
     * @param report current BacktraceReport
     * @return server response
     */
    public BacktraceResult send(BacktraceReport report)
    {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);
        if(this.beforeSendEventListener != null)
        {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }
        return this.backtraceApi.send(backtraceData);
    }

    /**
     * Sending asynchronously an exception to Backtrace API
     * @param report current BacktraceReport
     * @return server response
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report)
    {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);
        if(this.beforeSendEventListener != null)
        {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }
        return this.backtraceApi.sendAsync(backtraceData);
    }
}
