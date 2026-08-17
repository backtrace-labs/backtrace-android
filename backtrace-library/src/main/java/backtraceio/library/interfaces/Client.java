package backtraceio.library.interfaces;

import backtraceio.library.models.json.BacktraceReport;
import java.util.Map;

/**
 * Client interface.
 */
public interface Client {
    /**
     * Send new report to a Backtrace API
     *
     * @param report data which should be send to Backtrace API
     */
    void send(BacktraceReport report);

    /**
     * Synchronously installs the native crash handler (Backtrace database integration is required to enable this feature).
     *
     * <p>Configure initial native attributes, attachments, and breadcrumbs before calling this method.
     * Native crashes that occur before this method completes cannot be captured.
     *
     * <p>Call this method from exactly one application-controlled thread.
     * It may be invoked on a background thread, but doing so delays native crash coverage until initialization completes.
     * Do not call it concurrently with {@code disableNativeIntegration()}.
     */
    void enableNativeIntegration();

    /**
     * Adds new attributes to the client.
     * If the native integration is available and attributes are primitive type,
     * they will be added to the native reports.
     * Note: native crash reports omit attributes with an empty-string value
     * (managed reports are unaffected).
     * @param attributes client Attributes
     */
    void addAttribute(Map<String, Object> attributes);

    /**
     * Adds new attribute to the client.
     * If the native integration is available and attributes are primitive type,
     * they will be added to the native reports.
     * Note: native crash reports omit attributes with an empty-string value
     * (managed reports are unaffected).
     * @param key attribute key
     * @param value attribute value
     */
    void addAttribute(String key, Object value);
}
