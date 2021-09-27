package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class EventsPayload<T extends Event> {

    private static final transient String LOG_TAG = EventsPayload.class.getSimpleName();

    @SerializedName("application")
    private final String application;

    @SerializedName("appversion")
    private final String appVersion;

    public transient int numRetries = 0;

    @SerializedName("metadata")
    private final EventsMetadata eventsMetadata;

    public EventsPayload(String application, String appVersion, int droppedEvents) {
        this.application = application;
        this.appVersion = appVersion;
        this.eventsMetadata = new EventsMetadata(droppedEvents);
    }

    public int getDroppedEvents() {
        return this.eventsMetadata.getDroppedEvents();
    }

    public void setDroppedEvents(int droppedEvents) {
        this.eventsMetadata.setDroppedEvents(droppedEvents);
    }

    public abstract ConcurrentLinkedDeque<T> getEvents();
}
