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

    private static final transient String LOG_TAG = BacktraceBreadcrumbs.class.getSimpleName();

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

    public static final int DEFAULT_MAX_LOG_SIZE_BYTES = 64000;

    String breadcrumbLogDirectory;

    final private static String breadcrumbLogFileName = "bt-breadcrumbs-0";

    public BacktraceBreadcrumbs(String breadcrumbLogDirectory) {
        this.breadcrumbLogDirectory = breadcrumbLogDirectory;
    }

    private void unregisterAutomaticBreadcrumbReceivers() {
        // Unregister old receivers
        if (backtraceBroadcastReceiver != null) {
            BacktraceLogger.d(LOG_TAG, "Unregistering previous BacktraceBroadcastReceiver");
            this.context.unregisterReceiver(backtraceBroadcastReceiver);
            backtraceBroadcastReceiver = null;
        }

        if (backtraceComponentListener != null) {
            BacktraceLogger.d(LOG_TAG, "Unregistering previous BacktraceComponentListener");
            this.context.unregisterComponentCallbacks(backtraceComponentListener);
            backtraceComponentListener = null;
        }

        if (backtraceActivityLifecycleListener != null) {
            if (context instanceof Application) {
                BacktraceLogger.d(LOG_TAG, "Unregistering previous BacktraceActivityLifecycleListener");
                ((Application) context).unregisterActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
                backtraceActivityLifecycleListener = null;
            } else {
                BacktraceLogger.e(LOG_TAG, "BacktraceActivityLifecycleListener registered with non-Activity context");
            }
        }
    }

    private void registerAutomaticBreadcrumbReceivers() {
        unregisterAutomaticBreadcrumbReceivers();

        if (enabledBreadcrumbTypes == null) {
            BacktraceLogger.d(LOG_TAG, "No breadcrumbs are enabled, not registering any new breadcrumb receivers");
            return;
        }

        backtraceBroadcastReceiver = new BacktraceBroadcastReceiver(this);
        context.registerReceiver(backtraceBroadcastReceiver,
                backtraceBroadcastReceiver.getIntentFilter());

        if (enabledBreadcrumbTypes.contains(BacktraceBreadcrumbType.SYSTEM)) {
            backtraceComponentListener = new BacktraceComponentListener(this);
            context.registerComponentCallbacks(backtraceComponentListener);

            if (context instanceof Application) {
                backtraceActivityLifecycleListener = new BacktraceActivityLifecycleListener(this);
                ((Application) context).registerActivityLifecycleCallbacks(backtraceActivityLifecycleListener);
            }
        }
    }

    @Override
    public boolean enableBreadcrumbs(Context context) {
        return enableBreadcrumbs(context, BacktraceBreadcrumbType.ALL);
    }

    @Override
    public boolean enableBreadcrumbs(Context context, EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable) {
        return enableBreadcrumbs(context, breadcrumbTypesToEnable, DEFAULT_MAX_LOG_SIZE_BYTES);
    }

    @Override
    public boolean enableBreadcrumbs(Context context, int maxBreadcrumbLogSizeBytes) {
        return enableBreadcrumbs(context, BacktraceBreadcrumbType.ALL, maxBreadcrumbLogSizeBytes);
    }

    @Override
    public boolean enableBreadcrumbs(Context context, EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable, int maxBreadcrumbLogSizeBytes) {
        this.context = context;
        if (backtraceBreadcrumbsLogManager == null) {
            try {
                backtraceBreadcrumbsLogManager = new BacktraceBreadcrumbsLogManager(
                        breadcrumbLogDirectory + "/" + breadcrumbLogFileName,
                        maxBreadcrumbLogSizeBytes);
            } catch (Exception ex) {
                BacktraceLogger.e(LOG_TAG, "Could not start the Breadcrumb logger due to: " + ex.getMessage());
                return false;
            }
        }

        this.enabledBreadcrumbTypes = breadcrumbTypesToEnable;
        registerAutomaticBreadcrumbReceivers();

        // We should log all breadcrumb configuration changes in the breadcrumbs
        addConfigurationBreadcrumb();
        return true;
    }

    @Override
    public EnumSet<BacktraceBreadcrumbType> getEnabledBreadcrumbTypes() {
        return this.enabledBreadcrumbTypes;
    }

    @Override
    public boolean clearBreadcrumbs() {
        boolean success = backtraceBreadcrumbsLogManager.clear();
        // Make sure the configuration is always known
        addConfigurationBreadcrumb();
        return success;
    }

    /**
     * NOTE: This should only be used for testing
     *
     * @param breadcrumbId Will force set the current breadcrumb ID
     */
    @Override
    public void setCurrentBreadcrumbId(long breadcrumbId) {
        this.backtraceBreadcrumbsLogManager.setCurrentBreadcrumbId(breadcrumbId);
    }

    /**
     * Get the current breadcrumb ID (exclusive). This is useful when breadcrumbs are queued and
     * posted to an API because in the meantime before the breadcrumbs are finally posted we might
     * get more breadcrumbs which are not relevant (because they occur after queueing up the
     * breadcrumb sender). Therefore it is useful to mark the breadcrumb sender with the most
     * current breadcrumb ID at the time of queuing up the request to post the breadcrumbs.
     *
     * @return current breadcrumb ID (exclusive)
     */
    public long getCurrentBreadcrumbId() {
        return backtraceBreadcrumbsLogManager.getCurrentBreadcrumbId();
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     *
     * @param message
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message) {
        return addBreadcrumb(message, null, BacktraceBreadcrumbType.MANUAL, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     *
     * @param message
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, null, BacktraceBreadcrumbType.MANUAL, level);
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     *
     * @param message
     * @param attributes
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, Map<String, Object> attributes) {
        return addBreadcrumb(message, attributes, BacktraceBreadcrumbType.MANUAL, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     *
     * @param message
     * @param attributes
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, attributes, BacktraceBreadcrumbType.MANUAL, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     *
     * @param message
     * @param type
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type) {
        return addBreadcrumb(message, null, type, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     *
     * @param message
     * @param type
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        return addBreadcrumb(message, null, type, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     *
     * @param message
     * @param attributes
     * @param type
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type) {
        return addBreadcrumb(message, attributes, type, BacktraceBreadcrumbLevel.INFO);
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     *
     * @param message
     * @param attributes
     * @param type
     * @param level
     * @return true if the breadcrumb was successfully added
     */
    @Override
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (this.isBreadcrumbsEnabled() && backtraceBreadcrumbsLogManager != null) {
            return backtraceBreadcrumbsLogManager.addBreadcrumb(message, attributes, type, level);
        }
        return false;
    }

    /**
     * If Breadcrumbs is currently enabled, process the BacktraceReport for sending the Breadcrumb logs
     *
     * @param backtraceReport
     */
    @Override
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
     *
     * @return true if the breadcrumb was successfully added
     */
    private boolean addConfigurationBreadcrumb() {
        if (backtraceBreadcrumbsLogManager == null) {
            BacktraceLogger.e(LOG_TAG, "Could not add configuration breadcrumb, BreadcrumbsLogManager is null");
            return false;
        }

        Map<String, Object> attributes = new HashMap<String, Object>();

        for (BacktraceBreadcrumbType enabledType : BacktraceBreadcrumbType.values()) {
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

    private boolean isBreadcrumbsEnabled() {
        return enabledBreadcrumbTypes != null && !enabledBreadcrumbTypes.isEmpty();
    }

    public String getBreadcrumbLogPath() {
        return this.breadcrumbLogDirectory + "/" + breadcrumbLogFileName;
    }
}
