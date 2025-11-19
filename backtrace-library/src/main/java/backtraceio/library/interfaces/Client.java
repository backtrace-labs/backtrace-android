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
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     */
    void enableNativeIntegration();

    /**
     * Adds new attributes to the client.
     * If the native integration is available and attributes are primitive type,
     * they will be added to the native reports.
     * @param attributes client Attributes
     */
    void addAttribute(Map<String, Object> attributes);

    /**
     * Adds new attribute to the client.
     * If the native integration is available and attributes are primitive type,
     * they will be added to the native reports.
     * @param key attribute key
     * @param value attribute value
     */
    void addAttribute(String key, Object value);
}
