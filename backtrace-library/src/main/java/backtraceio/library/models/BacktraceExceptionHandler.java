package backtraceio.library.models;

import android.content.Context;
import android.util.Log;

import backtraceio.library.BacktraceClient;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final String TAG = "UncaughtExHandler";
    private final Context context;
    private final Thread.UncaughtExceptionHandler rootHandler;
    private BacktraceClient client;

    public BacktraceExceptionHandler(Context context, BacktraceClient client) {
        this.context = context;
        this.client = client;
        // we should store the current exception handler -- to invoke it for all not handled exceptions ...
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        try {
            if (ex instanceof Exception) {
                this.client.send(new BacktraceReport((Exception)ex));
            }
            // TODO: invoke root handler
        } catch (Exception e) {
            Log.e(TAG, "Exception Logger failed!", e);
        }
    }
}