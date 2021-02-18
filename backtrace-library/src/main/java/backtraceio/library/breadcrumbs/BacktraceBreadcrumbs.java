package backtraceio.library.breadcrumbs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.interfaces.Breadcrumbs;

public class BacktraceBreadcrumbs implements Breadcrumbs {

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

    private Context context;

    private static final int DEFAULT_MAX_LOG_SIZE_BYTES = 64000;

    public BacktraceBreadcrumbs(Context context, int maxBreadcrumbLogSizeBytes) throws IOException, NoSuchMethodException {
        // Create the breadcrumbs subdirectory for storing the breadcrumb logs
        this.context = context;
        String fullBreadcrumbLogDirectory = getBreadcrumbLogDirectory(context);
        backtraceBreadcrumbsLogger = new BacktraceBreadcrumbsLogger(fullBreadcrumbLogDirectory, maxBreadcrumbLogSizeBytes);
    }

    public BacktraceBreadcrumbs(Context context) throws IOException, NoSuchMethodException {
        this(context, DEFAULT_MAX_LOG_SIZE_BYTES);
    }

    private void registerAutomaticBreadcrumbReceivers(Set<BacktraceBreadcrumbType> breadcrumbTypesToEnable) {
        if (isBreadcrumbsEnabled()) {
            unregisterAutomaticBreadcrumbReceivers();
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

        this.enabledBreadcrumbTypes = breadcrumbTypesToEnable;
    }

    public void setEnabledBreadcrumbTypes(Set<BacktraceBreadcrumbType> breadcrumbTypesToEnable) {
        if (breadcrumbTypesToEnable == null)
            return;

        registerAutomaticBreadcrumbReceivers(breadcrumbTypesToEnable);
    }

    public void enableBreadcrumbs(Set<BacktraceBreadcrumbType> enabledBreadcrumbTypes)
    {
        setEnabledBreadcrumbTypes(enabledBreadcrumbTypes);

        // We should log all breadcrumb configuration changes in the breadcrumbs
        addConfigurationBreadcrumb();
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
        unregisterAutomaticBreadcrumbReceivers();

        enabledBreadcrumbTypes = null;

        // We should log all breadcrumb configuration changes in the breadcrumbs
        addConfigurationBreadcrumb();
    }

    public boolean clearBreadcrumbs() {
        boolean success = backtraceBreadcrumbsLogger.clear();
        // Make sure the configuration is always known
        addConfigurationBreadcrumb();
        return success;
    }

    public boolean isBreadcrumbsEnabled()
    {
        return enabledBreadcrumbTypes != null && !enabledBreadcrumbTypes.isEmpty();
    }

    public static String getBreadcrumbLogDirectory(@NonNull Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" + BacktraceBreadcrumbs.breadcrumbLogDirectory;
    }

    public static String getBreadcrumbLogFileName() {
        return BacktraceBreadcrumbsLogger.getLogFileName();
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
        return backtraceBreadcrumbsLogger.addBreadcrumb(message, attributes, type, level);
    }

    /**
     * Create a breadcrumb which reflects the current breadcrumb configuration
     * @return true if the breadcrumb was successfully added
     */
    private boolean addConfigurationBreadcrumb()
    {
        Map<String, Object> attributes = new HashMap<String, Object>();

        for (BacktraceBreadcrumbType enabledType : BacktraceBreadcrumbType.values())
        {
            String state = (enabledBreadcrumbTypes != null &&
                    enabledBreadcrumbTypes.contains(enabledType)) ? "enabled" : "disabled";

            attributes.put(enabledType.toString(), state);
        }

        return addBreadcrumb("Breadcrumbs configuration", attributes, BacktraceBreadcrumbType.CONFIGURATION);
    }
}
