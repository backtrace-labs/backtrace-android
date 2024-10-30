package backtraceio.library.models.metrics;

import backtraceio.library.common.serializers.SerializedName;

public class EventsMetadata {

    @SerializedName("dropped_events")
    private int droppedEvents = 0;

    public EventsMetadata(int droppedEvents) {
        this.droppedEvents = droppedEvents;
    }

    public int getDroppedEvents() {
        return this.droppedEvents;
    }

    public void setDroppedEvents(int droppedEvents) {
        this.droppedEvents = droppedEvents;
    }
}
