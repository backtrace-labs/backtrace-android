package backtraceio.library.breadcrumbs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceBreadcrumbs implements Breadcrumbs {

    private static transient String LOG_TAG = BacktraceBreadcrumbs.class.getSimpleName();

    /**
     * Which breadcrumb types are enabled?
     */
    private Set<BacktraceBreadcrumbType> enabledBreadcrumbTypes;

    /**
     * The Backtrace BroadcastReciever instance
     */
    private BacktraceBroadcastReceiver backtraceBroadcastReceiver;

    /**
     * The Backtrace ComponentCallbacks2 listener
     */
    private BacktraceComponentListener backtraceComponentListener;

    /**
     * The Backtrace ActivityLifecycleCallbacks listener
     */
    private BacktraceActivityLifecycleListener backtraceActivityLifecycleListener;

    /**
     * The Backtrace Breadcrumbs logger instance
     */
    private BacktraceBreadcrumbsLogger backtraceBreadcrumbsLogger;

    final private static String breadcrumbLogDirectory = "breadcrumbs";

    final private static String breadcrumbLogPath = "bt-breadcrumb-0";

    private Context context;

    private static final int DEFAULT_MAX_LOG_SIZE_BYTES = 64000;

    private int maxBreadcrumbLogSizeBytes;

    public BacktraceBreadcrumbs(Context context, int maxBreadcrumbLogSizeBytes) {
        this.context = context;
        this.maxBreadcrumbLogSizeBytes = maxBreadcrumbLogSizeBytes;
        File breadcrumbLogDir = new File(getBreadcrumbLogDirectory());
        breadcrumbLogDir.mkdir();
    }

    public BacktraceBreadcrumbs(Context context) throws IOException, NoSuchMethodException {
        this(context, DEFAULT_MAX_LOG_SIZE_BYTES);
    }

    private void registerAutomaticBreadcrumbReceivers(Set<BacktraceBreadcrumbType> breadcrumbTypesToEnable) {
        if (this.isBreadcrumbsEnabled() == true) {
            unregisterAutomaticBreadcrumbReceivers();
        }
        if (breadcrumbTypesToEnable == null) {
            return;
        }

        backtraceBroadcastReceiver = new BacktraceBroadcastReceiver(this);
        context.registerReceiver(backtraceBroadcastReceiver,
                backtraceBroadcastReceiver.getIntentFilter(breadcrumbTypesToEnable));

        if (breadcrumbTypesToEnable.contains(BacktraceBreadcrumbType.SYSTEM)) {
            backtraceComponentListener = new BacktraceComponentListener(this);
            context.registerComponentCallbacks(backtraceComponentListener);

            backtraceActivityLifecycleListener = new BacktraceActivityLifecycleListener(this);
            if (context instanceof Application) {
                ((Application) context).registerActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
            }
        }
    }

    public boolean enableBreadcrumbs(Set<BacktraceBreadcrumbType> enabledBreadcrumbTypes,
                                  boolean isNewInstance,
                                  BacktraceBase staleBreadcrumbLogSender) {
        if (isNewInstance == true) {
            handleExistingBreadcrumbLogFile(context, staleBreadcrumbLogSender);
        }
        if (backtraceBreadcrumbsLogger == null) {
            try {
                backtraceBreadcrumbsLogger = new BacktraceBreadcrumbsLogger(getBreadcrumbLogPath(this.context), this.maxBreadcrumbLogSizeBytes);
            } catch (Exception ex) {
                BacktraceLogger.e(LOG_TAG, "Could not start the Breadcrumb logger due to: " + ex.getMessage());
                return false;
            }
        }

        registerAutomaticBreadcrumbReceivers(enabledBreadcrumbTypes);

        this.enabledBreadcrumbTypes = enabledBreadcrumbTypes;

        // We should log all breadcrumb configuration changes in the breadcrumbs
        addConfigurationBreadcrumb();
        return true;
    }

    public boolean enableBreadcrumbs(boolean isNewInstance, BacktraceBase staleBreadcrumbLogSender) {
        final Set<BacktraceBreadcrumbType> enabledBreadcrumbTypes = new HashSet<BacktraceBreadcrumbType>(){{
            add(BacktraceBreadcrumbType.CONFIGURATION);
            add(BacktraceBreadcrumbType.HTTP);
            add(BacktraceBreadcrumbType.LOG);
            add(BacktraceBreadcrumbType.MANUAL);
            add(BacktraceBreadcrumbType.NAVIGATION);
            add(BacktraceBreadcrumbType.SYSTEM);
            add(BacktraceBreadcrumbType.USER);
        }};

        return enableBreadcrumbs(enabledBreadcrumbTypes, isNewInstance, staleBreadcrumbLogSender);
    }

    private void unregisterAutomaticBreadcrumbReceivers() {
        this.context.unregisterReceiver(backtraceBroadcastReceiver);
        backtraceBroadcastReceiver = null;

        this.context.unregisterComponentCallbacks(backtraceComponentListener);
        backtraceComponentListener = null;

        if (context instanceof Application) {
            ((Application) context).unregisterActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
        }
    }

    public void disableBreadcrumbs() {
        if (this.isBreadcrumbsEnabled()) {
            unregisterAutomaticBreadcrumbReceivers();
        }

        // We should log all breadcrumb configuration changes in the breadcrumbs
        enabledBreadcrumbTypes = null;
        addConfigurationBreadcrumb();
    }

    public boolean clearBreadcrumbs() {
        boolean success = backtraceBreadcrumbsLogger.clear();
        // Make sure the configuration is always known
        addConfigurationBreadcrumb();
        return success;
    }

    public String getBreadcrumbLogDirectory() {
        return context.getFilesDir().getAbsolutePath() + "/" + BacktraceBreadcrumbs.breadcrumbLogDirectory;
    }

    public static String getBreadcrumbLogPath(@NonNull Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" + BacktraceBreadcrumbs.breadcrumbLogDirectory + "/" + breadcrumbLogPath;
    }

    public long getCurrentBreadcrumbId() {
        return backtraceBreadcrumbsLogger.getCurrentBreadcrumbId();
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     * @param message
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message) {
        return addBreadcrumb(message, null, BacktraceBreadcrumbType.MANUAL, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     * @param message
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, null, BacktraceBreadcrumbType.MANUAL, level);
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     * @param message
     * @param attributes
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes) {
        return addBreadcrumb(message, attributes, BacktraceBreadcrumbType.MANUAL, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     * @param message
     * @param attributes
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, attributes, BacktraceBreadcrumbType.MANUAL, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     * @param message
     * @param type
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type) {
        return addBreadcrumb(message, null, type, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     * @param message
     * @param type
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, null, type, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     * @param message
     * @param attributes
     * @param type
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type) {
        return addBreadcrumb(message, attributes, type, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     * @param message
     * @param attributes
     * @param type
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (this.isBreadcrumbsEnabled() && backtraceBreadcrumbsLogger != null) {
            return backtraceBreadcrumbsLogger.addBreadcrumb(message, attributes, type, level);
        }
        return false;
    }

    /**
     * If Breadcrumbs is currently enabled, process the BacktraceReport for sending the Breadcrumb logs
     * @param backtraceReport
     */
    public void processBacktraceReport(BacktraceReport backtraceReport) {
        if (this.isBreadcrumbsEnabled()) {
            backtraceReport.attachmentPaths.add(BacktraceBreadcrumbs.getBreadcrumbLogPath(context));

            long lastBreadcrumbId = this.getCurrentBreadcrumbId();
            backtraceReport.attributes.put("breadcrumbs.lastId", lastBreadcrumbId);
        }
    }

    /**
     * Create a breadcrumb which reflects the current breadcrumb configuration
     * @return true if the breadcrumb was successfully added
     */
    private boolean addConfigurationBreadcrumb()
    {
        if (backtraceBreadcrumbsLogger == null) {
            return false;
        }

        Map<String, Object> attributes = new HashMap<String, Object>();

        for (BacktraceBreadcrumbType enabledType : BacktraceBreadcrumbType.values())
        {
            if (enabledType == BacktraceBreadcrumbType.CONFIGURATION) {
                attributes.put(enabledType.toString(), "enabled");
            }

            String state = (enabledBreadcrumbTypes != null &&
                    enabledBreadcrumbTypes.contains(enabledType)) ? "enabled" : "disabled";

            attributes.put(enabledType.toString(), state);
        }

        return backtraceBreadcrumbsLogger.addBreadcrumb("Breadcrumbs configuration",
                attributes,
                BacktraceBreadcrumbType.CONFIGURATION,
                BacktraceBreadcrumbLevel.INFO);
    }

    private void handleExistingBreadcrumbLogFile(Context context, BacktraceBase breadcrumbLogSender) {
        if (context == null || breadcrumbLogSender == null) {
            return;
        }

        final File breadcrumbLogFile = new File(BacktraceBreadcrumbs.getBreadcrumbLogPath(context));
        final File breadcrumbTempLogFile = new File(this.getBreadcrumbLogDirectory() + "/bt-stalecrumbs");
        try {
            if (breadcrumbLogFile.exists()) {
                FileUtils.moveFile(breadcrumbLogFile, breadcrumbTempLogFile);
                BacktraceReport staleBreadcrumbsReport = new BacktraceReport("Found breadcrumbs from a previous session");
                staleBreadcrumbsReport.attachmentPaths.add(breadcrumbTempLogFile.getAbsolutePath());

                OnServerResponseEventListener staleBreadcrumbSubmissionCompleteCallback = new OnServerResponseEventListener() {
                    @Override
                    public void onEvent(BacktraceResult backtraceResult) {
                        breadcrumbTempLogFile.delete();
                    }
                };

                breadcrumbLogSender.send(staleBreadcrumbsReport, staleBreadcrumbSubmissionCompleteCallback);
            }
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Could not send previously existing breadcrumbs due to: " + ex.getMessage());
            breadcrumbLogFile.delete();
        }
    }

    private boolean isBreadcrumbsEnabled()
    {
        return enabledBreadcrumbTypes != null && !enabledBreadcrumbTypes.isEmpty();
    }
}
