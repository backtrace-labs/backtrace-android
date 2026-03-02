package backtraceio.library.models.metrics;

import backtraceio.library.models.types.BacktraceResultStatus;

/**
 * Metrics events send method result
 */
public class EventsResult {

    /**
     * Message
     */
    public String message;

    /**
     * Result status eg. server error, ok
     */
    public BacktraceResultStatus status = BacktraceResultStatus.Ok;

    /**
     * Current report
     */
    private EventsPayload eventsPayload;

    /**
     * HTTP response code in case of error
     */
    private int statusCode = -1;

    /**
     * Create new instance of BacktraceResult
     *
     * @param payload    metrics events payload
     * @param message    message
     * @param status     result status eg. ok, server error
     * @param statusCode HTTP status code
     */
    public EventsResult(EventsPayload payload, String message, BacktraceResultStatus status, int statusCode) {
        setEventsPayload(payload);
        this.message = message;
        this.status = status;
        this.statusCode = statusCode;
    }

    /**
     * Set result when error occurs while sending data to API
     *
     * @param payload   submitted payload
     * @param exception current exception
     * @return BacktraceResult with exception information
     */
    public static EventsResult OnError(EventsPayload payload, Exception exception, int statusCode) {
        return new EventsResult(payload, exception.getMessage(), BacktraceResultStatus.ServerError, statusCode);
    }

    public EventsPayload getEventsPayload() {
        return eventsPayload;
    }

    public void setEventsPayload(EventsPayload eventsPayload) {
        this.eventsPayload = eventsPayload;
    }

    /**
     * @return HTTP status code
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Set the HTTP status code
     *
     * @param statusCode HTTP status code to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
