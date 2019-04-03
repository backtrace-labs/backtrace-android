package backtraceio.library.base;

import android.content.Context;
import android.os.AsyncTask;


import java.util.HashMap;
import java.util.Map;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.IBacktraceClient;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceApi;

/**
 * Base Backtrace Android client
 */
public class BacktraceBase implements IBacktraceClient {

    /**
     * Instance of BacktraceApi that allows to send data to Backtrace API
     */
    private BacktraceApi backtraceApi;

    private void setBacktraceApi(BacktraceApi backtraceApi) {
        this.backtraceApi = backtraceApi;
        if (this.database != null) {
            this.database.setApi(this.backtraceApi);
        }
    }

    /**
     * Application context
     */
    protected Context context;

    /**
     * Backtrace database instance
     */
    public IBacktraceDatabase database;

    /**
     * Get custom client attributes. Every argument stored in dictionary will be send to Backtrace API
     */
    public final Map<String, Object> attributes;

    /**
     * Event which will be executed before sending data to Backtrace API
     */
    private OnBeforeSendEventListener beforeSendEventListener = null;

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials) {
        this(context, credentials, (IBacktraceDatabase) null);
        this.context = context;
        this.backtraceApi = new BacktraceApi(credentials);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param attributes  additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Map<String, Object> attributes) {
        this(context, credentials, (IBacktraceDatabase) null, attributes);
        this.context = context;
        this.backtraceApi = new BacktraceApi(credentials);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings));
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @param attributes       additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings, Map<String, Object> attributes) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, IBacktraceDatabase database) {
        this(context, credentials, database, null);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     * @param attributes  additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, IBacktraceDatabase database, Map<String, Object> attributes) {
        this.context = context;
        this.attributes = attributes != null? attributes: new HashMap<String, Object>();
        this.database = database != null ? database : new BacktraceDatabase();
        this.setBacktraceApi(new BacktraceApi(credentials));
        this.database.start();
    }


    /**
     * Set event executed before sending data to Backtrace API
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnBeforeSendEventListener(OnBeforeSendEventListener eventListener) {
        this.beforeSendEventListener = eventListener;
    }

    /**
     * Set an event executed when Backtrace API return information about send report
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnServerResponseEventListner(OnServerResponseEventListener eventListener) {
        this.backtraceApi.setOnServerResponse(eventListener);
    }

    /**
     * Set an event executed after sending data to Backtrace API
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnAfterSendEventListener(OnAfterSendEventListener eventListener) {
        this.backtraceApi.setAfterSend(eventListener);
    }

    /**
     * Set an event executed when received bad request, unauthorize request or other information from server
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnServerErrorEventListener(OnServerErrorEventListener eventListener) {
        this.backtraceApi.setOnServerError(eventListener);
    }

    /**
     * Custom request handler for call to server
     *
     * @param requestHandler object with method which will be executed
     */
    public void setOnRequestHandler(RequestHandler requestHandler) {
        this.backtraceApi.setRequestHandler(requestHandler);
    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param report current BacktraceReport
     * @return server response
     */
    public BacktraceResult send(BacktraceReport report) {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);

        BacktraceDatabaseRecord record = this.database.add(report, this.attributes);

        if (this.beforeSendEventListener != null) {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }

        BacktraceResult result = this.backtraceApi.send(backtraceData);

        if(record != null)
        {
            record.close();
        }
        if(result != null && result.status == BacktraceResultStatus.Ok)
        {
            this.database.delete(record);
        }

        return result;
    }

    /**
     * Sending asynchronously an exception to Backtrace API
     *
     * @param report current BacktraceReport
     * @return server response
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report) {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);
        if (this.beforeSendEventListener != null) {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }
        return this.backtraceApi.sendAsync(backtraceData);
    }
}
