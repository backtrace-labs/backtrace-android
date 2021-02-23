package backtraceio.library.breadcrumbs;

import android.app.Application;
import android.content.Context;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceBreadcrumbs implements Breadcrumbs {

    private static transient String LOG_TAG = BacktraceBreadcrumbs.class.getSimpleName();

    /**
     * Which breadcrumb types are enabled?
     */
    private EnumSet<BacktraceBreadcrumbType> enabledBreadcrumbTypes;

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
     * The Backtrace Breadcrumbs log manager
     */
    private BacktraceBreadcrumbsLogManager backtraceBreadcrumbsLogManager;

    private Context context;

    private static final int DEFAULT_MAX_LOG_SIZE_BYTES = 64000;

    private int maxBreadcrumbLogSizeBytes;

    String breadcrumbLogDirectory;

    final private static String breadcrumbLogFileName = "bt-breadcrumbs-0";

    public BacktraceBreadcrumbs(String breadcrumbLogDirectory, int maxBreadcrumbLogSizeBytes) {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
        this.maxBreadcrumbLogSizeBytes = maxBreadcrumbLogSizeBytes;
    }

    public BacktraceBreadcrumbs(String breadcrumbLogDirectory) {
        this(breadcrumbLogDirectory, DEFAULT_MAX_LOG_SIZE_BYTES);
    }

    private void registerAutomaticBreadcrumbReceivers() {
        if (enabledBreadcrumbTypes == null) {
            return;
        }

        if (backtraceBroadcastReceiver == null) {
            backtraceBroadcastReceiver = new BacktraceBroadcastReceiver(this);
            context.registerReceiver(backtraceBroadcastReceiver,
                    backtraceBroadcastReceiver.getIntentFilter(enabledBreadcrumbTypes));
        }

        if (enabledBreadcrumbTypes.contains(BacktraceBreadcrumbType.SYSTEM)) {
            if (backtraceComponentListener == null) {
                backtraceComponentListener = new BacktraceComponentListener(this);
                context.registerComponentCallbacks(backtraceComponentListener);
            }

            if (backtraceActivityLifecycleListener == null) {
                backtraceActivityLifecycleListener = new BacktraceActivityLifecycleListener(this);
                if (context instanceof Application) {
                    ((Application) context).registerActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
                }
            }
        }
    }

    public boolean enableBreadcrumbs(Context context, EnumSet<BacktraceBreadcrumbType> enabledBreadcrumbTypes) {
        this.context = context;
        if (backtraceBreadcrumbsLogManager == null) {
            try {
                backtraceBreadcrumbsLogManager = new BacktraceBreadcrumbsLogManager(
                                    breadcrumbLogDirectory + "/" + breadcrumbLogFileName,
                                    this.maxBreadcrumbLogSizeBytes);
            } catch (Exception ex) {
                BacktraceLogger.e(LOG_TAG, "Could not start the Breadcrumb logger due to: " + ex.getMessage());
                return false;
            }
        }

        this.enabledBreadcrumbTypes = enabledBreadcrumbTypes;
        registerAutomaticBreadcrumbReceivers();

        // We should log all breadcrumb configuration changes in the breadcrumbs
        addConfigurationBreadcrumb();
        return true;
    }

    public boolean enableBreadcrumbs(Context context) {
        return enableBreadcrumbs(context, BacktraceBreadcrumbType.ALL);
    }

    private void unregisterAutomaticBreadcrumbReceivers() {
        if (backtraceBroadcastReceiver != null) {
            this.context.unregisterReceiver(backtraceBroadcastReceiver);
            backtraceBroadcastReceiver = null;
        }

        if (backtraceComponentListener != null) {
            this.context.unregisterComponentCallbacks(backtraceComponentListener);
            backtraceComponentListener = null;
        }

        if (backtraceActivityLifecycleListener != null) {
            if (context instanceof Application) {
                ((Application) context).unregisterActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
            }
            backtraceActivityLifecycleListener = null;
        }
    }

    public void disableBreadcrumbs() {
        if (this.isBreadcrumbsEnabled()) {
            unregisterAutomaticBreadcrumbReceivers();
        }

        // We should log all breadcrumb configuration changes in the breadcrumbs
        enabledBreadcrumbTypes = BacktraceBreadcrumbType.NONE;
        addConfigurationBreadcrumb();
    }

    public boolean clearBreadcrumbs() {
        boolean success = backtraceBreadcrumbsLogManager.clear();
        // Make sure the configuration is always known
        addConfigurationBreadcrumb();
        return success;
    }

    /**
     * Get the current breadcrumb ID (exclusive). This is useful when breadcrumbs are queued and
     * posted to an API because in the meantime before the breadcrumbs are finally posted we might
     * get more breadcrumbs which are not relevant (because they occur after queueing up the
     * breadcrumb sender). Therefore it is useful to mark the breadcrumb sender with the most
     * current breadcrumb ID at the time of queuing up the request to post the breadcrumbs.
     * @return current breadcrumb ID (exclusive)
     */
    public long getCurrentBreadcrumbId() {
        return backtraceBreadcrumbsLogManager.getCurrentBreadcrumbId();
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
        if (this.isBreadcrumbsEnabled() && backtraceBreadcrumbsLogManager != null) {
            return backtraceBreadcrumbsLogManager.addBreadcrumb(message, attributes, type, level);
        }
        return false;
    }

    /**
     * If Breadcrumbs is currently enabled, process the BacktraceReport for sending the Breadcrumb logs
     * @param backtraceReport
     */
    public void processReportBreadcrumbs(BacktraceReport backtraceReport) {
        if (this.isBreadcrumbsEnabled() == false) {
            return;
        }

        backtraceReport.attachmentPaths.add(this.getBreadcrumbLogPath());

        long lastBreadcrumbId = this.getCurrentBreadcrumbId();
        backtraceReport.attributes.put("breadcrumbs.lastId", lastBreadcrumbId);
    }

    /**
     * Create a breadcrumb which reflects the current breadcrumb configuration
     * @return true if the breadcrumb was successfully added
     */
    private boolean addConfigurationBreadcrumb()
    {
        if (backtraceBreadcrumbsLogManager == null) {
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

        return backtraceBreadcrumbsLogManager.addBreadcrumb("Breadcrumbs configuration",
                attributes,
                BacktraceBreadcrumbType.CONFIGURATION,
                BacktraceBreadcrumbLevel.INFO);
    }

    private boolean isBreadcrumbsEnabled()
    {
        return enabledBreadcrumbTypes != null && !enabledBreadcrumbTypes.isEmpty();
    }

    public String getBreadcrumbLogPath() {
        return this.breadcrumbLogDirectory + "/" + this.breadcrumbLogFileName;
    }
}
