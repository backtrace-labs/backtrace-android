package backtraceio.library.models;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import backtraceio.library.BacktraceClient;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace UncaughtExceptionHandler which will be invoked when a Thread abruptly terminates due
 * to an uncaught exception
 */
public class BacktraceExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static transient String LOG_TAG = BacktraceExceptionHandler.class.getSimpleName();

    private final Thread.UncaughtExceptionHandler rootHandler;
    private final CountDownLatch signal = new CountDownLatch(1);
    private BacktraceClient client;
    private static Map<String, Object> customAttributes;

    private BacktraceExceptionHandler(BacktraceClient client) {
        BacktraceLogger.d(LOG_TAG, "BacktraceExceptionHandler initialization");
        this.client = client;
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void setCustomAttributes(Map<String, Object> attributes) {
        BacktraceExceptionHandler.customAttributes = attributes;
    }

    /**
     * Enable catching unexpected exceptions by BacktraceClient
     *
     * @param client current Backtrace client instance
     *               which will be used to send information about exception
     */
    public static void enable(BacktraceClient client) {
        new BacktraceExceptionHandler(client);
    }

    /**
     * Called when a thread stops because of an uncaught exception
     *
     * @param thread    thread that is about to exit
     * @param throwable uncaught exception
     */
    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        OnServerResponseEventListener callback = getCallbackToDefaultHandler(thread, throwable);

        if (throwable instanceof Exception) {
            BacktraceLogger.e(LOG_TAG, "Sending uncaught exception to Backtrace API", throwable);
            this.client.send(new BacktraceReport((Exception) throwable, BacktraceExceptionHandler.customAttributes), callback);
            BacktraceLogger.d(LOG_TAG, "Uncaught exception sent to Backtrace API");
        }
        BacktraceLogger.d(LOG_TAG, "Default uncaught exception handler");
        try {
            signal.await();
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Exception during waiting for response", ex);
        }
    }

    private OnServerResponseEventListener getCallbackToDefaultHandler(final Thread thread, final Throwable throwable) {
        return new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                BacktraceLogger.d(LOG_TAG, "Root handler event callback");
                rootHandler.uncaughtException(thread, throwable);
                signal.countDown();
            }
        };
    }
}