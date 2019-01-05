package backtraceio.library.models;

import backtraceio.library.BacktraceClient;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final String TAG = "UncaughtExHandler";
    private final Thread.UncaughtExceptionHandler rootHandler;
    private BacktraceClient client;

    public BacktraceExceptionHandler(BacktraceClient client) {
        this.client = client;
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void enable(BacktraceClient client) {
        new BacktraceExceptionHandler(client);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        if (ex instanceof Exception) {
            this.client.send(new BacktraceReport((Exception) ex));
        }
        rootHandler.uncaughtException(thread, ex);
    }
}