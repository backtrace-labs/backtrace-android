package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.models.json.BacktraceAttributes;

public abstract class EventsPayload {

    private static transient String LOG_TAG = EventsPayload.class.getSimpleName();

    @SerializedName("application")
    private final String application;

    @SerializedName("appversion")
    private final String appVersion;

    public transient int numRetries = 0;

    @SerializedName("metadata")
    private EventsMetadata eventsMetadata;

    public EventsPayload(BacktraceAttributes backtraceAttributes, int droppedEvents) {
        this.application = backtraceAttributes.getApplicationName();
        this.appVersion = backtraceAttributes.getApplicationVersionOrEmpty();
        this.eventsMetadata = new EventsMetadata(droppedEvents);
    }

    public int getDroppedEvents() {
        return this.eventsMetadata.getDroppedEvents();
    }

    public void setDroppedEvents(int droppedEvents) {
        this.eventsMetadata.setDroppedEvents(droppedEvents);
    }

    public abstract ConcurrentLinkedDeque<Event> getEvents();
}
