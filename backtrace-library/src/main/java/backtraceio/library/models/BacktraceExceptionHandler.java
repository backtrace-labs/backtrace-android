package backtraceio.library.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final transient String LOG_TAG = BacktraceExceptionHandler.class.getSimpleName();
    private static Map<String, Object> customAttributes;
    private final Thread.UncaughtExceptionHandler rootHandler;
    private final CountDownLatch signal = new CountDownLatch(1);
    private final BacktraceClient client;

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

        BacktraceLogger.e(LOG_TAG, "Sending uncaught exception to Backtrace API", throwable);
        for (BacktraceReport report :
                this.transformExceptionIntoReports(throwable)) {
            this.client.send(report, callback);
        }
        BacktraceLogger.d(LOG_TAG, "Uncaught exception sent to Backtrace API");

        try {
            BacktraceLogger.d(LOG_TAG, "Default uncaught exception handler");
            signal.await();
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Exception during waiting for response", ex);
        }
    }

    private Exception getCausedException(Throwable throwable) {
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }

        return new UnhandledThrowableWrapper(throwable);
    }

    private OnServerResponseEventListener getCallbackToDefaultHandler(final Thread thread, final Throwable throwable) {
        return backtraceResult -> {
            BacktraceLogger.d(LOG_TAG, "Root handler event callback");
            rootHandler.uncaughtException(thread, throwable);
            signal.countDown();
        };
    }

    private List<BacktraceReport> transformExceptionIntoReports(Throwable throwable) {
        final String exceptionTrace = UUID.randomUUID().toString();
        BacktraceReport parent = null;

        List<BacktraceReport> reports = new ArrayList<>();
        while (throwable != null) {
            Exception currentException = throwable instanceof Exception ? (Exception) throwable : new UnhandledThrowableWrapper(throwable);
            BacktraceReport report = new BacktraceReport(currentException, BacktraceExceptionHandler.customAttributes);

            report.attributes.put(BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.UnhandledExceptionAttributeType);
            report.attributes.put("error.trace", exceptionTrace);
            report.attributes.put("error.id", report.uuid.toString());
            report.attributes.put("error.parent", parent != null ? parent.uuid.toString() : null);
            reports.add(report);

            throwable = throwable.getCause();
            parent = report;
        }

        return reports;
    }
}