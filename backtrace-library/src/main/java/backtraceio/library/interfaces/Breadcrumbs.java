package backtraceio.library.interfaces;

import android.content.Context;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.events.OnSuccessfulBreadcrumbAddEventListener;
import backtraceio.library.models.json.BacktraceReport;
import java.util.EnumSet;
import java.util.Map;

public interface Breadcrumbs {
    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context context of current state of the application
     * @return true if we successfully enabled breadcrumbs
     */
    boolean enableBreadcrumbs(Context context);

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                 context of current state of the application
     * @param breadcrumbTypesToEnable a set containing which breadcrumb types to enable
     * @return true if we successfully enabled breadcrumbs
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     * breadcrumbs will always be enabled
     */
    boolean enableBreadcrumbs(Context context, EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable);

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                   context of current state of the application
     * @param maxBreadcrumbLogSizeBytes breadcrumb log size limit in bytes, should be a power of 2
     * @return true if we successfully enabled breadcrumbs
     */
    boolean enableBreadcrumbs(Context context, int maxBreadcrumbLogSizeBytes);

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                   context of current state of the application
     * @param breadcrumbTypesToEnable   a set containing which breadcrumb types to enable
     * @param maxBreadcrumbLogSizeBytes breadcrumb log size limit in bytes, should be a power of 2
     * @return true if we successfully enabled breadcrumbs
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     * breadcrumbs will always be enabled
     */
    boolean enableBreadcrumbs(
            Context context, EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable, int maxBreadcrumbLogSizeBytes);

    /**
     * Gets the enabled breadcrumb types
     *
     * @return enabled breadcrumb types
     */
    EnumSet<BacktraceBreadcrumbType> getEnabledBreadcrumbTypes();

    /**
     * Clear breadcrumb logs
     *
     * @return true if log was successfully cleared
     */
    boolean clearBreadcrumbs();

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message);

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param level   the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes);

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param level      the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param type    broadly describes the category of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbType type);

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param type    broadly describes the category of this breadcrumb
     * @param level   the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type       broadly describes the category of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type);

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type       broadly describes the category of this breadcrumb
     * @param level      the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(
            String message,
            Map<String, Object> attributes,
            BacktraceBreadcrumbType type,
            BacktraceBreadcrumbLevel level);

    /**
     * Process a Backtrace Report to add breadcrumbs, if breadcrumbs is enabled
     *
     * @param report
     */
    void processReportBreadcrumbs(BacktraceReport report);

    /**
     * Get the location of the breadcrumb log
     *
     * @return Location of the breadcrumb log
     */
    String getBreadcrumbLogPath();

    /**
     * NOTE: This should only be used for testing
     *
     * @param breadcrumbId Will force set the current breadcrumb ID
     */
    void setCurrentBreadcrumbId(long breadcrumbId);

    /**
     * Get the current breadcrumb ID (exclusive). This is useful when breadcrumbs are queued and
     * posted to an API because in the meantime before the breadcrumbs are finally posted we might
     * get more breadcrumbs which are not relevant (because they occur after queueing up the
     * breadcrumb sender). Therefore it is useful to mark the breadcrumb sender with the most
     * current breadcrumb ID at the time of queuing up the request to post the breadcrumbs.
     *
     * @return current breadcrumb ID (exclusive)
     */
    long getCurrentBreadcrumbId();

    /**
     * Determinate if Breadcrumbs are enabled.
     * @return true if breadcrumbs are enabled.
     */
    boolean isEnabled();

    /**
     * Set event executed after adding a breadcrumb to the breadcrumb storage.
     *
     * @param eventListener object with method which will be executed.
     */
    void setOnSuccessfulBreadcrumbAddEventListener(OnSuccessfulBreadcrumbAddEventListener eventListener);
}
