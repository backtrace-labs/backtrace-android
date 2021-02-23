package backtraceio.library.interfaces;

import android.content.Context;

import java.util.EnumSet;
import java.util.Map;

import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.models.json.BacktraceReport;

public interface Breadcrumbs {
    /**
     * Enable logging of breadcrumbs and submission with crash reports
     * @param context                   context of current state of the application
     * @return true if we successfully enabled breadcrumbs
     */
    boolean enableBreadcrumbs(Context context);

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     * @param context                   context of current state of the application
     * @param breadcrumbTypesToEnable   a set containing which breadcrumb types to enable
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     *          breadcrumbs will always be enabled
     * @return true if we successfully enabled breadcrumbs
     */
    boolean enableBreadcrumbs(Context context,
                              EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable);

    /**
     * Disable logging of breadcrumbs and submission with crash reports
     */
    void disableBreadcrumbs();

    /**
     * Clear breadcrumb logs
     */
    boolean clearBreadcrumbs();

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     * @param message       a message which describes this breadcrumb (1KB max)
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message);

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param attributes    key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes);

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param attributes    key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param type          broadly describes the category of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbType type);

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param type          broadly describes the category of this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level);

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param attributes    key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type          broadly describes the category of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type);

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     * @param message       a message which describes this breadcrumb (1KB max)
     * @param attributes    key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type          broadly describes the category of this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level);

    /**
     * Process a Backtrace Report to add breadcrumbs, if breadcrumbs is enabled
     * @param report
     * @return
     */
    void processReportBreadcrumbs(BacktraceReport report);
}
